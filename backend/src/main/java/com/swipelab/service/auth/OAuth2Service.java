package com.swipelab.service.auth;

import com.swipelab.model.entity.User;
import com.swipelab.model.enums.AuthProvider;
import com.swipelab.model.enums.UserRole;
import com.swipelab.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.time.LocalDateTime;
import java.util.Optional;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.beans.factory.annotation.Value;
import java.util.Collections;
import java.io.IOException;
import java.security.GeneralSecurityException;

@Service
@RequiredArgsConstructor
public class OAuth2Service {

    private final UserRepository userRepository;

    @Value("${GOOGLE_CLIENT_ID}")
    private String googleClientId;

    public GoogleIdToken.Payload verifyGoogleToken(String idTokenString) {
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(),
                    new GsonFactory())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken != null) {
                return idToken.getPayload();
            } else {
                throw new IllegalArgumentException("Invalid ID token.");
            }
        } catch (GeneralSecurityException | IOException e) {
            throw new IllegalArgumentException("Invalid ID token: " + e.getMessage());
        }
    }

    @Transactional
    public User processUserLogin(OAuth2User oAuth2User) {
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String picture = oAuth2User.getAttribute("picture");
        String providerId = oAuth2User.getAttribute("sub");

        return processUser(email, name, picture, providerId);
    }

    // Method to be used by POST endpoint if we implement ID Token verification
    @Transactional
    public User processUserFromIdToken(String email, String name, String picture, String providerId) {
        return processUser(email, name, picture, providerId);
    }

    private User processUser(String email, String name, String picture, String providerId) {
        Optional<User> userOptional = userRepository.findByEmail(email);

        if (userOptional.isPresent()) {
            User user = userOptional.get();
            // Update existing user info
            user.setLastLogin(LocalDateTime.now());
            if (user.getDisplayName() == null)
                user.setDisplayName(name);
            if (user.getProfileImageUrl() == null)
                user.setProfileImageUrl(picture);

            // Should we update provider ID if it wasn't there? (e.g. linking local account)
            if (user.getProvider() == AuthProvider.LOCAL && user.getProviderId() == null) {
                // Optional: Link account
                // user.setProvider(AuthProvider.GOOGLE); // or keep as LOCAL
                user.setProviderId(providerId);
            }

            return userRepository.save(user);
        } else {
            return registerNewUser(email, name, picture, providerId);
        }
    }

    private User registerNewUser(String email, String name, String picture, String providerId) {
        User user = new User();
        user.setUsername(email);
        user.setEmail(email);
        user.setDisplayName(name);
        user.setProfileImageUrl(picture);
        user.setProvider(AuthProvider.GOOGLE);
        user.setProviderId(providerId);
        user.setRole(UserRole.USER);
        user.setEmailVerified(true);
        user.setActive(true);
        user.setAccountLocked(false);
        user.setCreatedAt(LocalDateTime.now());

        return userRepository.save(user);
    }
}
