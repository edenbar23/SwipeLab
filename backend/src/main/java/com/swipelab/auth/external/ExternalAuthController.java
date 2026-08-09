package com.swipelab.auth.external;

import com.swipelab.auth.domain.AuthMapper;
import com.swipelab.dto.request.ExternalLoginRequest;
import com.swipelab.dto.response.UserProfileResponse;
import com.swipelab.users.domain.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.Cookie;

@RestController
@RequestMapping("/api/v1/auth/external")
@RequiredArgsConstructor
public class ExternalAuthController {

    private final StardbiAuthService stardbiAuthService;
    private final AuthMapper authMapper;
    private final com.swipelab.auth.application.JwtService jwtService;

    private void setAuthCookies(HttpServletResponse response, String accessToken, String refreshToken) {
        Cookie accessCookie = new Cookie("accessToken", accessToken);
        accessCookie.setHttpOnly(true);
        accessCookie.setPath("/");
        accessCookie.setMaxAge(3600); // 1 hour

        Cookie refreshCookie = new Cookie("refreshToken", refreshToken);
        refreshCookie.setHttpOnly(true);
        refreshCookie.setPath("/");
        refreshCookie.setMaxAge(604800); // 7 days

        response.addCookie(accessCookie);
        response.addCookie(refreshCookie);
    }

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
            @Valid @RequestBody ExternalLoginRequest request, HttpServletResponse httpResponse) {

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
                    
            setAuthCookies(httpResponse, accessToken, refreshToken);
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(401).build();
    }
}
