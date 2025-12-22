package com.swipelab.util;

import com.swipelab.model.entity.Classification;
import com.swipelab.model.entity.Label;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
@Slf4j
public class CredibilityCalculator {

    /**
     * Calculates Cohen's Kappa coefficient between two sets of classifications.
     * Kappa measures inter-rater agreement for categorical items.
     *
     * @param user1Classifications Classifications from first user/expert
     * @param user2Classifications Classifications from second user
     * @return Kappa coefficient (-1 to 1, where 1 is perfect agreement)
     */
    public double calculateCohenKappa(
            List<Classification> user1Classifications,
            List<Classification> user2Classifications) {

        if (user1Classifications.isEmpty() || user2Classifications.isEmpty()) {
            log.debug("Cannot calculate Cohen's Kappa: one or both classification lists are empty");
            return 0.0;
        }

        // Map classifications by imageId
        Map<Long, Label> user1Labels = user1Classifications.stream()
                .collect(Collectors.toMap(
                        c -> c.getImage().getId(),
                        Classification::getLabel,
                        (existing, replacement) -> existing // keep first if duplicates
                ));

        Map<Long, Label> user2Labels = user2Classifications.stream()
                .collect(Collectors.toMap(
                        c -> c.getImage().getId(),
                        Classification::getLabel,
                        (existing, replacement) -> existing
                ));

        // Find common images (both users classified)
        Set<Long> commonImages = new HashSet<>(user1Labels.keySet());
        commonImages.retainAll(user2Labels.keySet());

        if (commonImages.isEmpty()) {
            log.debug("No common images classified by both users");
            return 0.0;
        }

        // Build confusion matrix
        Map<Long, Integer> labelCounts1 = new HashMap<>();
        Map<Long, Integer> labelCounts2 = new HashMap<>();
        int agreementCount = 0;
        int totalComparisons = commonImages.size();

        for (Long imageId : commonImages) {
            Label label1 = user1Labels.get(imageId);
            Label label2 = user2Labels.get(imageId);

            // Count agreements
            if (label1.getId().equals(label2.getId())) {
                agreementCount++;
            }

            // Count label frequencies for expected agreement
            labelCounts1.merge(label1.getId(), 1, Integer::sum);
            labelCounts2.merge(label2.getId(), 1, Integer::sum);
        }

        // Calculate observed agreement (Po)
        double observedAgreement = (double) agreementCount / totalComparisons;

        // Calculate expected agreement by chance (Pe)
        double expectedAgreement = 0.0;
        Set<Long> allLabels = new HashSet<>();
        allLabels.addAll(labelCounts1.keySet());
        allLabels.addAll(labelCounts2.keySet());

        for (Long labelId : allLabels) {
            double prob1 = labelCounts1.getOrDefault(labelId, 0) / (double) totalComparisons;
            double prob2 = labelCounts2.getOrDefault(labelId, 0) / (double) totalComparisons;
            expectedAgreement += (prob1 * prob2);
        }

        // Calculate Cohen's Kappa: κ = (Po - Pe) / (1 - Pe)
        if (expectedAgreement >= 1.0) {
            // Perfect expected agreement (shouldn't happen in practice)
            return 1.0;
        }

        double kappa = (observedAgreement - expectedAgreement) / (1.0 - expectedAgreement);

        log.debug("Cohen's Kappa calculated: {} (observed: {}, expected: {}, common images: {})",
                kappa, observedAgreement, expectedAgreement, totalComparisons);

        return kappa;
    }

    /**
     * Calculates the majority vote label for a set of classifications.
     * Majority is defined as >50% agreement.
     *
     * @param classifications All classifications for a specific image
     * @return The majority label, or null if no clear majority exists
     */
    public Label calculateMajorityVote(List<Classification> classifications) {
        if (classifications.isEmpty()) {
            return null;
        }

        if (classifications.size() == 1) {
            // Single classification - no majority yet
            return null;
        }

        // Count votes for each label
        Map<Long, Long> labelVotes = classifications.stream()
                .collect(Collectors.groupingBy(
                        c -> c.getLabel().getId(),
                        Collectors.counting()
                ));

        // Find the label with most votes
        long totalVotes = classifications.size();
        Map.Entry<Long, Long> maxEntry = labelVotes.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .orElse(null);

        if (maxEntry == null) {
            return null;
        }

        // Check if it's a true majority (>50%)
        double majorityPercentage = (double) maxEntry.getValue() / totalVotes;

        if (majorityPercentage > 0.5) {
            // Return the actual Label object
            Label majorityLabel = classifications.stream()
                    .filter(c -> c.getLabel().getId().equals(maxEntry.getKey()))
                    .map(Classification::getLabel)
                    .findFirst()
                    .orElse(null);

            log.debug("Majority vote found: label {} with {}/{} votes ({}%)",
                    maxEntry.getKey(), maxEntry.getValue(), totalVotes, majorityPercentage * 100);

            return majorityLabel;
        }

        log.debug("No majority consensus: highest vote was {}% (need >50%)", majorityPercentage * 100);
        return null; // No clear majority
    }

    /**
     * Calculates how well a user's classification agrees with the majority vote.
     *
     * @param userClassification The user's classification
     * @param majorityLabel The majority vote label (if exists)
     * @return 1.0 if matches majority, 0.0 if doesn't match or no majority
     */
    public double calculateMajorityAgreementScore(
            Classification userClassification,
            Label majorityLabel) {

        if (majorityLabel == null) {
            // No majority exists yet
            return 0.0;
        }

        boolean matches = userClassification.getLabel().getId().equals(majorityLabel.getId());
        return matches ? 1.0 : 0.0;
    }

    /**
     * Calculates the consensus strength (how strong is the majority).
     *
     * @param classifications All classifications for an image
     * @return Value between 0 and 1 representing consensus strength
     */
    public double calculateConsensusStrength(List<Classification> classifications) {
        if (classifications.size() < 2) {
            return 0.0;
        }

        Map<Long, Long> labelVotes = classifications.stream()
                .collect(Collectors.groupingBy(
                        c -> c.getLabel().getId(),
                        Collectors.counting()
                ));

        long maxVotes = labelVotes.values().stream()
                .max(Long::compareTo)
                .orElse(0L);

        return (double) maxVotes / classifications.size();
    }
}