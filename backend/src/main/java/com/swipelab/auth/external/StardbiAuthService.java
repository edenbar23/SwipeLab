package com.swipelab.auth.external;

import com.swipelab.auth.infrastructure.AuthProvider;
import com.swipelab.auth.dto.ExternalLoginRequest;
import com.swipelab.integration.stardbi.StardbiClientPort;
import com.swipelab.model.enums.UserRole;
import com.swipelab.model.enums.UserStatus;
import com.swipelab.users.domain.User;
import com.swipelab.users.infrastructure.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class StardbiAuthService {

    private final StardbiAuthProvider stardbiAuthProvider;
    private final StardbiClientPort stardbiClient;
    private final UserRepository userRepository;
    private final org.springframework.cache.CacheManager cacheManager;

    /**
     * Validates the Stardbi access token, then provisions a local SwipeLab user
     * if one doesn't already exist. Stardbi users always get the ADMIN role.
     *
     * @return the local {@link User} entity (new or existing) on success, or {@code null} on failure
     */
    @Transactional
    public User loginExternal(ExternalLoginRequest request) {
        String accessToken = request.getAccess();
        String username    = request.getUsername();

        // 1. Validate the token against Stardbi
        if (!stardbiAuthProvider.validateToken(accessToken)) {
            log.warn("Stardbi token validation failed for user '{}'", username);
            return null;
        }

        // 2. Cache the Stardbi access and refresh tokens for BFF proxying
        org.springframework.cache.Cache cache = cacheManager.getCache(com.swipelab.config.CacheConfig.CACHE_STARDBI_TOKENS);
        if (cache != null) {
            cache.put(username, new StardbiTokensDto(accessToken, request.getRefresh()));
        }

        // 3. Provision local user if this is first login
        Optional<User> existing = userRepository.findByUsername(username);
        if (existing.isPresent()) {
            User user = existing.get();
            log.info("Stardbi user '{}' found in SwipeLab DB (id={})", username, user.getUsername());
            return user;
        }

        // Build a display name from first + last (fall back to username)
        String firstName   = request.getFirstName() != null ? request.getFirstName() : "";
        String lastName    = request.getLastName()  != null ? request.getLastName()  : "";
        String displayName = (firstName + " " + lastName).trim();
        if (displayName.isEmpty()) {
            displayName = username;
        }

        // Email from Stardbi may be blank – generate a placeholder so the NOT NULL constraint is satisfied
        String email = (request.getEmail() != null && !request.getEmail().isBlank())
                ? request.getEmail()
                : username + "@stardbi.external";

        User newUser = User.builder()
                .username(username)
                .email(email)
                .displayName(displayName)
                .provider(AuthProvider.STARDBI)
                .providerId(String.valueOf(request.getId()))
                .role(UserRole.RESEARCHER)
                .status(UserStatus.ACTIVE)
                .emailVerified(true)          // external provider is implicitly verified
                .active(true)
                .accountLocked(false)
                .isFlagged(false)
                .build();

        User saved = userRepository.save(newUser);
        log.info("Auto-provisioned new SwipeLab user for Stardbi researcher '{}' (stardbi id={})",
                username, request.getId());
        return saved;
    }

    /**
     * Executes a given action using the cached Stardbi token for the user.
     * Automatically handles token refresh on 401 Unauthorized errors from Stardbi.
     *
     * @param username The local SwipeLab username
     * @param action The operation to perform using the Stardbi access token
     * @return The result of the action
     */
    public <T> T executeWithStardbiToken(String username, java.util.function.Function<String, T> action) {
        org.springframework.cache.Cache cache = cacheManager.getCache(com.swipelab.config.CacheConfig.CACHE_STARDBI_TOKENS);
        if (cache == null) {
            throw new com.swipelab.exception.StardbiSessionExpiredException("Stardbi tokens cache is not configured.");
        }

        StardbiTokensDto tokens = cache.get(username, StardbiTokensDto.class);
        if (tokens == null || tokens.accessToken() == null) {
            throw new com.swipelab.exception.StardbiSessionExpiredException("Stardbi session expired (token not in cache).");
        }

        try {
            return action.apply(tokens.accessToken());
        } catch (org.springframework.web.client.HttpClientErrorException.Unauthorized e) {
            log.info("Stardbi access token expired for user '{}'. Attempting refresh...", username);

            if (tokens.refreshToken() == null) {
                cache.evict(username);
                throw new com.swipelab.exception.StardbiSessionExpiredException("Stardbi session expired and no refresh token is available.");
            }

            try {
                String newAccessToken = stardbiAuthProvider.refreshToken(tokens.refreshToken());
                if (newAccessToken == null || newAccessToken.isBlank()) {
                    throw new RuntimeException("Refresh token response was null/empty");
                }
                
                cache.put(username, new StardbiTokensDto(newAccessToken, tokens.refreshToken()));
                log.info("Stardbi token refreshed successfully for user '{}'. Retrying operation.", username);
                
                return action.apply(newAccessToken);
                
            } catch (Exception refreshEx) {
                log.warn("Failed to refresh Stardbi token for user '{}'. Logging out.", username, refreshEx);
                cache.evict(username);
                throw new com.swipelab.exception.StardbiSessionExpiredException("Stardbi session expired and refresh failed.");
            }
        }
    }
}



