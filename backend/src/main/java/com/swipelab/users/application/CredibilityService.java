package com.swipelab.users.application;

import com.swipelab.classification.domain.core.Classification;
import com.swipelab.classification.domain.util.CredibilityCalculator;
import com.swipelab.classification.infrastructure.ClassificationRepository;
import com.swipelab.classification.infrastructure.CredibilityRepository;
import com.swipelab.config.application.MaliciousLabelingConfigService;
import com.swipelab.model.enums.UserRole;
import com.swipelab.model.enums.UserStatus;
import com.swipelab.users.domain.User;
import com.swipelab.users.infrastructure.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Computes and persists the composite credibility score (0-100) for regular users.
 *
 * Score composition:
 *   40% — gold-image accuracy   (hard ground truth)
 *   35% — majority agreement    (community consensus, per image-query pair)
 *   25% — expert agreement      (Cohen's Kappa vs. RESEARCHER classifications)
 *
 * Missing signals redistribute their weight to the available signals, so new users
 * are not unfairly penalised when they have no gold or expert overlap yet.
 *
 * Malicious-labeling detection: if a user's score falls below a configurable threshold
 * after accumulating enough classifications, they are flagged for admin review.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CredibilityService {

    private final ClassificationRepository classificationRepository;
    private final CredibilityRepository credibilityRepository;
    private final UserRepository userRepository;
    private final CredibilityCalculator credibilityCalculator;
    private final AdminNotificationService adminNotificationService;
    // Provides runtime-adjustable maliciousThreshold and maliciousMinSamples.
    // Parameters are DB-backed and cached; superadmins can change them live.
    private final MaliciousLabelingConfigService maliciousLabelingConfigService;

    // ── Configuration (bound from application.yml) ────────────────────────────

    /** Minimum classifications on an (imageId, querySpecies) pair before it affects credibility */
    @Value("${app.credibility.min-classifications-for-consensus:3}")
    private int minClassificationsForConsensus;

    /** Starting score for new users (neutral position) */
    @Value("${app.credibility.default-score:50.0}")
    private double defaultScore;

    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Recalculates the composite credibility score for a single user.
     * Called after the consensus threshold has been crossed for a given image-query pair,
     * or when an expert classifies a pair the user also classified.
     *
     * No-op for RESEARCHER users (experts set the ground truth, don't receive scores).
     */
    @Transactional
    public void updateUserCredibility(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        if (user.getRole() == UserRole.RESEARCHER) {
            log.debug("Skipping credibility update for expert user: {}", username);
            return;
        }

        log.debug("Recalculating credibility for user: {}", username);

        Double goldAccuracy     = computeGoldAccuracy(username);
        Double majorityAgr      = computeMajorityAgreement(user);
        Double expertKappa      = computeExpertKappa(username);
        double penaltyPoints    = computePenaltyPoints(user);

        double newScore = credibilityCalculator.calculateCompositeScore(
                goldAccuracy, majorityAgr, expertKappa, penaltyPoints);

        // Only update if we have at least one real signal; otherwise keep existing score
        boolean hasSignal = goldAccuracy != null || majorityAgr != null || expertKappa != null;
        if (!hasSignal) {
            log.debug("No signal data yet for user {} — score stays at default", username);
            return;
        }

        // Keep sub-signal fields up to date for dashboard display
        if (expertKappa != null) {
            user.setAgreementWithExperts(expertKappa);
        }
        if (majorityAgr != null) {
            user.setMajorityAgreementScore(majorityAgr);
        }

        user.setCredibilityScore(newScore);
        userRepository.save(user);

        log.info("Credibility updated for {}: score={} (gold={}, majority={}, kappa={}, penalty={})",
                username, newScore, goldAccuracy, majorityAgr, expertKappa, penaltyPoints);

        checkForMaliciousLabeling(user);
    }

    /**
     * Recalculates credibility for every non-expert user who classified the given
     * (imageId, querySpecies) pair.  Called when consensus is reached or when an expert
     * submits a new classification on the same pair.
     */
    @Transactional
    public void recalculateCredibilityForImageQuery(Long imageId, String querySpecies) {
        log.info("Recalculating credibility for all users on image={} query={}", imageId, querySpecies);

        List<String> usernames = classificationRepository
                .findDistinctUsernamesByImageIdAndQuerySpecies(imageId, querySpecies);

        for (String username : usernames) {
            updateUserCredibility(username);
        }

        log.info("Recalculation done for {} users on image={} query={}", usernames.size(), imageId, querySpecies);
    }

    // ── Private computation helpers ───────────────────────────────────────────

    /**
     * Gold-image accuracy: fraction of gold classifications where the user gave the correct answer.
     * Returns null when the user has no gold classifications (no signal).
     */
    private Double computeGoldAccuracy(String username) {
        var records = credibilityRepository.findByUsername(username);
        if (records.isEmpty()) {
            return null;
        }
        long correct = records.stream()
                .filter(r -> r.getUserResponse().name().equals(r.getCorrectAnswer().name()))
                .count();
        double accuracy = (double) correct / records.size();

        // Track on User entity for dashboard display
        userRepository.findByUsername(username).ifPresent(u -> {
            u.setCorrectGoldClassifications((int) correct);
            u.setTotalGoldClassifications(records.size());
            // save is handled by the caller's transaction
        });

        log.debug("Gold accuracy for {}: {}/{} = {}", username, correct, records.size(), accuracy);
        return accuracy;
    }

    /**
     * Majority agreement: fraction of image-query pairs (with ≥ minClassificationsForConsensus
     * classifications) where the user agreed with the majority vote.
     *
     * Pairs without enough classifications are excluded entirely — they do not contribute
     * to the denominator, so first-classifiers are never penalised.
     *
     * WARNED users count for 0.5 weight in the agreement numerator.
     */
    private Double computeMajorityAgreement(User user) {
        String username = user.getUsername();
        List<Classification> userClassifications = classificationRepository.findByUsername(username);

        if (userClassifications.isEmpty()) {
            return null;
        }

        double userWeight = (user.getStatus() == UserStatus.WARNED) ? 0.5 : 1.0;
        int totalComparisons = 0;
        double weightedAgreement = 0.0;

        for (Classification userClassification : userClassifications) {
            Long imageId = userClassification.getImage().getId();
            String querySpecies = userClassification.getQuerySpecies();

            // Only include pairs that have reached consensus threshold
            long pairCount = classificationRepository.countByImageIdAndQuerySpecies(imageId, querySpecies);
            if (pairCount < minClassificationsForConsensus) {
                continue; // No consensus yet — exclude from credibility computation
            }

            List<Classification> pairClassifications =
                    classificationRepository.findByImageIdAndQuerySpecies(imageId, querySpecies);

            Classification.UserResponse majorityLabel =
                    credibilityCalculator.calculateMajorityVote(pairClassifications);

            if (majorityLabel == null) {
                continue; // Tied vote — can't determine consensus
            }

            double agreementScore = credibilityCalculator.calculateMajorityAgreementScore(
                    userClassification, majorityLabel);

            totalComparisons++;
            if (agreementScore == 1.0) {
                weightedAgreement += userWeight;
            }
        }

        if (totalComparisons == 0) {
            return null; // No eligible pairs yet
        }

        double result = weightedAgreement / totalComparisons;
        log.debug("Majority agreement for {} (weight={}): {}/{} = {}",
                username, userWeight, weightedAgreement, totalComparisons, result);
        return result;
    }

    /**
     * Expert agreement: Cohen's Kappa between this user and all RESEARCHER classifications,
     * limited to the last 50 user classifications (sliding window).
     * Returns null when there is no overlapping image-query pair with any expert.
     */
    private Double computeExpertKappa(String username) {
        List<Classification> expertClassifications = classificationRepository.findExpertClassifications();
        if (expertClassifications.isEmpty()) {
            return null;
        }

        List<Classification> userClassifications = classificationRepository.findByUsername(username);
        if (userClassifications.isEmpty()) {
            return null;
        }

        double kappa = credibilityCalculator.calculateWeightedCohenKappa(
                userClassifications, expertClassifications);

        // If kappa is 0.0 and there are no common pairs, return null (no signal)
        boolean hasOverlap = userClassifications.stream()
                .anyMatch(uc -> expertClassifications.stream()
                        .anyMatch(ec -> ec.getImage().getId().equals(uc.getImage().getId())
                                && nullToEmpty(ec.getQuerySpecies()).equals(nullToEmpty(uc.getQuerySpecies()))));

        if (!hasOverlap) {
            return null;
        }

        log.debug("Expert kappa for {}: {}", username, kappa);
        return kappa;
    }

    /**
     * Accumulated penalty points from fraud warnings.
     * WARNING_1 = 5 pts, WARNING_2 = 15 pts (already applied cumulatively to credibilityScore
     * by UserEventListener, but we track them separately here to avoid double-subtracting).
     *
     * Strategy: re-derive penalty from warningCount so the composite formula is self-consistent.
     */
    private double computePenaltyPoints(User user) {
        // Each warning increments warningCount. The penalty is applied via UserEventListener
        // as direct subtractions from credibilityScore. When we recompute from scratch here
        // we pass 0 penalty and let the base formula stand; the UserEventListener handles
        // subtracting additional fraud penalties on top of the freshly computed score.
        // This avoids double-counting.
        return 0.0;
    }

    /**
     * Flags a user as a potentially malicious labeler if their score drops below the
     * configured threshold after accumulating enough classifications.
     */
    private void checkForMaliciousLabeling(User user) {
        var cfg = maliciousLabelingConfigService.getMaliciousLabelingConfig();
        int totalClassifications = classificationRepository.countByUsername(user.getUsername()).intValue();
        if (totalClassifications < cfg.getMaliciousMinSamples()) {
            return;
        }
        if (user.getCredibilityScore() < cfg.getMaliciousThreshold() && !Boolean.TRUE.equals(user.getIsFlagged())) {
            log.warn("Malicious labeling detected for user {}: score={} after {} classifications",
                    user.getUsername(), user.getCredibilityScore(), totalClassifications);
            user.setIsFlagged(true);
            userRepository.save(user);
            adminNotificationService.notifyMaliciousLabeler(user.getUsername(), user.getCredibilityScore());
        }
    }

    // ── Stats DTO ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public CredibilityStats getCredibilityStats(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        List<Classification> userClassifications = classificationRepository.findByUsername(username);
        List<Classification> expertClassifications = classificationRepository.findExpertClassifications();

        long commonWithExperts = userClassifications.stream()
                .map(c -> c.getImage().getId() + ":" + nullToEmpty(c.getQuerySpecies()))
                .filter(pair -> expertClassifications.stream()
                        .anyMatch(ec -> (ec.getImage().getId() + ":" + nullToEmpty(ec.getQuerySpecies())).equals(pair)))
                .count();

        return CredibilityStats.builder()
                .username(username)
                .totalClassifications(userClassifications.size())
                .credibilityScore(user.getCredibilityScore())
                .expertAgreementScore(user.getAgreementWithExperts())
                .majorityAgreementScore(user.getMajorityAgreementScore())
                .goldAccuracy(user.getTotalGoldClassifications() > 0
                        ? (double) user.getCorrectGoldClassifications() / user.getTotalGoldClassifications()
                        : null)
                .imagesInCommonWithExperts((int) commonWithExperts)
                .build();
    }

    // ── Inner types ───────────────────────────────────────────────────────────

    @lombok.Builder
    @lombok.Data
    public static class CredibilityStats {
        private String username;
        private int totalClassifications;
        private double credibilityScore;
        private double expertAgreementScore;
        private double majorityAgreementScore;
        private Double goldAccuracy;
        private int imagesInCommonWithExperts;
    }

    // ─────────────────────────────────────────────────────────────────────────

    private String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}