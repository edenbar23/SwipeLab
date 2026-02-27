package com.swipelab.users.application;

import com.swipelab.classification.infrastructure.ImageRepository;
import com.swipelab.tasks.infrastructure.TaskRepository;
import com.swipelab.users.infrastructure.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class UserDashboardServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private ImageRepository imageRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserDashboardService userDashboardService;

    @Test
    void getMyTasks_ShouldThrowNotImplemented() {
        assertThrows(IllegalArgumentException.class, () -> userDashboardService.getMyTasks(1, 20));
    }

    @Test
    void getTaskDetails_ShouldThrowNotImplemented() {
        assertThrows(IllegalArgumentException.class, () -> userDashboardService.getTaskDetails(1L));
    }

    @Test
    void playTask_ShouldThrowNotImplemented() {
        assertThrows(IllegalArgumentException.class, () -> userDashboardService.playTask(1L, 10));
    }
}
