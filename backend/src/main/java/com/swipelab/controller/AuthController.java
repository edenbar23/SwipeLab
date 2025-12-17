package com.swipelab.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

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

    @GetMapping("/test")
    public ResponseEntity<?> testEndpoint() {
        return ResponseEntity.ok(Map.of(
                "message", "Security configuration is working!",
                "timestamp", System.currentTimeMillis()
        ));
    }
}