package com.swipelab.classification.domain.threshold;
import com.swipelab.classification.domain.core.Classification;

import com.swipelab.classification.infrastructure.ClassificationRepository;
import com.swipelab.users.application.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * An implementation of {@link ThresholdPolicy} that weights each classification based on the user's credibility score.
 * <p>
 * The credibility score (0-100) is normalized into a weight (0.0 - 1.0).
 * Users with the "RESEARCHER" role are treated as experts and are automatically assigned a weight of 1.0.
 * If the sum of weights for any specific decision reaches or exceeds the target threshold,
 * that decision is considered to have reached consensus.
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CredibilityWeightedThresholdPolicy implements ThresholdPolicy {

    private final ClassificationRepository classificationRepository;
    private final UserService userService;

    private static final String ROLE_RESEARCHER = "RESEARCHER";
    private static final double MAX_CREDIBILITY_SCORE = 100.0;
    private static final double DEFAULT_CREDIBILITY_SCORE = 50.0;

    @Override
    public Optional<ThresholdResult> evaluateThreshold(Long imageId, Long taskId, String querySpecies, Double targetThreshold) {
        if (targetThreshold == null || targetThreshold < 3.0 || targetThreshold > 20.0) {
            log.warn("Invalid target threshold: {}. Must be between 3.0 and 20.0", targetThreshold);
            return Optional.empty();
        }

        // Fetch all classifications for this image and species
        List<Classification> classifications = classificationRepository.findByImageIdAndQuerySpecies(imageId, querySpecies);

        if (classifications.isEmpty()) {
            return Optional.empty();
        }

        // Group classifications by their response and sum their credibility weights
        Map<String, Double> weightedScores = classifications.stream()
                .collect(Collectors.groupingBy(
                        c -> c.getUserResponse().name(),
                        Collectors.summingDouble(this::calculateWeight)
                ));

        // Find the decision with the highest score that meets or exceeds the threshold
        return weightedScores.entrySet().stream()
                .filter(entry -> entry.getValue() >= targetThreshold)
                .max(Map.Entry.comparingByValue())
                .map(entry -> new ThresholdResult(entry.getKey(), entry.getValue()));
    }

    /**
     * Calculates the weight of a single classification based on the user's role and credibility score.
     *
     * @param classification The classification to evaluate.
     * @return A weight between 0.0 and 1.0.
     */
    private double calculateWeight(Classification classification) {
        if (ROLE_RESEARCHER.equalsIgnoreCase(classification.getUserRole())) {
            return 1.0;
        }

        Double credibilityScore = userService.getUserCredibility(classification.getUsername());
        if (credibilityScore == null) {
            credibilityScore = DEFAULT_CREDIBILITY_SCORE;
        }

        // Normalize score (0 - 100) to weight (0.0 - 1.0)
        return Math.max(0.0, Math.min(1.0, credibilityScore / MAX_CREDIBILITY_SCORE));
    }
}
