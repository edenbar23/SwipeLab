package com.swipelab.analytics.api;

import com.swipelab.analytics.application.AnalyticsService;
import com.swipelab.analytics.dto.*;
import com.swipelab.dto.response.UserPerformanceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    // 1. Progress (mapped to /api/v1/classifications/progress or
    // /api/v1/statistics/progress?)
    // User request: GET /api/v1/classifications/progress
    @GetMapping("/api/v1/classifications/progress")
    @PreAuthorize("hasAnyRole('USER', 'RESEARCHER') or @securityAuthorizationService.isSuperAdmin(authentication.name)")
    public ResponseEntity<UserProgressResponse> getProgress(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(analyticsService.getUserProgress(userDetails.getUsername()));
    }

    // 2. User Statistics (Base Path: /api/v1/statistics)

    @GetMapping("/api/v1/statistics/me")
    @PreAuthorize("hasAnyRole('USER', 'RESEARCHER') or @securityAuthorizationService.isSuperAdmin(authentication.name)")
    public ResponseEntity<UserStatisticsResponse> getUserStatistics(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(analyticsService.getUserStatistics(userDetails.getUsername()));
    }

    @GetMapping("/api/v1/statistics/me/vs-experts")
    @PreAuthorize("hasAnyRole('USER', 'RESEARCHER') or @securityAuthorizationService.isSuperAdmin(authentication.name)")
    public ResponseEntity<UserVsExpertsResponse> getUserVsExperts(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(analyticsService.getUserVsExperts(userDetails.getUsername()));
    }

    @GetMapping("/api/v1/statistics/me/vs-users")
    @PreAuthorize("hasAnyRole('USER', 'RESEARCHER') or @securityAuthorizationService.isSuperAdmin(authentication.name)")
    public ResponseEntity<UserVsExpertsResponse> getUserVsUsers(@AuthenticationPrincipal UserDetails userDetails) {
        // Re-using same response structure or method for now as per confusion
        // Or implement separate logic.
        return ResponseEntity.ok(analyticsService.getUserVsExperts(userDetails.getUsername()));
    }

    @GetMapping("/api/v1/statistics/me/breakdown")
    @PreAuthorize("hasAnyRole('USER', 'RESEARCHER') or @securityAuthorizationService.isSuperAdmin(authentication.name)")
    public ResponseEntity<PerformanceBreakdownResponse> getBreakdown(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(analyticsService.getPerformanceBreakdown(userDetails.getUsername()));
    }

    @GetMapping("/api/v1/statistics/me/timeseries")
    @PreAuthorize("hasAnyRole('USER', 'RESEARCHER') or @securityAuthorizationService.isSuperAdmin(authentication.name)")
    public ResponseEntity<TimeSeriesResponse> getTimeSeries(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "accuracy") String metric,
            @RequestParam(defaultValue = "30d") String period) {
        return ResponseEntity.ok(analyticsService.getTimeSeries(userDetails.getUsername(), metric, period));
    }

    // ADMIN ANALYTICS SECTION

    // 3. Task Analytics (Base Path: /dashboard)
    @GetMapping("api/v1/analytics/tasks/{taskId}")
    @PreAuthorize("hasRole('RESEARCHER') or @securityAuthorizationService.isSuperAdmin(authentication.name)")
    public ResponseEntity<TaskAnalyticsResponse> getTaskAnalytics(
            @PathVariable Long taskId,
            @RequestParam(required = false) Boolean includePerSpecies,
            @RequestParam(required = false) Boolean includeTimeSeries) {
        return ResponseEntity.ok(analyticsService.getTaskAnalytics(taskId));
    }

    @PostMapping("api/v1/analytics/exports")
    @PreAuthorize("hasRole('RESEARCHER') or @securityAuthorizationService.isSuperAdmin(authentication.name)")
    public ResponseEntity<Map<String, Object>> createExport(@RequestBody Map<String, Object> request) {
        // Placeholder for export
        return ResponseEntity.accepted().body(Map.of(
                "exportId", "exp_" + UUID.randomUUID(),
                "status", "QUEUED",
                "createdAt", LocalDateTime.now(),
                "estimatedCompletion", LocalDateTime.now().plusMinutes(10)));
    }

    @GetMapping("api/v1/analytics/users")
    @PreAuthorize("hasRole('RESEARCHER') or @securityAuthorizationService.isSuperAdmin(authentication.name)")
    public List<UserPerformanceResponse> getUserPerformance(@RequestParam(required = false) Long taskId) {
        return analyticsService.getUserPerformanceMetrics(taskId);
    }

    @GetMapping("api/v1/analytics/top-performers")
    @PreAuthorize("hasRole('RESEARCHER') or @securityAuthorizationService.isSuperAdmin(authentication.name)")
    public List<UserPerformanceResponse> getTopPerformers(@RequestParam(defaultValue = "10") int limit) {
        return analyticsService.getTopPerformers(limit);
    }
}
