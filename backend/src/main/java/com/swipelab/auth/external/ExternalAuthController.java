package com.swipelab.auth.external;

import com.swipelab.auth.domain.AuthMapper;
import com.swipelab.dto.request.ExternalLoginRequest;
import com.swipelab.dto.response.UserProfileResponse;
import com.swipelab.users.domain.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth/external")
@RequiredArgsConstructor
public class ExternalAuthController {

    private final StardbiAuthService stardbiAuthService;
    private final AuthMapper authMapper;
    private final com.swipelab.auth.application.JwtService jwtService;

    /**
     * Called by the frontend immediately after a successful Stardbi login.
     * Validates the Stardbi access token, auto-provisions a local SwipeLab
     * user if this is their first time, caches the Stardbi token, and returns
     * a native SwipeLab JWT.
     *
     * Expected request body: the full Stardbi login response object.
     */
    @PostMapping("/stardbi/loginExternal")
    public ResponseEntity<com.swipelab.dto.response.AuthResponse> loginExternal(
            @Valid @RequestBody ExternalLoginRequest request) {

        User user = stardbiAuthService.loginExternal(request);
        if (user != null) {
            String accessToken = jwtService.generateAccessToken(user);
            String refreshToken = jwtService.generateRefreshToken(user);

            UserProfileResponse profile = authMapper.toUserProfileResponse(user);
            
            com.swipelab.dto.response.AuthResponse response = com.swipelab.dto.response.AuthResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .expiresIn(jwtService.getAccessTokenExpirySeconds())
                    .message("External login successful")
                    .user(profile)
                    .build();
                    
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(401).build();
    }
}
