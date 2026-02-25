package com.swipelab.auth.domain;

import com.swipelab.auth.infrastructure.AuthProvider;
import com.swipelab.dto.request.RegisterRequest;
import com.swipelab.dto.response.AuthResponse;
import com.swipelab.dto.response.UserProfileResponse;
import com.swipelab.users.domain.User;
import com.swipelab.model.enums.UserRole;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;

@Component
public class AuthMapper {

    public User toUser(RegisterRequest request) {
        if (request == null) {
            return null;
        }

        return User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .displayName(request.getDisplayName())
                .role(UserRole.USER)
                .provider(AuthProvider.LOCAL)
                .active(true)
                .accountLocked(false)
                .emailVerified(false)
                .credibilityScore(0.0)
                .score(0L)
                .badges("")
                .rank("UNRANKED")
                .build();
    }

    public UserProfileResponse toUserProfileResponse(User user) {
        if (user == null) {
            return null;
        }

        return UserProfileResponse.builder()
                .username(user.getUsername())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .profileImageUrl(user.getProfileImageUrl())
                .role(user.getRole())
                .score(user.getScore() != null ? user.getScore() : 0L)
                .badges(user.getBadges() != null && !user.getBadges().isEmpty()
                        ? Arrays.asList(user.getBadges().split(","))
                        : new ArrayList<>())
                .rank(user.getRank() != null ? user.getRank() : "UNRANKED")
                .build();
    }

    public AuthResponse toAuthResponse(String accessToken, String refreshToken, User user) {
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(86400) // 24 hours in seconds, aligns with JWT service usually
                .message("Authentication successful")
                .user(toUserProfileResponse(user))
                .build();
    }
}
