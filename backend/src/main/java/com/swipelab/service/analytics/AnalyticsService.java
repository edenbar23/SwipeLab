package com.swipelab.service.analytics;

import com.swipelab.dto.response.TaskAnalyticsResponse;
import com.swipelab.dto.response.UserPerformanceResponse;
import com.swipelab.exception.ResourceNotFoundException;
import com.swipelab.classification.domain.Classification;
import com.swipelab.classification.domain.Image;
import com.swipelab.tasks.domain.Task;
import com.swipelab.model.entity.User;
import com.swipelab.classification.infrastructure.ClassificationRepository;
import com.swipelab.classification.infrastructure.ImageRepository;
import com.swipelab.tasks.infrastructure.TaskRepository;
import com.swipelab.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final TaskRepository taskRepository;
    private final ImageRepository imageRepository;
    private final ClassificationRepository classificationRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public TaskAnalyticsResponse getTaskAnalytics(Long taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found: " + taskId));

        List<Image> images = imageRepository.findByTaskId(taskId);
        int totalImages = images.size();

        // Get all classifications for this task's images
        Set<String> classifiers = new HashSet<>();
        int totalClassifications = 0;
        int classifiedImages = 0;
        int lowConsensus = 0;
        int highConsensus = 0;
        double totalConsensus = 0;
        Map<String, Long> labelDistribution = new HashMap<>();

        for (Image image : images) {
            List<Classification> classifications = classificationRepository.findByImageId(image.getId());
            if (!classifications.isEmpty()) {
                classifiedImages++;
                totalClassifications += classifications.size();

                // Track unique classifiers
                for (Classification c : classifications) {
                    classifiers.add(c.getUser().getUsername());

                    // Count label distribution
                    String labelName = c.getLabel().getName();
                    labelDistribution.merge(labelName, 1L, Long::sum);
                }

                // Calculate consensus for this image
                double consensus = calculateImageConsensus(classifications);
                totalConsensus += consensus;

                if (consensus < 0.6) {
                    lowConsensus++;
                } else if (consensus >= 0.8) {
                    highConsensus++;
                }
            }
        }

        double avgConsensus = classifiedImages > 0 ? totalConsensus / classifiedImages : 0;
        double completionPct = totalImages > 0 ? (double) classifiedImages / totalImages * 100 : 0;

        return TaskAnalyticsResponse.builder()
                .taskId(taskId)
                .taskName(task.getTitle())
                .status(task.getStatus().name())
                .totalImages(totalImages)
                .classifiedImages(classifiedImages)
                .completionPercentage(Math.round(completionPct * 10) / 10.0)
                .averageConsensus(Math.round(avgConsensus * 100) / 100.0)
                .lowConsensusCount(lowConsensus)
                .highConsensusCount(highConsensus)
                .labelDistribution(labelDistribution)
                .totalClassifications(totalClassifications)
                .uniqueClassifiers(classifiers.size())
                .build();
    }

    @Transactional(readOnly = true)
    public List<UserPerformanceResponse> getUserPerformanceMetrics(Long taskId) {
        List<User> users = userRepository.findAll();

        return users.stream()
                .filter(user -> user.getTotalClassifications() > 0)
                .map(this::mapToPerformanceResponse)
                .sorted((u1, u2) -> Double.compare(u2.getGoldAccuracy(), u1.getGoldAccuracy()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<UserPerformanceResponse> getTopPerformers(int limit) {
        return userRepository.findAll().stream()
                .filter(user -> user.getTotalGoldClassifications() > 0)
                .map(this::mapToPerformanceResponse)
                .sorted((u1, u2) -> Double.compare(u2.getGoldAccuracy(), u1.getGoldAccuracy()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    private double calculateImageConsensus(List<Classification> classifications) {
        if (classifications.isEmpty())
            return 0;

        // Find the most common label
        Map<Long, Long> labelCounts = classifications.stream()
                .collect(Collectors.groupingBy(c -> c.getLabel().getId(), Collectors.counting()));

        long maxCount = labelCounts.values().stream().max(Long::compareTo).orElse(0L);
        return (double) maxCount / classifications.size();
    }

    private UserPerformanceResponse mapToPerformanceResponse(User user) {
        double goldAccuracy = 0;
        if (user.getTotalGoldClassifications() != null && user.getTotalGoldClassifications() > 0) {
            int correct = user.getCorrectGoldClassifications() != null ? user.getCorrectGoldClassifications() : 0;
            goldAccuracy = (double) correct / user.getTotalGoldClassifications() * 100;
        }

        return UserPerformanceResponse.builder()
                .username(user.getUsername())
                .displayName(user.getDisplayName())
                .totalClassifications(user.getTotalClassifications() != null ? user.getTotalClassifications() : 0)
                .goldImageClassifications(
                        user.getTotalGoldClassifications() != null ? user.getTotalGoldClassifications() : 0)
                .correctGoldClassifications(
                        user.getCorrectGoldClassifications() != null ? user.getCorrectGoldClassifications() : 0)
                .goldAccuracy(Math.round(goldAccuracy * 10) / 10.0)
                .credibilityScore(user.getCredibilityScore() != null ? user.getCredibilityScore() : 0)
                .currentStreak(user.getCurrentStreak() != null ? user.getCurrentStreak() : 0)
                .points(user.getPoints() != null ? user.getPoints() : 0)
                .build();
    }
}
