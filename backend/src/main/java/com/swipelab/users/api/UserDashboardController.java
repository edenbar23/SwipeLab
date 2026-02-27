package com.swipelab.users.api;

import com.swipelab.dto.response.dashboard.*;
import com.swipelab.users.application.UserDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/v1/dashboard")
@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
@RequiredArgsConstructor
public class UserDashboardController {

    private final UserDashboardService userDashboardService;

    @GetMapping("/my-tasks")
    @ResponseBody
    public MyTasksPageResponse getMyTasks(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return userDashboardService.getMyTasks(page, pageSize);
    }

    @GetMapping("/my-tasks/{taskId}")
    @ResponseBody
    public MyTaskDetailsResponse getTaskDetails(@PathVariable Long taskId) {
        return userDashboardService.getTaskDetails(taskId);
    }

    @GetMapping("/my-tasks/{taskId}/play")
    @ResponseBody
    public PlayTaskResponse playTask(
            @PathVariable Long taskId,
            @RequestParam(defaultValue = "10") int count) {
        return userDashboardService.playTask(taskId, count);
    }
}
