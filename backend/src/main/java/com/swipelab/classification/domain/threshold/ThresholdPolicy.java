package com.swipelab.classification.domain.threshold;

import java.util.Optional;

/**
 * Defines the policy for determining if a set of classifications has reached a required threshold.
 * This abstraction allows the system to support various consensus mechanisms, such as:
 * <ul>
 *     <li>Simple majority voting (counting raw classifications).</li>
 *     <li>Credibility-weighted voting (weighting votes based on user expertise).</li>
 *     <li>Machine-learning assisted thresholding.</li>
 * </ul>
 */
public interface ThresholdPolicy {

    /**
     * Evaluates whether the classifications for a specific image and species query have reached
     * the required threshold.
     *
     * @param imageId         The ID of the image being classified.
     * @param taskId          The ID of the task the image belongs to.
     * @param querySpecies    The species being queried (e.g., "Corn").
     * @param targetThreshold The target threshold score required to reach consensus (e.g., 3.0).
     * @return An Optional containing the {@link ThresholdResult} if the threshold is met,
     *         or an empty Optional if the threshold is not yet reached.
     */
    Optional<ThresholdResult> evaluateThreshold(Long imageId, Long taskId, String querySpecies, Double targetThreshold);

    /**
     * Represents the outcome of a successful threshold evaluation.
     *
     * @param winningDecision The classification response that won (e.g., YES, NO, DONT_KNOW).
     * @param finalScore      The final cumulative score that surpassed the threshold.
     */
    record ThresholdResult(String winningDecision, Double finalScore) {}
}
