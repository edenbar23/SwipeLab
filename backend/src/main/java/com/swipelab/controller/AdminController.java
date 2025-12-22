package com.swipelab.controller;

import com.swipelab.repository.UserRepository;
import com.swipelab.service.user.CredibilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final CredibilityService credibilityService;
    private final UserRepository userRepository;

    @PostMapping("/credibility/recalculate/{username}")
    public ResponseEntity<?> recalculateCredibility(@PathVariable String username) {
        credibilityService.updateOverallCredibilityScore(username);
        return ResponseEntity.ok(Map.of("message", "Credibility updated for " + username));
    }

    @PostMapping("/credibility/recalculate-all")
    public ResponseEntity<?> recalculateAllCredibility() {
        userRepository.findAll().forEach(user ->
                credibilityService.updateOverallCredibilityScore(user.getUsername())
        );
        return ResponseEntity.ok(Map.of("message", "All credibility scores updated"));
    }
}