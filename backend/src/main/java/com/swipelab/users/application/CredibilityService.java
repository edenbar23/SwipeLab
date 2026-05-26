package com.swipelab.users.application;

import com.swipelab.classification.domain.Classification;
import com.swipelab.classification.domain.util.CredibilityCalculator;
import com.swipelab.users.domain.User;
import com.swipelab.model.enums.UserRole;
import com.swipelab.model.enums.UserStatus;
import com.swipelab.classification.infrastructure.ClassificationRepository;
import com.swipelab.users.infrastructure.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CredibilityService {

    private final ClassificationRepository classificationRepository;
    private final UserRepository userRepository;
    private final CredibilityCalculator credibilityCalculator;

    @Transactional
    public void updateUserCredibility(String username, Long imageId) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        if (user.getRole() == UserRole.RESEARCHER) {
            log.debug("Skipping credibility update for expert user: {}", username);
            return;
        }

        log.info("Updating credibility for user: {} after classifying image: {}", username, imageId);

        updateExpertAgreementScore(user);
        updateMajorityAgreementScore(user, imageId);

        userRepository.save(user);

        log.info("Credibility update complete for user: {} - Expert Agreement: {}, Majority Agreement: {}",
                username, user.getAgreementWithExperts(), user.getMajorityAgreementScore());
    }

    @Transactional
    public void recalculateCredibilityForImage(Long imageId) {
        log.info("Recalculating credibility for all users who classified image: {}", imageId);

        List<Classification> classifications = classificationRepository.findNonExpertClassificationsByImageId(imageId);

        List<User> users = classifications.stream()
                .map(Classification::getUsername)
                .distinct()
                .map(u -> userRepository.findByUsername(u).orElse(null))
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());

        for (User user : users) {
            updateExpertAgreementScore(user);
            updateMajorityAgreementScore(user, imageId);
            userRepository.save(user);

            log.debug("Updated credibility for user: {} - Expert Agreement: {}, Majority Agreement: {}",
                    user.getUsername(), user.getAgreementWithExperts(), user.getMajorityAgreementScore());
        }

        log.info("Recalculation complete for {} users", users.size());
    }

    private void updateExpertAgreementScore(User user) {
        List<Classification> userClassifications = classificationRepository.findByUsername(user.getUsername());

        if (userClassifications.isEmpty()) {
            log.debug("No classifications found for user: {}", user.getUsername());
            return;
        }

        List<Classification> expertClassifications = classificationRepository.findExpertClassifications();

        if (expertClassifications.isEmpty()) {
            log.debug("No expert classifications found to compare against");
            return;
        }

        double kappa = credibilityCalculator.calculateCohenKappa(userClassifications, expertClassifications);

        user.setAgreementWithExperts(kappa);

        log.debug("Updated expert agreement for user {}: Cohen's Kappa = {}", user.getUsername(), kappa);
    }

    private void updateMajorityAgreementScore(User user, Long recentImageId) {
        List<Classification> userClassifications = classificationRepository.findByUsername(user.getUsername());

        if (userClassifications.isEmpty()) {
            return;
        }

        // Warned users count for 0.5 weight in majority voting — their credibility
        // is penalised but not fully removed so they can still contribute data.
        double userWeight = (user.getStatus() == UserStatus.WARNED) ? 0.5 : 1.0;

        int totalComparisons = 0;
        double weightedAgreement = 0.0;

        for (Classification userClassification : userClassifications) {
            Long imageId = userClassification.getImage().getId();

            List<Classification> allClassifications = classificationRepository.findByImageId(imageId);

            if (allClassifications.size() < 2) {
                continue;
            }

            Classification.UserResponse majorityLabel = credibilityCalculator.calculateMajorityVote(allClassifications);

            if (majorityLabel == null) {
                continue;
            }

            double agreementScore = credibilityCalculator.calculateMajorityAgreementScore(
                    userClassification, majorityLabel);

            totalComparisons++;
            if (agreementScore == 1.0) {
                weightedAgreement += userWeight; // WARNED = 0.5, ACTIVE = 1.0
            }
        }

        double majorityAgreementScore = totalComparisons > 0
                ? weightedAgreement / totalComparisons
                : 0.0;

        user.setMajorityAgreementScore(majorityAgreementScore);

        log.debug("Updated majority agreement for user {} (weight={}): {}/{} effective agreements ({}%)",
                user.getUsername(), userWeight, weightedAgreement, totalComparisons,
                majorityAgreementScore * 100);
    }

    @Transactional(readOnly = true)
    public CredibilityStats getCredibilityStats(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        List<Classification> userClassifications = classificationRepository.findByUsername(username);
        List<Classification> expertClassifications = classificationRepository.findExpertClassifications();

        long commonWithExperts = userClassifications.stream()
                .map(c -> c.getImage().getId())
                .filter(imageId -> expertClassifications.stream()
                        .anyMatch(ec -> ec.getImage().getId().equals(imageId)))
                .count();

        return CredibilityStats.builder()
                .username(username)
                .totalClassifications(userClassifications.size())
                .expertAgreementScore(user.getAgreementWithExperts())
                .majorityAgreementScore(user.getMajorityAgreementScore())
                .imagesInCommonWithExperts((int) commonWithExperts)
                .build();
    }

    @lombok.Builder
    @lombok.Data
    public static class CredibilityStats {
        private String username;
        private int totalClassifications;
        private double expertAgreementScore;
        private double majorityAgreementScore;
        private int imagesInCommonWithExperts;
    }
}