package com.swipelab.controller;

import com.swipelab.dto.request.CreateRecipientGroupRequest;
import com.swipelab.dto.request.CreateTaskRequest;
import com.swipelab.dto.request.UpdateRecipientGroupRequest;
import com.swipelab.dto.request.UpdateTaskRequest;
import com.swipelab.dto.response.*;
import com.swipelab.statistics.application.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import java.util.List;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminDashboardController {

    private final com.swipelab.tasks.application.TaskService taskService;
    private final com.swipelab.tasks.application.RecipientGroupService recipientGroupService;
    private final AnalyticsService analyticsService;

    // ===== TASKS =====

    @GetMapping("/tasks")
    public List<TaskResponse> getTasks() {
        return taskService.getAllTasks();
    }

    @PostMapping("/tasks/create")
    public TaskResponse createTask(@RequestBody CreateTaskRequest request) {
        return taskService.createTask(request);
    }

    @PostMapping("/tasks/archive/{taskId}")
    public TaskResponse archiveTask(@PathVariable Long taskId) {
        return taskService.archiveTask(taskId);
    }

    @PutMapping("/tasks/{taskId}")
    public TaskResponse updateTask(
            @PathVariable Long taskId,
            @RequestBody UpdateTaskRequest request) {
        return taskService.updateTask(taskId, request);
    }

    @PostMapping("/tasks/{taskId}/activate")
    public TaskResponse activateTask(@PathVariable Long taskId) {
        return taskService.activateTask(taskId);
    }

    @PostMapping("/tasks/{taskId}/pause")
    public TaskResponse pauseTask(@PathVariable Long taskId) {
        return taskService.pauseTask(taskId);
    }

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

    @GetMapping("/recipients")
    public List<RecipientGroupResponse> getRecipients() {
        return recipientGroupService.getRecipientGroups();
    }

    @PostMapping("/recipients/create")
    public RecipientGroupResponse createRecipients(
            @RequestBody CreateRecipientGroupRequest request) {
        return recipientGroupService.createRecipientGroup(request);
    }

    @DeleteMapping("/recipients/{groupId}")
    public void deleteRecipients(@PathVariable Long groupId) {
        recipientGroupService.deleteRecipientGroup(groupId);
    }

    @PutMapping("/recipients/{groupId}/update")
    public RecipientGroupResponse updateRecipients(
            @PathVariable Long groupId,
            @RequestBody UpdateRecipientGroupRequest request) {
        return recipientGroupService.updateRecipientGroup(groupId, request);
    }
}
