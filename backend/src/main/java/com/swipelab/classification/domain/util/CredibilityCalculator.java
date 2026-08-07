package com.swipelab.classification.domain.util;

import com.swipelab.classification.domain.core.Classification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
@Slf4j
public class CredibilityCalculator {

    /**
     * Calculates the composite credibility score normalized to 0-100.
     *
     * Weights:
     *   - Gold accuracy  : 40%  (ground truth — strongest signal)
     *   - Majority agr.  : 35%  (community consensus)
     *   - Expert kappa   : 25%  (researcher agreement)
     *
     * Missing signals (null) redistribute their weight proportionally to the
     * remaining available signals so new users aren't unfairly penalised.
     *
     * Penalties (fraud detections, warnings) are subtracted after combining.
     *
     * @param goldAccuracy       fraction of correct gold answers  [0, 1], or null if no gold data
     * @param majorityAgreement  fraction agreeing with majority   [0, 1], or null if no consensus data
     * @param expertKappa        Cohen's Kappa vs experts          [-1, 1], or null if no expert overlap
     * @param penaltyPoints      raw penalty points already accumulated (deducted from final score)
     * @return composite score in [0, 100]
     */
    public double calculateCompositeScore(
            Double goldAccuracy,
            Double majorityAgreement,
            Double expertKappa,
            double penaltyPoints) {

        // Normalise kappa from [-1,1] → [0,1] before weighting
        Double expertNormalized = (expertKappa != null) ? normalizeKappa(expertKappa) : null;

        // Base weights — must sum to 1.0
        double wGold     = 0.40;
        double wMajority = 0.35;
        double wExpert   = 0.25;

        // Collect available signals and their weights
        double totalWeight = 0.0;
        double weightedSum = 0.0;

        if (goldAccuracy != null) {
            weightedSum += wGold * goldAccuracy;
            totalWeight += wGold;
        }
        if (majorityAgreement != null) {
            weightedSum += wMajority * majorityAgreement;
            totalWeight += wMajority;
        }
        if (expertNormalized != null) {
            weightedSum += wExpert * expertNormalized;
            totalWeight += wExpert;
        }

        if (totalWeight == 0.0) {
            // No signal at all — return neutral default (handled by caller)
            return 50.0;
        }

        // Proportionally rescale so missing weights don't deflate the score
        double normalized = weightedSum / totalWeight;  // still in [0,1]
        double score = normalized * 100.0 - penaltyPoints;

        return Math.max(0.0, Math.min(100.0, score));
    }

    /**
     * Maps Cohen's Kappa from [-1, 1] to [0, 1].
     * kappa = -1 → 0.0, kappa = 0 → 0.5, kappa = 1 → 1.0
     */
    public double normalizeKappa(double kappa) {
        return (kappa + 1.0) / 2.0;
    }

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

        // Map classifications by (imageId, querySpecies) to honour per-query granularity
        Map<String, Classification.UserResponse> user1Labels = buildLabelMap(user1Classifications);
        Map<String, Classification.UserResponse> user2Labels = buildLabelMap(user2Classifications);

        // Find common image-query pairs
        Set<String> commonPairs = new HashSet<>(user1Labels.keySet());
        commonPairs.retainAll(user2Labels.keySet());

        if (commonPairs.isEmpty()) {
            log.debug("No common image-query pairs classified by both users");
            return 0.0;
        }

        Map<Classification.UserResponse, Integer> labelCounts1 = new HashMap<>();
        Map<Classification.UserResponse, Integer> labelCounts2 = new HashMap<>();
        int agreementCount = 0;
        int totalComparisons = commonPairs.size();

        for (String pair : commonPairs) {
            Classification.UserResponse label1 = user1Labels.get(pair);
            Classification.UserResponse label2 = user2Labels.get(pair);

            if (label1 == label2) {
                agreementCount++;
            }
            labelCounts1.merge(label1, 1, Integer::sum);
            labelCounts2.merge(label2, 1, Integer::sum);
        }

        double observedAgreement = (double) agreementCount / totalComparisons;

        // Expected agreement by chance (Pe)
        double expectedAgreement = 0.0;
        Set<Classification.UserResponse> allLabels = new HashSet<>();
        allLabels.addAll(labelCounts1.keySet());
        allLabels.addAll(labelCounts2.keySet());

        for (Classification.UserResponse label : allLabels) {
            double prob1 = labelCounts1.getOrDefault(label, 0) / (double) totalComparisons;
            double prob2 = labelCounts2.getOrDefault(label, 0) / (double) totalComparisons;
            expectedAgreement += prob1 * prob2;
        }

        if (expectedAgreement >= 1.0) {
            return 1.0;
        }

        double kappa = (observedAgreement - expectedAgreement) / (1.0 - expectedAgreement);

        log.debug("Cohen's Kappa: {} (observed={}, expected={}, commonPairs={})",
                kappa, observedAgreement, expectedAgreement, totalComparisons);
        return kappa;
    }

    /**
     * Calculates the majority vote label for a set of classifications on the same
     * (imageId, querySpecies) pair.
     * Majority is defined as >50% agreement.
     *
     * @param classifications All classifications for a specific image-query pair
     * @return The majority response, or null if no clear majority exists
     */
    public Classification.UserResponse calculateMajorityVote(List<Classification> classifications) {
        if (classifications == null || classifications.isEmpty()) {
            return null;
        }
        if (classifications.size() == 1) {
            return null; // Single classification — no majority yet
        }

        Map<Classification.UserResponse, Long> labelVotes = classifications.stream()
                .collect(Collectors.groupingBy(Classification::getUserResponse, Collectors.counting()));

        long totalVotes = classifications.size();
        Map.Entry<Classification.UserResponse, Long> maxEntry = labelVotes.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .orElse(null);

        if (maxEntry == null) {
            return null;
        }

        double majorityPercentage = (double) maxEntry.getValue() / totalVotes;
        if (majorityPercentage > 0.5) {
            log.debug("Majority vote: {} with {}/{} votes ({}%)",
                    maxEntry.getKey(), maxEntry.getValue(), totalVotes, majorityPercentage * 100);
            return maxEntry.getKey();
        }

        log.debug("No majority consensus: highest was {}% (need >50%)", majorityPercentage * 100);
        return null;
    }

    /**
     * Returns 1.0 if the user's classification matches the majority label, 0.0 otherwise.
     * Returns 0.0 when majorityLabel is null (no consensus yet).
     */
    public double calculateMajorityAgreementScore(
            Classification userClassification,
            Classification.UserResponse majorityLabel) {

        if (majorityLabel == null) {
            return 0.0;
        }
        return userClassification.getUserResponse() == majorityLabel ? 1.0 : 0.0;
    }

    /**
     * Calculates the consensus strength (how strong the majority is).
     *
     * @param classifications All classifications for an image-query pair
     * @return Value between 0 and 1 representing consensus strength
     */
    public double calculateConsensusStrength(List<Classification> classifications) {
        if (classifications == null || classifications.size() < 2) {
            return 0.0;
        }

        Map<Classification.UserResponse, Long> labelVotes = classifications.stream()
                .collect(Collectors.groupingBy(Classification::getUserResponse, Collectors.counting()));

        long maxVotes = labelVotes.values().stream()
                .max(Long::compareTo)
                .orElse(0L);

        return (double) maxVotes / classifications.size();
    }

    /**
     * Weighted Kappa using only the last 50 classifications (sliding-window effect).
     */
    public double calculateWeightedCohenKappa(List<Classification> userClassifications,
            List<Classification> expertClassifications) {
        List<Classification> recentClassifications = userClassifications.stream()
                .sorted(Comparator.comparing(Classification::getCreatedAt).reversed())
                .limit(50)
                .toList();
        return calculateCohenKappa(recentClassifications, expertClassifications);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Builds a map keyed by "imageId:querySpecies" so credibility is computed
     * per image-query pair (not just per image).
     */
    private Map<String, Classification.UserResponse> buildLabelMap(List<Classification> classifications) {
        return classifications.stream()
                .collect(Collectors.toMap(
                        c -> c.getImage().getId() + ":" + nullToEmpty(c.getQuerySpecies()),
                        Classification::getUserResponse,
                        (existing, replacement) -> existing // keep first on duplicate
                ));
    }

    private String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}