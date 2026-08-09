package com.swipelab.analytics.api;

import com.swipelab.analytics.application.AnalyticsService;
import com.swipelab.analytics.dto.*;
import com.swipelab.analytics.dto.DashboardStatsResponse;
import com.swipelab.analytics.dto.UserPerformanceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    // ─── User-scoped endpoints ────────────────────────────────────────────────

    @GetMapping("/api/v1/classifications/progress")
    @PreAuthorize("hasAnyRole('USER', 'RESEARCHER') or @securityAuthorizationService.isSuperAdmin(authentication.name)")
    public ResponseEntity<UserProgressResponse> getProgress(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(analyticsService.getUserProgress(userDetails.getUsername()));
    }

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
    public ResponseEntity<UserVsUsersResponse> getUserVsUsers(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(analyticsService.getUserVsUsers(userDetails.getUsername()));
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

    // ─── Platform overview (Researcher / SuperAdmin) ──────────────────────────

    /**
     * Returns a platform-wide snapshot broken down by today / this week / this month:
     * - classifications count
     * - unique images classified
     * - unique users active
     * - unique tasks involved
     * - unique experiments involved
     * Also returns a 30-day confidence trend and label-distribution breakdown.
     */
    @GetMapping("/api/v1/analytics/overview")
    @PreAuthorize("hasRole('RESEARCHER') or @securityAuthorizationService.isSuperAdmin(authentication.name)")
    public ResponseEntity<PlatformOverviewResponse> getPlatformOverview() {
        return ResponseEntity.ok(analyticsService.getPlatformOverview());
    }

    /**
     * Exposes the global totals (users, swipes, active tasks, images).
     * Useful as a lightweight header stat for the researcher dashboard.
     */
    @GetMapping("/api/v1/analytics/global-stats")
    @PreAuthorize("hasRole('RESEARCHER') or @securityAuthorizationService.isSuperAdmin(authentication.name)")
    public ResponseEntity<DashboardStatsResponse> getGlobalStats() {
        return ResponseEntity.ok(analyticsService.getGlobalStats());
    }

    // ─── Task-scoped endpoints (Researcher / SuperAdmin) ─────────────────────

    @GetMapping("/api/v1/analytics/tasks/{taskId}")
    @PreAuthorize("hasRole('RESEARCHER') or @securityAuthorizationService.isSuperAdmin(authentication.name)")
    public ResponseEntity<TaskAnalyticsResponse> getTaskAnalytics(
            @PathVariable Long taskId,
            @RequestParam(required = false) Boolean includePerSpecies,
            @RequestParam(required = false) Boolean includeTimeSeries) {
        return ResponseEntity.ok(analyticsService.getTaskAnalytics(taskId));
    }

    // ─── Admin user-performance endpoints (Researcher / SuperAdmin) ───────────

    @GetMapping("/api/v1/analytics/users")
    @PreAuthorize("hasRole('RESEARCHER') or @securityAuthorizationService.isSuperAdmin(authentication.name)")
    public ResponseEntity<List<UserPerformanceResponse>> getUserPerformance(
            @RequestParam(required = false) Long taskId) {
        return ResponseEntity.ok(analyticsService.getUserPerformanceMetrics(taskId));
    }

    @GetMapping("/api/v1/analytics/top-performers")
    @PreAuthorize("hasRole('RESEARCHER') or @securityAuthorizationService.isSuperAdmin(authentication.name)")
    public ResponseEntity<List<UserPerformanceResponse>> getTopPerformers(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(analyticsService.getTopPerformers(limit));
    }
}






