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

    private HttpHeaders createAuthHeaders(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
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
        String url = baseUrl + "/swipelab/experiments/";
        HttpEntity<Void> entity = new HttpEntity<>(createAuthHeaders(accessToken));
        
        ResponseEntity<List<ExternalExperimentDto>> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, new ParameterizedTypeReference<List<ExternalExperimentDto>>() {});
        return response.getBody();
    }

    // ======================================
    // IMAGES (BOUNDING BOXES)
    // ======================================

    public List<Long> getUnclassifiedImageIds(Long experimentId, String accessToken) {
        String url = baseUrl + "/swipelab/bounding_boxes/?experiment=" + experimentId;
        HttpEntity<Void> entity = new HttpEntity<>(createAuthHeaders(accessToken));
        
        ResponseEntity<List<Long>> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, new ParameterizedTypeReference<List<Long>>() {});
        return response.getBody();
    }

    public byte[] getImageBuffer(Long imageId, String accessToken) {
        String url = baseUrl + "/swipelab/bounding_boxes/" + imageId + "/";
        HttpEntity<Void> entity = new HttpEntity<>(createAuthHeaders(accessToken));
        
        ResponseEntity<byte[]> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, byte[].class);
        return response.getBody();
    }

    // ======================================
    // TAXONOMY
    // ======================================

    public List<ExternalTaxonomyDto> getTaxonomy(String accessToken) {
        String url = baseUrl + "/swipelab/taxonomy/";
        HttpEntity<Void> entity = new HttpEntity<>(createAuthHeaders(accessToken));
        
        ResponseEntity<List<ExternalTaxonomyDto>> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, new ParameterizedTypeReference<List<ExternalTaxonomyDto>>() {});
        return response.getBody();
    }

    // ======================================
    // CLASSIFICATION
    // ======================================

    public void submitClassifications(List<ExternalClassificationDto> classifications, String accessToken) {
        String url = baseUrl + "/swipelab/classification/";
        HttpEntity<List<ExternalClassificationDto>> entity = new HttpEntity<>(classifications, createAuthHeaders(accessToken));
        
        restTemplate.exchange(url, HttpMethod.POST, entity, Void.class);
    }
}
