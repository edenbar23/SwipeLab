package com.swipelab.analytics.application;

import com.swipelab.dto.response.DashboardStatsResponse;
import com.swipelab.classification.infrastructure.ClassificationRepository;
import com.swipelab.classification.infrastructure.ImageRepository;
import com.swipelab.tasks.infrastructure.TaskRepository;
import com.swipelab.users.infrastructure.UserRepository;
import com.swipelab.tasks.domain.TaskStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final UserRepository userRepository;
    private final ClassificationRepository classificationRepository;
    private final TaskRepository taskRepository;
    private final ImageRepository imageRepository;

    public DashboardStatsResponse getGlobalStats() {
        return DashboardStatsResponse.builder()
                .totalUsers(userRepository.count())
                .totalSwipes(classificationRepository.count())
                .activeTasks(taskRepository.countByStatus(TaskStatus.ACTIVE))
                .totalImages(imageRepository.count())
                .build();
    }
}
