package com.swipelab.security;

import com.swipelab.model.entity.User;
import com.swipelab.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        // Extract user information
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String picture = oAuth2User.getAttribute("picture");
        String providerId = oAuth2User.getAttribute("sub"); // Google ID

        // Create or update user
        User user = userRepository.findByEmail(email)
                .orElseGet(() -> {
                    // Generate a unique username from email if needed, or just use email part
                    String username = email.split("@")[0];
                    if (userRepository.findByUsername(username).isPresent()) {
                        username = email; // Fallback to full email if simple username taken
                    }

                    User newUser = User.builder()
                            .username(username)
                            .email(email)
                            .displayName(name)
                            .profileImageUrl(picture)
                            .provider(com.swipelab.model.enums.AuthProvider.GOOGLE)
                            .providerId(providerId)
                            .role(com.swipelab.model.enums.UserRole.USER)
                            .build();
                    return userRepository.save(newUser);
                });

        // Update profile picture if changed
        if (picture != null && !picture.equals(user.getProfileImageUrl())) {
            user.setProfileImageUrl(picture);
            userRepository.save(user);
        }

        return oAuth2User;
    }
}