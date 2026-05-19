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
    @PreAuthorize("hasAnyRole('USER', 'RESEARCHER') or @securityAuthorizationService.isSuperAdmin(authentication.name)")
    public ResponseEntity<TaskPageResponse> getMyTasks(
            @AuthenticationPrincipal UserDetails userDetails,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(taskService.getTasksForUser(userDetails.getUsername(), pageable));
    }

    @GetMapping("/available-tasks")
    @PreAuthorize("hasAnyRole('USER', 'RESEARCHER') or @securityAuthorizationService.isSuperAdmin(authentication.name)")
    public ResponseEntity<TaskPageResponse> getAvailableTasks(
            @AuthenticationPrincipal UserDetails userDetails,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(taskService.getExploreTasksForUser(userDetails.getUsername(), pageable));
    }

    @GetMapping("/my-tasks/{taskId}")
    @PreAuthorize("hasAnyRole('USER', 'RESEARCHER') or @securityAuthorizationService.isSuperAdmin(authentication.name)")
    public ResponseEntity<TaskResponse> getMyTaskDetails(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long taskId) {
        return ResponseEntity.ok(taskService.getTaskForUser(taskId, userDetails.getUsername()));
    }

    @PostMapping("/{taskId}/assign")
    @PreAuthorize("hasAnyRole('USER', 'RESEARCHER') or @securityAuthorizationService.isSuperAdmin(authentication.name)")
    public ResponseEntity<TaskResponse> assignTask(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long taskId) {
        TaskResponse response = taskService.assignTaskToUser(taskId, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // =========================
    // Admin Endpoints
    // =========================

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('RESEARCHER') or @securityAuthorizationService.isSuperAdmin(authentication.name)")
    public ResponseEntity<List<TaskResponse>> getResearcherDashboard(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(taskService.getResearcherDashboard(userDetails.getUsername()));
    }

    @GetMapping("/dashboard/{taskId}")
    @PreAuthorize("hasRole('RESEARCHER') or @securityAuthorizationService.isSuperAdmin(authentication.name)")
    // get task details admin view
    public ResponseEntity<TaskResponse> getTaskDetailsResearcher(@AuthenticationPrincipal UserDetails userDetails, @PathVariable Long taskId) {
        return ResponseEntity.ok(taskService.getTaskDetailsResearcher(taskId, userDetails.getUsername()));
    }

    @GetMapping("/dashboard/experiments")
    @PreAuthorize("hasRole('RESEARCHER') or @securityAuthorizationService.isSuperAdmin(authentication.name)")
    public ResponseEntity<List<ExternalExperimentDto>> getExperiments(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String token = authHeader.substring(7);
        return ResponseEntity.ok(stardbiClient.getExperiments(token));
    }

    @PostMapping("/create")
    @PreAuthorize("hasRole('RESEARCHER') or @securityAuthorizationService.isSuperAdmin(authentication.name)")
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
    @PreAuthorize("hasRole('RESEARCHER') or @securityAuthorizationService.isSuperAdmin(authentication.name)")
    public ResponseEntity<TaskResponse> archiveTask(@AuthenticationPrincipal UserDetails userDetails, @PathVariable Long taskId) {
        return ResponseEntity.ok(taskService.archiveTask(taskId, userDetails.getUsername()));
    }

    @PutMapping("/{taskId}")
    @PreAuthorize("hasRole('RESEARCHER') or @securityAuthorizationService.isSuperAdmin(authentication.name)")
    public ResponseEntity<TaskResponse> updateTask(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long taskId,
            @Valid @RequestBody UpdateTaskRequest request) {
        return ResponseEntity.ok(taskService.updateTask(taskId, request, userDetails.getUsername()));
    }

    @PostMapping("/{taskId}/activate")
    @PreAuthorize("hasRole('RESEARCHER') or @securityAuthorizationService.isSuperAdmin(authentication.name)")
    public ResponseEntity<TaskResponse> activateTask(@AuthenticationPrincipal UserDetails userDetails, @PathVariable Long taskId) {
        return ResponseEntity.ok(taskService.activateTask(taskId, userDetails.getUsername()));
    }

    @PostMapping("/{taskId}/pause")
    @PreAuthorize("hasRole('RESEARCHER') or @securityAuthorizationService.isSuperAdmin(authentication.name)")
    public ResponseEntity<TaskResponse> pauseTask(@AuthenticationPrincipal UserDetails userDetails, @PathVariable Long taskId) {
        return ResponseEntity.ok(taskService.pauseTask(taskId, userDetails.getUsername()));
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
