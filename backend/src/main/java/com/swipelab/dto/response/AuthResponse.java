package com.swipelab.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class AuthResponse {

    private final String accessToken;
    private final String refreshToken;
    private final long expiresIn;
    private final String message;
    private final UserProfileResponse user;
}
