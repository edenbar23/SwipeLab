package com.swipelab.classification.domain;

/**
 * Policy interface that decides whether the next image served to a user
 * should be a gold-standard (quality control) image.
 *
 * Separates the distribution policy from both the data access layer and
 * the TaskDistributionService orchestration logic.
 */
public interface GoldImagePolicy {

    boolean shouldIncludeGoldImageInBatch(String username, Long taskId, int batchSize);
}
