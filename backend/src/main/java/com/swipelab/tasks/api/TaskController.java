package com.swipelab.tasks.api;

import com.swipelab.dto.request.CreateTaskRequest;
import com.swipelab.dto.request.UpdateTaskRequest;
import com.swipelab.dto.response.TaskPageResponse;
import com.swipelab.dto.response.TaskResponse;
import com.swipelab.tasks.application.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    // =========================
    // User Endpoints
    // =========================

    @GetMapping("/my-tasks")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<TaskPageResponse> getMyTasks(
            @AuthenticationPrincipal UserDetails userDetails,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(taskService.getTasksForUser(userDetails.getUsername(), pageable));
    }

    @GetMapping("/my-tasks/{taskId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<TaskResponse> getMyTaskDetails(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long taskId) {
        return ResponseEntity.ok(taskService.getTaskForUser(taskId, userDetails.getUsername()));
    }

    // =========================
    // Admin Endpoints
    // =========================

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<TaskResponse>> getAdminDashboard() {
        // In real app, verify admin role
        return ResponseEntity.ok(taskService.getAdminDashboard());
    }

    @GetMapping("/dashboard/{taskId}")
    @PreAuthorize("hasRole('ADMIN')")
    // get task details admin view
    public ResponseEntity<TaskResponse> getTaskDetailsAdmin(@PathVariable Long taskId) {
        return ResponseEntity.ok(taskService.getTaskDetailsAdmin(taskId));
    }

    @PostMapping("/create")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TaskResponse> createTask(@Valid @RequestBody CreateTaskRequest request) {
        // In real app, verify admin role
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(taskService.createTask(request));
    }

    @PostMapping("/{taskId}/archive")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TaskResponse> archiveTask(@PathVariable Long taskId) {
        return ResponseEntity.ok(taskService.archiveTask(taskId));
    }

    @PutMapping("/{taskId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TaskResponse> updateTask(
            @PathVariable Long taskId,
            @Valid @RequestBody UpdateTaskRequest request) {
        return ResponseEntity.ok(taskService.updateTask(taskId, request));
    }

    @PostMapping("/{taskId}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TaskResponse> activateTask(@PathVariable Long taskId) {
        return ResponseEntity.ok(taskService.activateTask(taskId));
    }

    @PostMapping("/{taskId}/pause")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TaskResponse> pauseTask(@PathVariable Long taskId) {
        return ResponseEntity.ok(taskService.pauseTask(taskId));
    }

    // =========================
    // Legacy / Aliases (Optional, keeping for compatibility if needed)
    // =========================

    // Original "createTask" mapped to POST /api/v1/tasks
    // We can keep it or remove it. Contract says /create for Admin.
    // If I keep it, it might conflict with intention.
    // I will comment it out or relying on the specific endpoints above.

    // @PostMapping
    // public ResponseEntity<TaskResponse> create(@AuthenticationPrincipal
    // UserDetails userDetails, ...)

    // @GetMapping
    // public ResponseEntity<List<TaskResponse>> getAll() ...
    // This is replaced by /dashboard for admins or /my-tasks for users.
}
