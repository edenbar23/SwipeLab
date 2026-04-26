package com.swipelab.integration.stardbi;

import com.swipelab.integration.stardbi.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StardbiClient {

    private final RestTemplate restTemplate;

    @Value("${stardbi.api.url:https://stardbi.cs.bgu.ac.il}")
    private String baseUrl;

    @Value("${stardbi.service-account.username:swipelab_server}")
    private String serviceAccountUsername;

    @Value("${stardbi.service-account.password:default_password}")
    private String serviceAccountPassword;

    private String serviceAccountToken;
    private java.time.Instant tokenExpiration;

    private synchronized String getServiceAccountToken() {
        if (serviceAccountUsername == null || serviceAccountUsername.isEmpty() || 
            serviceAccountPassword == null || serviceAccountPassword.isEmpty()) {
            log.error("Stardbi service account credentials are not configured properly! Check properties.");
            throw new IllegalStateException("Missing Stardbi service account credentials. Cannot authenticate.");
        }

        if (serviceAccountToken == null || java.time.Instant.now().isAfter(tokenExpiration)) {
            StardbiAuthResponseDto dto = login(new StardbiAuthRequestDto(serviceAccountUsername, serviceAccountPassword));
            this.serviceAccountToken = dto.getAccess();
            long lifetimeSeconds = dto.getLifetime() != null ? dto.getLifetime().longValue() : 3600;
            this.tokenExpiration = java.time.Instant.now().plusSeconds(lifetimeSeconds - 60);
        }
        return serviceAccountToken;
    }

    private HttpHeaders createAuthHeaders(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        if (accessToken != null) {
            headers.setBearerAuth(accessToken);
        }
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        return headers;
    }

    // ======================================
    // AUTHENTICATION (Per-User with role Researcher (ADMIN))
    // ======================================

    public StardbiAuthResponseDto login(StardbiAuthRequestDto request) {
        String url = baseUrl + "/auth/get_token/";
        ResponseEntity<StardbiAuthResponseDto> response = restTemplate.postForEntity(url, request, StardbiAuthResponseDto.class);
        return response.getBody();
    }

    public boolean checkAuth(String accessToken) {
        String url = baseUrl + "/auth/check_auth/";
        HttpEntity<Void> entity = new HttpEntity<>(createAuthHeaders(accessToken));
        try {
            ResponseEntity<Void> response = restTemplate.exchange(url, HttpMethod.GET, entity, Void.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.warn("Check auth failed or returned 401: {}", e.getMessage());
            return false;
        }
    }

    public StardbiAuthResponseDto refreshToken(StardbiRefreshTokenRequestDto request) {
        String url = baseUrl + "/auth/token_refresh/";
        ResponseEntity<StardbiAuthResponseDto> response = restTemplate.postForEntity(url, request, StardbiAuthResponseDto.class);
        return response.getBody();
    }

    public void logout(StardbiLogoutRequestDto request, String accessToken) {
        String url = baseUrl + "/auth/logout/";
        HttpEntity<StardbiLogoutRequestDto> entity = new HttpEntity<>(request, createAuthHeaders(accessToken));
        restTemplate.exchange(url, HttpMethod.POST, entity, Void.class);
    }

    // ======================================
    // EXPERIMENTS
    // ======================================

    public List<ExternalExperimentDto> getExperiments(String accessToken) {
        String url = baseUrl + "/swipe_lab/experiments/";
        HttpEntity<Void> entity = new HttpEntity<>(createAuthHeaders(accessToken));
        
        ResponseEntity<List<ExternalExperimentDto>> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, new ParameterizedTypeReference<List<ExternalExperimentDto>>() {});
        return response.getBody();
    }

    public List<ExternalExperimentDto> getExperiments() {
        return getExperiments(getServiceAccountToken());
    }

    // ======================================
    // IMAGES (BOUNDING BOXES)
    // ======================================

    public List<ExternalCropDto> getUnclassifiedImageIds(Long experimentId) {
        String url = baseUrl + "/swipe_lab/crops/?experiment=" + experimentId;
        // Use the Stardbi service account token instead of the SwipeLab JWT
        HttpEntity<Void> entity = new HttpEntity<>(createAuthHeaders(getServiceAccountToken()));
        
        ResponseEntity<List<ExternalCropDto>> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, new ParameterizedTypeReference<List<ExternalCropDto>>() {});
        return response.getBody();
    }

    public byte[] getImageBuffer(Long boxId) {
        String url = baseUrl + "/swipe_lab/crops/" + boxId + "/image/";
        HttpHeaders headers = createAuthHeaders(getServiceAccountToken());
        headers.setAccept(List.of(MediaType.ALL));
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        
        ResponseEntity<byte[]> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, byte[].class);
        return response.getBody();
    }

    public byte[] downloadExperimentCropsZip(Long experimentId, String accessToken) {
        String url = baseUrl + "/swipe_lab/crops/download/?experiment=" + experimentId;
        String tokenToUse = (accessToken != null && !accessToken.isEmpty()) ? accessToken : getServiceAccountToken();
        HttpHeaders headers = createAuthHeaders(tokenToUse);
        headers.setAccept(List.of(MediaType.ALL));
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        
        ResponseEntity<byte[]> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, byte[].class);
        return response.getBody();
    }

    // ======================================
    // TAXONOMY
    // ======================================

    public List<ExternalTaxonomyDto> getTaxonomy() {
        String url = baseUrl + "/swipe_lab/taxonomy/";
        HttpEntity<Void> entity = new HttpEntity<>(createAuthHeaders(getServiceAccountToken()));
        
        ResponseEntity<List<ExternalTaxonomyDto>> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, new ParameterizedTypeReference<List<ExternalTaxonomyDto>>() {});
        return response.getBody();
    }

    // ======================================
    // CLASSIFICATION (LABELS)
    // ======================================

    public void postLabel(ExternalLabelDto label) {
        String url = baseUrl + "/swipe_lab/labels/";
        
        try {
            HttpEntity<ExternalLabelDto> entity = new HttpEntity<>(label, createAuthHeaders(getServiceAccountToken()));
            restTemplate.exchange(url, HttpMethod.POST, entity, Void.class);
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED || e.getStatusCode() == HttpStatus.FORBIDDEN) {
                log.warn("Stardbi API returned {} for postLabel, forcing token refresh and retrying...", e.getStatusCode());
                // Force a token refresh by resetting the token so getServiceAccountToken() will fetch a new one immediately
                synchronized(this) {
                    this.serviceAccountToken = null;
                }
                
                // Retry the request once with the new token
                HttpEntity<ExternalLabelDto> retryEntity = new HttpEntity<>(label, createAuthHeaders(getServiceAccountToken()));
                restTemplate.exchange(url, HttpMethod.POST, retryEntity, Void.class);
            } else {
                throw e;
            }
        }
    }
}
