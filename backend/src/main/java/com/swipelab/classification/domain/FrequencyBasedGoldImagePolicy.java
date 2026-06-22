package com.swipelab.classification.domain;

import com.swipelab.classification.infrastructure.ClassificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Implements GoldImagePolicy using a frequency-based rule:
 * serve a gold-standard image once every {@value GOLD_IMAGE_FREQUENCY} classifications.
 *
 * This class owns the data access needed for the policy decision,
 * keeping that concern out of TaskDistributionService.
 */
@Component
@RequiredArgsConstructor
public class FrequencyBasedGoldImagePolicy implements GoldImagePolicy {

    // 1 gold image per N regular classifications
    private static final int GOLD_IMAGE_FREQUENCY = 15;

    private final ClassificationRepository classificationRepository;

    @Override
    public boolean shouldIncludeGoldImageInBatch(String username, Long taskId, int batchSize) {
        Long count = classificationRepository.countByUsernameAndTaskId(username, taskId);
        long safeCount = count == null ? 0 : count;
        long currentInterval = safeCount / GOLD_IMAGE_FREQUENCY;
        long nextInterval = (safeCount + batchSize) / GOLD_IMAGE_FREQUENCY;
        return nextInterval > currentInterval;
    }
}
