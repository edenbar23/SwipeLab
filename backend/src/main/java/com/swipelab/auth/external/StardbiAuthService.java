package com.swipelab.auth.external;

import com.swipelab.auth.infrastructure.AuthProvider;
import com.swipelab.dto.request.ExternalLoginRequest;
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

        // 2. Eagerly cache user details so subsequent filter checks are free
        stardbiAuthProvider.cacheUserDetails(accessToken, username);

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
     * Called by {@link ExternalAuthFilter} on every authenticated request.
     * Returns Spring {@link UserDetails} if the token is valid; {@code null} otherwise.
     */
    public UserDetails processStardbiToken(String accessToken) {
        if (stardbiAuthProvider.validateToken(accessToken)) {
            UserDetails details = stardbiAuthProvider.getUserDetails(accessToken);
            if (details != null) {
                return details;
            }
        }
        return null;
    }
}
