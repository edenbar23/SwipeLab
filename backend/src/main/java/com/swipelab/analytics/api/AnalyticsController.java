package com.swipelab.analytics.api;

import com.swipelab.analytics.application.AnalyticsService;
import com.swipelab.analytics.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<UserProgressResponse> getProgress(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(analyticsService.getUserProgress(userDetails.getUsername()));
    }

    // 2. User Statistics (Base Path: /api/v1/statistics)

    @GetMapping("/api/v1/statistics/me")
    public ResponseEntity<UserStatisticsResponse> getUserStatistics(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(analyticsService.getUserStatistics(userDetails.getUsername()));
    }

    @GetMapping("/api/v1/statistics/me/vs-experts")
    public ResponseEntity<UserVsExpertsResponse> getUserVsExperts(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(analyticsService.getUserVsExperts(userDetails.getUsername()));
    }

    @GetMapping("/api/v1/statistics/me/vs-users")
    public ResponseEntity<UserVsExpertsResponse> getUserVsUsers(@AuthenticationPrincipal UserDetails userDetails) {
        // Re-using same response structure or method for now as per confusion
        // Or implement separate logic.
        return ResponseEntity.ok(analyticsService.getUserVsExperts(userDetails.getUsername()));
    }

    @GetMapping("/api/v1/statistics/me/breakdown")
    public ResponseEntity<PerformanceBreakdownResponse> getBreakdown(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(analyticsService.getPerformanceBreakdown(userDetails.getUsername()));
    }

    @GetMapping("/api/v1/statistics/me/timeseries")
    public ResponseEntity<TimeSeriesResponse> getTimeSeries(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "accuracy") String metric,
            @RequestParam(defaultValue = "30d") String period) {
        return ResponseEntity.ok(analyticsService.getTimeSeries(userDetails.getUsername(), metric, period));
    }

    // 3. Task Analytics (Base Path: /dashboard)
    @GetMapping("/dashboard/tasks/{taskId}/analytics")
    public ResponseEntity<TaskAnalyticsResponse> getTaskAnalytics(
            @PathVariable Long taskId,
            @RequestParam(required = false) Boolean includePerSpecies,
            @RequestParam(required = false) Boolean includeTimeSeries) {
        return ResponseEntity.ok(analyticsService.getTaskAnalytics(taskId));
    }

    @PostMapping("/dashboard/exports")
    public ResponseEntity<Map<String, Object>> createExport(@RequestBody Map<String, Object> request) {
        // Placeholder for export
        return ResponseEntity.accepted().body(Map.of(
                "exportId", "exp_" + UUID.randomUUID(),
                "status", "QUEUED",
                "createdAt", LocalDateTime.now(),
                "estimatedCompletion", LocalDateTime.now().plusMinutes(10)));
    }
}
