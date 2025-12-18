package com.swipelab.controller;

import com.swipelab.dto.request.EmailVerificationRequest;
import com.swipelab.dto.request.RegisterRequest;
import com.swipelab.dto.response.AuthResponse;
import com.swipelab.exception.EmailVerificationException;
import com.swipelab.service.auth.AuthenticationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationService authenticationService;

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
}