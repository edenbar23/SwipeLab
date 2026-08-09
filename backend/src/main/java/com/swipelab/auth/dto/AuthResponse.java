package com.swipelab.auth.dto;

import com.swipelab.users.dto.UserProfileResponse;
import lombok.AllArgsConstructor;
import com.swipelab.users.dto.UserProfileResponse;
import lombok.Builder;
import com.swipelab.users.dto.UserProfileResponse;
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




