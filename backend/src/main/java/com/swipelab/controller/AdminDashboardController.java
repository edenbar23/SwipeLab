package com.swipelab.controller;

import com.swipelab.dto.request.CreateTaskRequest;
import com.swipelab.dto.request.UpdateTaskRequest;
import com.swipelab.dto.response.*;
// Note: TaskAnalyticsResponse might be in dto.response package too. 
// We want com.swipelab.analytics.dto.TaskAnalyticsResponse
// So we import it explicitly to override the star import if needed.
import com.swipelab.analytics.dto.TaskAnalyticsResponse;
import com.swipelab.analytics.application.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminDashboardController {

    private final com.swipelab.tasks.application.TaskService taskService;
    private final AnalyticsService analyticsService;

    // ===== ANALYTICS =====

    @GetMapping("/tasks/{taskId}/analytics")
    public TaskAnalyticsResponse taskAnalytics(@PathVariable Long taskId) {
        return analyticsService.getTaskAnalytics(taskId);
    }

    @GetMapping("/analytics/users")
    public List<UserPerformanceResponse> getUserPerformance(@RequestParam(required = false) Long taskId) {
        return analyticsService.getUserPerformanceMetrics(taskId);
    }

    @GetMapping("/analytics/top-performers")
    public List<UserPerformanceResponse> getTopPerformers(@RequestParam(defaultValue = "10") int limit) {
        return analyticsService.getTopPerformers(limit);
    }

    // ===== RECIPIENTS =====

}
