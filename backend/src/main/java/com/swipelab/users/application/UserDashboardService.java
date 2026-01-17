package com.swipelab.users.application;

import com.swipelab.dto.response.dashboard.*;
import com.swipelab.exception.ResourceNotFoundException;
import com.swipelab.classification.domain.Image;
import com.swipelab.tasks.domain.Task;
import com.swipelab.users.domain.User;
import com.swipelab.classification.infrastructure.ImageRepository;
import com.swipelab.tasks.infrastructure.TaskRepository;
import com.swipelab.users.infrastructure.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserDashboardService {

    private final TaskRepository taskRepository;
    private final ImageRepository imageRepository;
    private final UserRepository userRepository;

    // =========================
    // 5.1 MY TASKS
    // =========================

    public MyTasksPageResponse getMyTasks(int page, int pageSize) {
        throw new IllegalArgumentException("not implemented yet");
    }

    // =========================
    // 5.2 TASK DETAILS
    // =========================

    public MyTaskDetailsResponse getTaskDetails(Long taskId) {
        throw new IllegalArgumentException("not implemented yet");
    }

    // =========================
    // 5.3 PLAY TASK
    // =========================

    public PlayTaskResponse playTask(Long taskId, int count) {
        throw new IllegalArgumentException("not implemented yet");
    }

}
