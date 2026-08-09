package com.swipelab.auth.api;


import com.swipelab.auth.dto.*;
import com.swipelab.classification.dto.*;
import com.swipelab.tasks.dto.*;
import com.swipelab.users.dto.*;
import com.swipelab.recipients.dto.*;
import com.swipelab.auth.dto.AuthResponse;
import com.swipelab.users.dto.UserProfileResponse;

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
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Cookie;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationService authenticationService;
    private final UserService userService;
    private final com.swipelab.auth.application.OAuth2Service oAuth2Service;
    private final com.swipelab.auth.domain.AuthMapper authMapper;
    private final com.swipelab.auth.application.JwtService jwtService;
    private final SpringTemplateEngine templateEngine;

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

    private void clearAuthCookies(HttpServletResponse response) {
        Cookie accessCookie = new Cookie("accessToken", "");
        accessCookie.setHttpOnly(true);
        accessCookie.setPath("/");
        accessCookie.setMaxAge(0);

        Cookie refreshCookie = new Cookie("refreshToken", "");
        refreshCookie.setHttpOnly(true);
        refreshCookie.setPath("/");
        refreshCookie.setMaxAge(0);

        response.addCookie(accessCookie);
        response.addCookie(refreshCookie);
    }

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
     * Verify user email with token via browser GET (link clicked in email).
     *
     * Security: previously used raw string concatenation of frontendUrl and e.getMessage()
     * into HTML, creating a Reflected XSS vector. Now uses Thymeleaf templates where all
     * dynamic values are rendered via th:text (auto-escaped) or safe data attributes.
     * The raw exception message is never forwarded to the client.
     */
    @GetMapping(value = "/verify-email", produces = org.springframework.http.MediaType.TEXT_HTML_VALUE)
    public String verifyEmailLink(@RequestParam String token) {
        Context ctx = new Context();
        try {
            authenticationService.verifyEmail(token);
            // frontendUrl is a server-side config value, not user input.
            // Rendered into a data attribute via th:data-url so the redirect
            // JS never uses string concatenation into the page source.
            ctx.setVariable("frontendUrl", frontendUrl);
            return templateEngine.process("auth/email-verify-success", ctx);
        } catch (Exception e) {
            // Log the full exception server-side; only a generic category
            // is surfaced to the client — never the raw exception message.
            org.slf4j.LoggerFactory.getLogger(getClass())
                    .warn("Email verification failed for token [{}]: {}", token, e.getMessage());
            ctx.setVariable("errorMessage", "The verification link is invalid or has expired.");
            return templateEngine.process("auth/email-verify-failure", ctx);
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
            @Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        AuthResponse authResponse = authenticationService.login(request);
        setAuthCookies(response, authResponse.getAccessToken(), authResponse.getRefreshToken());
        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            HttpServletRequest request, HttpServletResponse response) {

        String refreshToken = null;
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            refreshToken = authorizationHeader.substring(7);
        } else if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("refreshToken".equals(cookie.getName())) {
                    refreshToken = cookie.getValue();
                }
            }
        }

        if (refreshToken == null) {
            throw new UnauthorizedException("Missing refresh token");
        }

        AuthResponse authResponse = authenticationService.refresh(refreshToken);
        setAuthCookies(response, authResponse.getAccessToken(), authResponse.getRefreshToken());
        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            HttpServletRequest request, HttpServletResponse response) {

        String refreshToken = null;
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            refreshToken = authorizationHeader.substring(7);
        } else if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("refreshToken".equals(cookie.getName())) {
                    refreshToken = cookie.getValue();
                }
            }
        }

        if (refreshToken != null) {
            authenticationService.logout(refreshToken);
        }

        clearAuthCookies(response);
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
    public ResponseEntity<AuthResponse> loginGoogle(@RequestBody Map<String, String> payload, HttpServletResponse response) {
        String credential = payload.get("credential");
        if (credential == null) {
            credential = payload.get("idToken");
        }
        if (credential == null) {
            credential = payload.get("accessToken");
        }

        if (credential == null) {
            throw new IllegalArgumentException("Missing Google token");
        }

        String email, name, picture, providerId;

        // An id_token is a JWT: three base64 parts separated by dots.
        // An access_token is an opaque string that does NOT have that structure.
        if (credential.split("\\.").length == 3) {
            // --- id_token path ---
            com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload googlePayload =
                    oAuth2Service.verifyGoogleToken(credential);

            email      = googlePayload.getEmail();
            name       = (String) googlePayload.get("name");
            picture    = (String) googlePayload.get("picture");
            providerId = googlePayload.getSubject();
        } else {
            // --- access_token path (mobile / Expo Go) ---
            java.util.Map<String, Object> userInfo = oAuth2Service.verifyGoogleAccessToken(credential);

            email      = (String) userInfo.get("email");
            name       = (String) userInfo.get("name");
            picture    = (String) userInfo.get("picture");
            providerId = (String) userInfo.get("sub");
        }

        if (email == null) {
            throw new IllegalArgumentException("Could not retrieve email from Google token");
        }

        User user = oAuth2Service.processUserFromIdToken(email, name, picture, providerId);

        String accessToken  = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        setAuthCookies(response, accessToken, refreshToken);
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
     * Change password for the currently authenticated user
     *
     * Endpoint: POST /api/v1/auth/password/change
     */
    @PostMapping("/password/change")
    public ResponseEntity<Map<String, String>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request) {
        
        authenticationService.changePassword(request);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Password changed successfully.");
        response.put("status", "success");

        return ResponseEntity.ok(response);
    }

    /**
     * Send an invitation email to a new admin or researcher.
     * Restricted to the Super Admin — uses the same SpEL bean check
     * as all other privileged endpoints in this application.
     *
     * Endpoint: POST /api/v1/auth/invitation/admin
     */
    @PostMapping("/invitation/admin")
    @org.springframework.security.access.prepost.PreAuthorize("@securityAuthorizationService.isSuperAdmin(authentication.name)")
    public ResponseEntity<Map<String, String>> inviteAdmin(
            @Valid @RequestBody InviteAdminRequest request) {

        authenticationService.inviteAdmin(request);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Invitation sent successfully to " + request.getEmail());
        response.put("status", "success");

        return ResponseEntity.ok(response);
    }

}





