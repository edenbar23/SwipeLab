// backend/src/main/java/com/swipelab/util/CredibilityCalculator.java
package com.swipelab.util;

import java.util.*;
import java.util.stream.Collectors;

public class CredibilityCalculator {

    /**
     * Calculate Cohen's Kappa between two raters
     *
     * @param rater1Classifications List of label IDs from rater 1
     * @param rater2Classifications List of label IDs from rater 2 (must be same order/images)
     * @return Cohen's Kappa score (-1 to 1)
     */
    public static double calculateCohenKappa(List<Long> rater1Classifications,
                                             List<Long> rater2Classifications) {
        if (rater1Classifications.size() != rater2Classifications.size() ||
                rater1Classifications.isEmpty()) {
            throw new IllegalArgumentException("Classification lists must be non-empty and equal in size");
        }

        int n = rater1Classifications.size();

        // Calculate observed agreement (Po)
        int agreements = 0;
        for (int i = 0; i < n; i++) {
            if (rater1Classifications.get(i).equals(rater2Classifications.get(i))) {
                agreements++;
            }
        }
        double po = (double) agreements / n;

        // Calculate expected agreement (Pe)
        Map<Long, Integer> rater1Counts = countOccurrences(rater1Classifications);
        Map<Long, Integer> rater2Counts = countOccurrences(rater2Classifications);

        // Get all unique labels
        Set<Long> allLabels = new HashSet<>();
        allLabels.addAll(rater1Counts.keySet());
        allLabels.addAll(rater2Counts.keySet());

        double pe = 0.0;
        for (Long label : allLabels) {
            double p1 = rater1Counts.getOrDefault(label, 0) / (double) n;
            double p2 = rater2Counts.getOrDefault(label, 0) / (double) n;
            pe += p1 * p2;
        }

        // Cohen's Kappa formula
        if (pe == 1.0) {
            return 1.0; // Perfect agreement
        }

        return (po - pe) / (1 - pe);
    }

    /**
     * Calculate Fleiss' Kappa for multiple raters
     *
     * @param classificationsMatrix Map of imageId -> Map of userId -> labelId
     * @return Fleiss' Kappa score (-1 to 1)
     */
    public static double calculateFleissKappa(Map<Long, Map<String, Long>> classificationsMatrix) {
        if (classificationsMatrix.isEmpty()) {
            throw new IllegalArgumentException("Classifications matrix cannot be empty");
        }

        // n = number of subjects (images)
        int n = classificationsMatrix.size();

        // N = number of ratings per subject (should be consistent)
        int N = classificationsMatrix.values().iterator().next().size();

        // Validate all images have same number of raters
        for (Map<String, Long> ratings : classificationsMatrix.values()) {
            if (ratings.size() != N) {
                throw new IllegalArgumentException("All images must have the same number of classifications");
            }
        }

        // Get all unique labels
        Set<Long> allLabels = classificationsMatrix.values().stream()
                .flatMap(ratings -> ratings.values().stream())
                .collect(Collectors.toSet());

        int k = allLabels.size(); // number of categories

        // Build frequency matrix: nij = number of raters who assigned category j to subject i
        Map<Long, Map<Long, Integer>> frequencyMatrix = new HashMap<>();
        for (Map.Entry<Long, Map<String, Long>> entry : classificationsMatrix.entrySet()) {
            Long imageId = entry.getKey();
            Map<Long, Integer> labelCounts = new HashMap<>();

            for (Long labelId : entry.getValue().values()) {
                labelCounts.put(labelId, labelCounts.getOrDefault(labelId, 0) + 1);
            }
            frequencyMatrix.put(imageId, labelCounts);
        }

        // Calculate P̄ (mean of Pi values)
        double pBarSum = 0.0;
        for (Long imageId : frequencyMatrix.keySet()) {
            Map<Long, Integer> labelCounts = frequencyMatrix.get(imageId);
            int sumSquared = 0;

            for (Long label : allLabels) {
                int nij = labelCounts.getOrDefault(label, 0);
                sumSquared += nij * nij;
            }

            double pi = (sumSquared - N) / (double) (N * (N - 1));
            pBarSum += pi;
        }
        double pBar = pBarSum / n;

        // Calculate P̄e (expected agreement)
        double peSum = 0.0;
        for (Long label : allLabels) {
            int totalForLabel = 0;
            for (Map<Long, Integer> labelCounts : frequencyMatrix.values()) {
                totalForLabel += labelCounts.getOrDefault(label, 0);
            }
            double pj = totalForLabel / (double) (n * N);
            peSum += pj * pj;
        }

        // Fleiss' Kappa formula
        if (peSum == 1.0) {
            return 1.0;
        }

        return (pBar - peSum) / (1 - peSum);
    }

    /**
     * Calculate Fleiss' Kappa contribution for a single user across multiple images
     */
    public static double calculateUserFleissContribution(
            Map<Long, Map<String, Long>> classificationsMatrix,
            String targetUsername) {

        // Filter to only images where target user participated
        Map<Long, Map<String, Long>> userImages = classificationsMatrix.entrySet().stream()
                .filter(entry -> entry.getValue().containsKey(targetUsername))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        if (userImages.isEmpty()) {
            return 0.0;
        }

        return calculateFleissKappa(userImages);
    }

    // Helper method to count occurrences
    private static Map<Long, Integer> countOccurrences(List<Long> list) {
        Map<Long, Integer> counts = new HashMap<>();
        for (Long item : list) {
            counts.put(item, counts.getOrDefault(item, 0) + 1);
        }
        return counts;
    }

    /**
     * Interpret Kappa score as a readable quality level
     */
    public static String interpretKappa(double kappa) {
        if (kappa < 0) return "Poor (worse than chance)";
        if (kappa < 0.20) return "Slight agreement";
        if (kappa < 0.40) return "Fair agreement";
        if (kappa < 0.60) return "Moderate agreement";
        if (kappa < 0.80) return "Substantial agreement";
        return "Almost perfect agreement";
    }
}