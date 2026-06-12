package com.swipelab.auth.external;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.swipelab.integration.stardbi.StardbiClientPort;
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

    private final StardbiClientPort stardbiClient;
    private final ObjectMapper objectMapper;
    private final com.swipelab.users.infrastructure.UserRepository userRepository;

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
                String payloadBase64Url = parts[1];
                while (payloadBase64Url.length() % 4 != 0) {
                    payloadBase64Url += "=";
                }
                String payloadJson = new String(Base64.getUrlDecoder().decode(payloadBase64Url));
                JsonNode payload = objectMapper.readTree(payloadJson);

                String username = null;
                if (payload.has("username")) {
                    username = payload.get("username").asText();
                } else if (payload.has("user_id")) {
                    String stardbiId = payload.get("user_id").asText();
                    // Resolve the real username from the DB using the provider_id
                    com.swipelab.users.domain.User localUser = userRepository
                            .findByProviderIdAndProvider(stardbiId, com.swipelab.auth.infrastructure.AuthProvider.STARDBI)
                            .orElse(null);
                    if (localUser != null) {
                        username = localUser.getUsername();
                    }
                }

                if (username != null) {
                    UserDetails user = User.builder()
                            .username(username)
                            .password("")
                            .authorities("ROLE_RESEARCHER")
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
                .authorities("ROLE_RESEARCHER")
                .build();
        tokenCache.put(accessToken, user);
    }
}
