package com.swipelab.tasks.api;

import com.swipelab.dto.request.CreateTaskRequest;
import com.swipelab.dto.request.UpdateTaskRequest;
import com.swipelab.dto.response.TaskPageResponse;
import com.swipelab.dto.response.TaskResponse;
import com.swipelab.integration.stardbi.StardbiClient;
import com.swipelab.integration.stardbi.dto.ExternalExperimentDto;
import com.swipelab.tasks.application.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;
    private final StardbiClient stardbiClient;

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

    @GetMapping("/available-tasks")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<TaskPageResponse> getAvailableTasks(
            @AuthenticationPrincipal UserDetails userDetails,
            @PageableDefault(size = 20) Pageable pageable) {
        log.info("Entered getAvailableTasks controller method for user: {}", userDetails.getUsername());
        try {
            TaskPageResponse response = taskService.getTasksForUser(userDetails.getUsername(), pageable);
            log.info("getAvailableTasks returning {} tasks for user: {}", response.getTasks().size(), userDetails.getUsername());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Fatal error in available-tasks endpoint: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
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

    @GetMapping("/dashboard/experiments")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ExternalExperimentDto>> getExperiments(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String token = authHeader.substring(7);
        return ResponseEntity.ok(stardbiClient.getExperiments(token));
    }

    @PostMapping("/create")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TaskResponse> createTask(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestHeader(value = "X-Stardbi-Access-Token", required = false) String explicitStardbiToken,
            @RequestHeader(value = "X-Stardbi-Refresh-Token", required = false) String stardbiRefreshToken,
            @Valid @RequestBody CreateTaskRequest request) {
        
        String stardbiAccessToken = explicitStardbiToken;
        if ((stardbiAccessToken == null || stardbiAccessToken.isEmpty()) && authHeader != null && authHeader.startsWith("Bearer ")) {
            stardbiAccessToken = authHeader.substring(7);
        }

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(taskService.createTask(request, userDetails.getUsername(), stardbiAccessToken, stardbiRefreshToken));
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
