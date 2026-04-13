package com.swipelab.auth.api;

import com.swipelab.dto.request.*;
import com.swipelab.dto.response.AuthResponse;
import com.swipelab.dto.response.UserProfileResponse;

import com.swipelab.exception.UnauthorizedException;
import com.swipelab.auth.application.AuthenticationService;
import com.swipelab.users.application.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import com.swipelab.users.domain.User;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationService authenticationService;

    private final UserService userService;
    private final com.swipelab.auth.application.OAuth2Service oAuth2Service;
    private final com.swipelab.auth.domain.AuthMapper authMapper;
    private final com.swipelab.auth.application.JwtService jwtService;

    /**
     * Register a new user
     */
    @PostMapping("/register")
    public ResponseEntity<java.util.Map<String, Object>> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authenticationService.register(request));
    }

    /**
     * Verify user email with token
     * Endpoint: POST /api/v1/auth/email/verify
     */
    @PostMapping("/email/verify")
    public ResponseEntity<Map<String, String>> verifyEmail(
            @Valid @RequestBody EmailVerificationRequest request) {
        authenticationService.verifyEmail(request.getToken());

        Map<String, String> response = new HashMap<>();
        response.put("message", "Email verified successfully");
        response.put("status", "success");

        return ResponseEntity.ok(response);
    }

    @Value("${app.frontend-url}")
    private String frontendUrl;

    /**
     * Verify user email with token via browser GET (Link clicked in email)
     */
    @GetMapping(value = "/verify-email", produces = org.springframework.http.MediaType.TEXT_HTML_VALUE)
    public String verifyEmailLink(@RequestParam String token) {
        try {
            authenticationService.verifyEmail(token);
            return "<html><head><meta http-equiv=\"refresh\" content=\"5;url=" + frontendUrl + "\" /></head>" +
                   "<body style='font-family:sans-serif; text-align:center; padding-top: 50px; background-color: #f4f6f9;'>" +
                   "<div style='max-width: 600px; margin: 0 auto; background: white; padding: 40px; border-radius: 8px; box-shadow: 0 4px 6px rgba(0,0,0,0.1);'>" +
                   "<h2 style='color: #4B7BE5;'>Email Verified Successfully! <br>&#10004;</h2>" +
                   "<p style='font-size: 16px; color: #333;'>Your account is now fully active.</p>" +
                   "<p style='font-size: 16px; color: #666;'>You will be redirected back to the app to log in shortly.</p>" +
                   "<p style='font-size: 14px; color: #999; margin-top: 20px;'>Redirecting in <span id='countdown'>5</span> seconds...</p>" +
                   "<script>var time=5; setInterval(function(){ if(time>0){time--; document.getElementById('countdown').innerText=time;} if(time===0){time--; window.location.href='" + frontendUrl + "';} }, 1000);</script>" +
                   "</div></body></html>";
        } catch (Exception e) {
            return "<html><body style='font-family:sans-serif; text-align:center; padding-top: 50px; background-color: #f4f6f9;'>" +
                   "<div style='max-width: 600px; margin: 0 auto; background: white; padding: 40px; border-radius: 8px; box-shadow: 0 4px 6px rgba(0,0,0,0.1);'>" +
                   "<h2 style='color: #E11D48;'>Verification Failed &#10006;</h2>" +
                   "<p style='font-size: 16px; color: #333;'>" + e.getMessage() + "</p>" +
                   "<p style='font-size: 16px; color: #666;'>Please try registering again or contact support.</p>" +
                   "</div></body></html>";
        }
    }

    /**
     * Resend verification email
     * Endpoint: POST /api/v1/auth/email/resend
     */
    @PostMapping("/email/resend")
    public ResponseEntity<Map<String, String>> resendVerificationEmail(
            @RequestParam String email) {
        authenticationService.resendVerificationEmail(email);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Verification email sent successfully");
        response.put("status", "success");

        return ResponseEntity.ok(response);
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

        // Use mapper
        return ResponseEntity.ok(authMapper.toAuthResponse(accessToken, refreshToken, user));
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
        // Process password reset
        authenticationService.resetPassword(request);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Password reset successfully. You can now login with your new password.");
        response.put("status", "success");

        return ResponseEntity.ok(response);
    }

    /**
     * Send an invitation email to a new admin or researcher
     *
     * Endpoint: POST /api/v1/auth/invitation/admin
     */
    @PostMapping("/invitation/admin")
    // @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> inviteAdmin(
            @Valid @RequestBody InviteAdminRequest request) {

        authenticationService.inviteAdmin(request);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Invitation sent successfully to " + request.getEmail());
        response.put("status", "success");

        return ResponseEntity.ok(response);
    }

}