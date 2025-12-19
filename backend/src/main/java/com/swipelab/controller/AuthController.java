package com.swipelab.controller;

import com.swipelab.dto.request.*;
import com.swipelab.dto.response.AuthResponse;
import com.swipelab.dto.response.UserProfileResponse;
import com.swipelab.exception.EmailVerificationException;
import com.swipelab.exception.PasswordResetException;
import com.swipelab.exception.UnauthorizedException;
import com.swipelab.service.auth.AuthenticationService;
import com.swipelab.service.user.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import com.swipelab.model.entity.User;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationService authenticationService;

    private final UserService userService;
    private final com.swipelab.service.auth.OAuth2Service oAuth2Service;
    private final com.swipelab.service.auth.JwtService jwtService;

    /**
     * Register a new user
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        try {
            AuthResponse response = authenticationService.register(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(AuthResponse.builder()
                            .message(e.getMessage())
                            .build());
        }
    }

    /**
     * Verify user email with token
     * Endpoint: POST /api/v1/auth/email/verify
     */
    @PostMapping("/email/verify")
    public ResponseEntity<Map<String, String>> verifyEmail(
            @Valid @RequestBody EmailVerificationRequest request) {
        try {
            authenticationService.verifyEmail(request.getToken());

            Map<String, String> response = new HashMap<>();
            response.put("message", "Email verified successfully");
            response.put("status", "success");

            return ResponseEntity.ok(response);
        } catch (EmailVerificationException e) {
            Map<String, String> response = new HashMap<>();
            response.put("message", e.getMessage());
            response.put("status", "error");

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    /**
     * Resend verification email
     * Endpoint: POST /api/v1/auth/email/resend
     */
    @PostMapping("/email/resend")
    public ResponseEntity<Map<String, String>> resendVerificationEmail(
            @RequestParam String email) {
        try {
            authenticationService.resendVerificationEmail(email);

            Map<String, String> response = new HashMap<>();
            response.put("message", "Verification email sent successfully");
            response.put("status", "success");

            return ResponseEntity.ok(response);
        } catch (EmailVerificationException e) {
            Map<String, String> response = new HashMap<>();
            response.put("message", e.getMessage());
            response.put("status", "error");

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    /**
     * Get current authenticated user
     */
    @GetMapping("/user")
    public ResponseEntity<?> getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.ok(Map.of("authenticated", false));
        }

        Map<String, Object> response = new HashMap<>();
        response.put("authenticated", true);
        response.put("email", userDetails.getUsername());
        response.put("authorities", userDetails.getAuthorities());

        return ResponseEntity.ok(response);
    }

    /**
     * Test endpoint to verify security configuration
     */
    @GetMapping("/test")
    public ResponseEntity<?> testEndpoint() {
        return ResponseEntity.ok(Map.of(
                "message", "Security configuration is working!",
                "timestamp", System.currentTimeMillis()));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authenticationService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(
            @RequestHeader("Authorization") String authorizationHeader) {

        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new UnauthorizedException("Missing refresh token");
        }

        String refreshToken = authorizationHeader.substring(7);
        return ResponseEntity.ok(authenticationService.refresh(refreshToken));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestHeader("Authorization") String authorizationHeader) {

        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new UnauthorizedException("Missing refresh token");
        }

        String refreshToken = authorizationHeader.substring(7);
        authenticationService.logout(refreshToken);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> me(Principal principal) {

        if (principal == null) {
            throw new UnauthorizedException("Unauthorized");
        }

        return ResponseEntity.ok(
                userService.getUserProfile(principal.getName()));
    }

    @PostMapping("/login/google")
    public ResponseEntity<AuthResponse> loginGoogle(@RequestBody Map<String, String> payload) {
        String token = payload.get("credential"); // Standard field for Google Identity Services
        if (token == null) {
            token = payload.get("idToken"); // Fallback
        }

        if (token == null) {
            throw new RuntimeException("Missing Google ID Token");
        }

        // Verify the token securely
        com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload googlePayload = oAuth2Service
                .verifyGoogleToken(token);

        String email = googlePayload.getEmail();
        String name = (String) googlePayload.get("name");
        String picture = (String) googlePayload.get("picture");
        String providerId = googlePayload.getSubject();

        if (email == null) {
            throw new RuntimeException("Invalid ID Token: Email not found");
        }

        User user = oAuth2Service.processUserFromIdToken(email, name, picture, providerId);

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        return ResponseEntity.ok(AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtService.getAccessTokenExpirySeconds())
                .message("Google Login successful")
                .build());
    }

    /**
     * Forgot password endpoint
     * Generates reset token and sends email for existing users
     * Returns success for all requests (prevents email enumeration)
     *
     * Endpoint: POST /api/v1/auth/password/forgot
     */
    @PostMapping("/password/forgot")
    public ResponseEntity<Map<String, String>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {

        // Process forgot password request
        authenticationService.forgotPassword(request.getEmail());

        // Always return success message
        Map<String, String> response = new HashMap<>();
        response.put("message", "If your email exists in our system, you will receive a password reset link shortly.");
        response.put("status", "success");

        return ResponseEntity.ok(response);
    }

    /**
     * Reset password endpoint
     * Validates reset token and updates user password
     * Token is invalidated after use (one-time use only)
     *
     * Endpoint: POST /api/v1/auth/password/reset
     */
    @PostMapping("/password/reset")
    public ResponseEntity<Map<String, String>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        try {
            // Process password reset
            authenticationService.resetPassword(request);

            Map<String, String> response = new HashMap<>();
            response.put("message", "Password reset successfully. You can now login with your new password.");
            response.put("status", "success");

            return ResponseEntity.ok(response);
        } catch (PasswordResetException e) {
            Map<String, String> response = new HashMap<>();
            response.put("message", e.getMessage());
            response.put("status", "error");

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

}