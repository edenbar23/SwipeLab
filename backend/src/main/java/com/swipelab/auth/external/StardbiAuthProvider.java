package com.swipelab.auth.external;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.swipelab.integration.stardbi.StardbiClient;
import com.swipelab.integration.stardbi.dto.StardbiRefreshTokenRequestDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class StardbiAuthProvider implements ExternalAuthProvider {

    private final StardbiClient stardbiClient;
    private final ObjectMapper objectMapper;

    // Cache to map token to UserDetails if we extract directly from login
    private final Map<String, UserDetails> tokenCache = new ConcurrentHashMap<>();

    @Override
    public boolean supports(String providerName) {
        return "stardbi".equalsIgnoreCase(providerName);
    }

    @Override
    public boolean validateToken(String accessToken) {
        return stardbiClient.checkAuth(accessToken);
    }

    @Override
    public String refreshToken(String refreshToken) {
        try {
            var response = stardbiClient.refreshToken(new StardbiRefreshTokenRequestDto(refreshToken));
            return response.getAccess();
        } catch (Exception e) {
            log.warn("Failed to refresh Stardbi token: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public UserDetails getUserDetails(String accessToken) {
        if (tokenCache.containsKey(accessToken)) {
            return tokenCache.get(accessToken);
        }

        // Fallback: extract from JWT payload if not in cache (e.g., server restart)
        try {
            String[] parts = accessToken.split("\\.");
            if (parts.length == 3) {
                String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]));
                JsonNode payload = objectMapper.readTree(payloadJson);

                String username = null;
                if (payload.has("username")) {
                    username = payload.get("username").asText();
                } else if (payload.has("user_id")) {
                    username = payload.get("user_id").asText();
                }

                if (username != null) {
                    UserDetails user = User.builder()
                            .username(username)
                            .password("")
                            .authorities("ROLE_ADMIN")
                            .build();
                    tokenCache.put(accessToken, user);
                    return user;
                }
            }
        } catch (Exception e) {
            log.debug("Could not decode Stardbi token to extract username", e);
        }

        return null;
    }

    public void cacheUserDetails(String accessToken, String username) {
        UserDetails user = User.builder()
                .username(username)
                .password("")
                .authorities("ROLE_ADMIN")
                .build();
        tokenCache.put(accessToken, user);
    }
}
