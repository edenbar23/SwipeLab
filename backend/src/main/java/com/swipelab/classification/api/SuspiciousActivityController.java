package com.swipelab.classification.api;

import com.swipelab.classification.application.SuspiciousActivityService;
import com.swipelab.dto.response.SuspiciousActivityResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Endpoints for viewing and managing the suspicious activity audit log.
 *
 * GET  /api/admin/suspicious-activity              – full audit log (Researcher+)
 * GET  /api/admin/suspicious-activity/{username}   – log for a specific user
 * POST /api/admin/suspicious-activity/{username}/reset – clear strikes (Super Admin only)
 */
@RestController
@RequestMapping("/api/admin/suspicious-activity")
@PreAuthorize("hasRole('RESEARCHER') or @securityAuthorizationService.isSuperAdmin(authentication.name)")
@RequiredArgsConstructor
public class SuspiciousActivityController {

    private final SuspiciousActivityService suspiciousActivityService;

    /** Full audit log — all users, all severities, ordered by most recent first. */
    @GetMapping
    public ResponseEntity<List<SuspiciousActivityResponse>> getAllActivity() {
        return ResponseEntity.ok(suspiciousActivityService.getAllActivity());
    }

    /** Audit log filtered to a single user. */
    @GetMapping("/{username}")
    public ResponseEntity<List<SuspiciousActivityResponse>> getActivityForUser(
            @PathVariable String username) {
        return ResponseEntity.ok(suspiciousActivityService.getActivityForUser(username));
    }

    /**
     * Clears the user's strike counter and restores their status to ACTIVE.
     * Intended for false-positive remediation by Super Admin.
     * Requires Super Admin — Researchers can view but not modify.
     */
    @PreAuthorize("@securityAuthorizationService.isSuperAdmin(authentication.name)")
    @PostMapping("/{username}/reset")
    public ResponseEntity<Map<String, String>> resetStrikes(@PathVariable String username) {
        suspiciousActivityService.resetStrikes(username);
        return ResponseEntity.ok(Map.of(
                "username", username,
                "message", "Strikes and warnings cleared. User status restored to ACTIVE."));
    }
}
