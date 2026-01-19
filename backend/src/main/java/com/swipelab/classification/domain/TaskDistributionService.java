package com.swipelab.classification.domain;

import com.swipelab.classification.infrastructure.ClassificationRepository;
import com.swipelab.classification.infrastructure.ImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskDistributionService {

    private final ImageRepository imageRepository;
    private final ClassificationRepository classificationRepository;

    // Gold image insertion ratio: 1 gold image per 15 regular images
    private static final int GOLD_IMAGE_FREQUENCY = 15;

    // Session-based tracking for gold image insertion
    // In production, this should be stored in Redis or database per user session
    private final Map<String, Integer> userClassificationCount = new HashMap<>();

    /**
     * Get the next image for a user to classify.
     * Implements:
     * - Gold image insertion (1 in 15)
     * - Duplicate prevention
     * - Intelligent assignment (prioritize under-classified images)
     */
    @Transactional(readOnly = true)
    public Optional<Image> getNextImageForUser(String username, Long taskId) {
        // Get user's classification count for this session
        String sessionKey = username + "_" + taskId;
        int count = userClassificationCount.getOrDefault(sessionKey, 0);

        // Determine if this should be a gold image (every 15th image)
        boolean shouldBeGold = (count > 0) && (count % GOLD_IMAGE_FREQUENCY == 0);

        Optional<Image> nextImage;
        if (shouldBeGold) {
            nextImage = getNextGoldImage(username, taskId);
            // If no gold images available, fall back to regular images
            if (nextImage.isEmpty()) {
                nextImage = getNextRegularImage(username, taskId);
            }
        } else {
            nextImage = getNextRegularImage(username, taskId);
        }

        // Increment count for next call
        if (nextImage.isPresent()) {
            userClassificationCount.put(sessionKey, count + 1);
        }

        return nextImage;
    }

    /**
     * Get next gold standard image that user hasn't classified yet
     */
    private Optional<Image> getNextGoldImage(String username, Long taskId) {
        List<Image> goldImages = imageRepository.findGoldStandardImagesByTaskId(taskId);

        // Filter out images already classified by this user
        List<Image> unclassifiedGold = goldImages.stream()
                .filter(image -> !classificationRepository.existsByUsernameAndImageId(username, image.getId()))
                .collect(Collectors.toList());

        if (unclassifiedGold.isEmpty()) {
            return Optional.empty();
        }

        // Randomly select from available gold images
        Collections.shuffle(unclassifiedGold);
        return Optional.of(unclassifiedGold.get(0));
    }

    /**
     * Get next regular image with intelligent assignment.
     * Prioritizes images with fewer classifications.
     */
    private Optional<Image> getNextRegularImage(String username, Long taskId) {
        List<Image> regularImages = imageRepository.findRegularImagesByTaskId(taskId);

        // Filter out images already classified by this user
        List<Image> unclassifiedRegular = regularImages.stream()
                .filter(image -> !classificationRepository.existsByUsernameAndImageId(username, image.getId()))
                .collect(Collectors.toList());

        if (unclassifiedRegular.isEmpty()) {
            return Optional.empty();
        }

        // Count classifications for each image
        Map<Long, Long> classificationCounts = new HashMap<>();
        for (Image image : unclassifiedRegular) {
            long count = classificationRepository.countByImage_Id(image.getId());
            classificationCounts.put(image.getId(), count);
        }

        // Sort by: 1) Priority (descending), 2) Classification count (ascending)
        unclassifiedRegular.sort((img1, img2) -> {
            // First compare by priority (higher priority first)
            int priorityCompare = Integer.compare(img2.getPriority(), img1.getPriority());
            if (priorityCompare != 0) {
                return priorityCompare;
            }
            // Then by classification count (fewer classifications first)
            return Long.compare(
                    classificationCounts.getOrDefault(img1.getId(), 0L),
                    classificationCounts.getOrDefault(img2.getId(), 0L));
        });

        return Optional.of(unclassifiedRegular.get(0));
    }

    /**
     * Reset classification count for a user session (useful for testing or session
     * management)
     */
    public void resetUserSession(String username, Long taskId) {
        String sessionKey = username + "_" + taskId;
        userClassificationCount.remove(sessionKey);
    }
}
