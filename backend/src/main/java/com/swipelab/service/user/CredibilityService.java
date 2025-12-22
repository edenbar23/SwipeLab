package com.swipelab.service.user;

import com.swipelab.model.entity.Classification;
import com.swipelab.model.entity.Label;
import com.swipelab.model.entity.User;
import com.swipelab.model.enums.UserRole;
import com.swipelab.repository.ClassificationRepository;
import com.swipelab.repository.UserRepository;
import com.swipelab.util.CredibilityCalculator;
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

    /**
     * Updates a user's credibility score after they submit a classification.
     * This method:
     * 1. Compares with expert classifications (Cohen's Kappa)
     * 2. Compares with majority consensus
     *
     * @param username The user who just classified an image
     * @param imageId The image that was classified
     */
    @Transactional
    public void updateUserCredibility(String username, Long imageId) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        // Skip if user is an expert (we only calculate credibility for regular users)
        if (user.getRole() == UserRole.RESEARCHER) {
            log.debug("Skipping credibility update for expert user: {}", username);
            return;
        }

        log.info("Updating credibility for user: {} after classifying image: {}", username, imageId);

        // Update expert agreement score (Cohen's Kappa)
        updateExpertAgreementScore(user);

        // Update majority consensus agreement
        updateMajorityAgreementScore(user, imageId);

        // Save updated user
        userRepository.save(user);

        log.info("Credibility update complete for user: {} - Expert Agreement: {}, Majority Agreement: {}",
                username, user.getAgreementWithExperts(), user.getMajorityAgreementScore());
    }

    /**
     * Recalculates credibility for all users who classified a specific image.
     * Called when an expert classifies an image - need to update all regular users' scores.
     *
     * @param imageId The image that was classified by an expert
     */
    @Transactional
    public void recalculateCredibilityForImage(Long imageId) {
        log.info("Recalculating credibility for all users who classified image: {}", imageId);

        // Get all non-expert classifications for this image
        List<Classification> classifications = classificationRepository.findNonExpertClassificationsByImageId(imageId);

        // Get unique users
        List<User> users = classifications.stream()
                .map(Classification::getUser)
                .distinct()
                .collect(Collectors.toList());

        // Update each user's credibility
        for (User user : users) {
            updateExpertAgreementScore(user);
            updateMajorityAgreementScore(user, imageId);
            userRepository.save(user);

            log.debug("Updated credibility for user: {} - Expert Agreement: {}, Majority Agreement: {}",
                    user.getUsername(), user.getAgreementWithExperts(), user.getMajorityAgreementScore());
        }

        log.info("Recalculation complete for {} users", users.size());
    }

    /**
     * Calculates and updates the user's agreement with expert classifications using Cohen's Kappa.
     */
    private void updateExpertAgreementScore(User user) {
        // Get all user's classifications
        List<Classification> userClassifications = classificationRepository.findByUser_Username(user.getUsername());

        if (userClassifications.isEmpty()) {
            log.debug("No classifications found for user: {}", user.getUsername());
            return;
        }

        // Get all expert classifications
        List<Classification> expertClassifications = classificationRepository.findExpertClassifications();

        if (expertClassifications.isEmpty()) {
            log.debug("No expert classifications found to compare against");
            return;
        }

        // Calculate Cohen's Kappa between user and all experts
        double kappa = credibilityCalculator.calculateCohenKappa(userClassifications, expertClassifications);

        // Update user's expert agreement score
        user.setAgreementWithExperts(kappa);

        log.debug("Updated expert agreement for user {}: Cohen's Kappa = {}", user.getUsername(), kappa);
    }

    /**
     * Calculates and updates the user's agreement with majority vote.
     * This checks all images the user has classified and sees how often they agree with majority.
     */
    private void updateMajorityAgreementScore(User user, Long recentImageId) {
        // Get all user's classifications
        List<Classification> userClassifications = classificationRepository.findByUser_Username(user.getUsername());

        if (userClassifications.isEmpty()) {
            return;
        }

        int totalComparisons = 0;
        int agreementCount = 0;

        // For each image the user classified, check if they agreed with majority
        for (Classification userClassification : userClassifications) {
            Long imageId = userClassification.getImage().getId();

            // Get all classifications for this image
            List<Classification> allClassifications = classificationRepository.findByImageId(imageId);

            // Need at least 2 classifications to have a majority
            if (allClassifications.size() < 2) {
                continue;
            }

            // Calculate majority vote
            Label majorityLabel = credibilityCalculator.calculateMajorityVote(allClassifications);

            if (majorityLabel == null) {
                // No clear majority yet
                continue;
            }

            // Check if user agreed with majority
            double agreementScore = credibilityCalculator.calculateMajorityAgreementScore(
                    userClassification, majorityLabel);

            totalComparisons++;
            if (agreementScore == 1.0) {
                agreementCount++;
            }
        }

        // Calculate percentage of agreement with majority
        double majorityAgreementScore = totalComparisons > 0
                ? (double) agreementCount / totalComparisons
                : 0.0;

        user.setMajorityAgreementScore(majorityAgreementScore);

        log.debug("Updated majority agreement for user {}: {}/{} agreements ({}%)",
                user.getUsername(), agreementCount, totalComparisons, majorityAgreementScore * 100);
    }

    /**
     * Gets detailed credibility statistics for a user.
     * Can be used for debugging or displaying to admins.
     */
    @Transactional(readOnly = true)
    public CredibilityStats getCredibilityStats(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        List<Classification> userClassifications = classificationRepository.findByUser_Username(username);
        List<Classification> expertClassifications = classificationRepository.findExpertClassifications();

        // Count common images with experts
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

    /**
     * Inner class to hold credibility statistics
     */
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