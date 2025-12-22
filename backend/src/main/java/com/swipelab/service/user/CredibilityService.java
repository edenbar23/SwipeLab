// backend/src/main/java/com/swipelab/service/user/CredibilityService.java
package com.swipelab.service.user;

import com.swipelab.model.entity.Classification;
import com.swipelab.model.entity.User;
import com.swipelab.model.enums.UserRole;
import com.swipelab.repository.ClassificationRepository;
import com.swipelab.repository.UserRepository;
import com.swipelab.util.CredibilityCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CredibilityService {

    private final ClassificationRepository classificationRepository;
    private final UserRepository userRepository;

    /**
     * Recalculate Cohen's Kappa for a user against all experts
     */
    @Transactional
    public void updateCohenKappaForUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Don't calculate for experts
        if (user.getRole() == UserRole.RESEARCHER || user.getRole() == UserRole.ADMIN) {
            log.info("Skipping Cohen's Kappa for expert user: {}", username);
            return;
        }

        List<User> experts = userRepository.findAll().stream()
                .filter(u -> u.getRole() == UserRole.RESEARCHER || u.getRole() == UserRole.ADMIN)
                .collect(Collectors.toList());

        if (experts.isEmpty()) {
            log.warn("No experts found in system");
            return;
        }

        List<Double> kappaScores = new ArrayList<>();

        for (User expert : experts) {
            double kappa = calculateCohenKappaBetweenUsers(username, expert.getUsername());
            if (!Double.isNaN(kappa)) {
                kappaScores.add(kappa);
                log.info("Cohen's Kappa between {} and expert {}: {}",
                        username, expert.getUsername(), kappa);
            }
        }

        if (!kappaScores.isEmpty()) {
            double avgKappa = kappaScores.stream()
                    .mapToDouble(Double::doubleValue)
                    .average()
                    .orElse(0.0);

            user.setCohenKappaAvg(avgKappa);
            user.setCredibilityLastUpdated(LocalDateTime.now());
            userRepository.save(user);

            log.info("Updated Cohen's Kappa for {}: {}", username, avgKappa);
        }
    }

    /**
     * Calculate Cohen's Kappa between two specific users
     */
    private double calculateCohenKappaBetweenUsers(String user1, String user2) {
        // Get all images both users classified
        List<Classification> user1Classifications = classificationRepository
                .findByUser_Username(user1);
        List<Classification> user2Classifications = classificationRepository
                .findByUser_Username(user2);

        // Find common images
        Set<Long> user1Images = user1Classifications.stream()
                .map(c -> c.getImage().getId())
                .collect(Collectors.toSet());

        Map<Long, Long> user2ImageToLabel = user2Classifications.stream()
                .collect(Collectors.toMap(
                        c -> c.getImage().getId(),
                        c -> c.getLabel().getId(),
                        (v1, v2) -> v1  // If duplicate, take first
                ));

        // Build parallel lists of labels for common images
        List<Long> user1Labels = new ArrayList<>();
        List<Long> user2Labels = new ArrayList<>();

        for (Classification c1 : user1Classifications) {
            Long imageId = c1.getImage().getId();
            if (user2ImageToLabel.containsKey(imageId)) {
                user1Labels.add(c1.getLabel().getId());
                user2Labels.add(user2ImageToLabel.get(imageId));
            }
        }

        if (user1Labels.size() < 2) {
            log.warn("Not enough common classifications between {} and {} (found {})",
                    user1, user2, user1Labels.size());
            return Double.NaN;
        }

        return CredibilityCalculator.calculateCohenKappa(user1Labels, user2Labels);
    }

    /**
     * Calculate Fleiss' Kappa for images with 3+ classifications
     * and update user's average Fleiss score
     */
    @Transactional
    public void updateFleissKappaForUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Get all classifications
        List<Classification> allClassifications = classificationRepository.findAll();

        // Group by imageId
        Map<Long, List<Classification>> imageGroups = allClassifications.stream()
                .collect(Collectors.groupingBy(c -> c.getImage().getId()));

        // Filter images with 3+ raters
        Map<Long, Map<String, Long>> classificationsMatrix = new HashMap<>();

        for (Map.Entry<Long, List<Classification>> entry : imageGroups.entrySet()) {
            Long imageId = entry.getKey();
            List<Classification> classifications = entry.getValue();

            if (classifications.size() >= 3) {
                Map<String, Long> userToLabel = classifications.stream()
                        .collect(Collectors.toMap(
                                c -> c.getUser().getUsername(),
                                c -> c.getLabel().getId(),
                                (v1, v2) -> v1  // Take first if duplicate
                        ));

                // Only include if our target user participated
                if (userToLabel.containsKey(username)) {
                    classificationsMatrix.put(imageId, userToLabel);
                }
            }
        }

        if (classificationsMatrix.isEmpty()) {
            log.warn("No images with 3+ classifications found for user {}", username);
            return;
        }

        try {
            double fleissKappa = CredibilityCalculator.calculateUserFleissContribution(
                    classificationsMatrix, username);

            user.setFleissKappaAvg(fleissKappa);
            user.setCredibilityLastUpdated(LocalDateTime.now());
            userRepository.save(user);

            log.info("Updated Fleiss' Kappa for {}: {}", username, fleissKappa);
        } catch (Exception e) {
            log.error("Error calculating Fleiss' Kappa for {}: {}", username, e.getMessage());
        }
    }

    /**
     * Update overall credibility score (weighted average of Cohen's and Fleiss')
     */
    @Transactional
    public void updateOverallCredibilityScore(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Recalculate both metrics
        updateCohenKappaForUser(username);
        updateFleissKappaForUser(username);

        // Refresh user from DB
        user = userRepository.findByUsername(username).orElseThrow();

        // Weighted average (60% Cohen's, 40% Fleiss')
        double cohenWeight = 0.6;
        double fleissWeight = 0.4;

        double cohenScore = user.getCohenKappaAvg() != null ? user.getCohenKappaAvg() : 0.0;
        double fleissScore = user.getFleissKappaAvg() != null ? user.getFleissKappaAvg() : 0.0;

        double credibilityScore = (cohenScore * cohenWeight) + (fleissScore * fleissWeight);

        // Normalize to 0-100 scale (assuming kappa is -1 to 1, map to 0-100)
        credibilityScore = ((credibilityScore + 1) / 2) * 100;

        user.setCredibilityScore(credibilityScore);
        userRepository.save(user);

        log.info("Updated overall credibility for {}: {} (Cohen: {}, Fleiss: {})",
                username, credibilityScore, cohenScore, fleissScore);
    }

    /**
     * Trigger credibility update after a new classification
     */
    public void onNewClassification(String username, Long imageId) {
        log.info("Triggering credibility update for user {} after classifying image {}",
                username, imageId);

        // Update in background/async to avoid blocking the classification flow
        updateOverallCredibilityScore(username);

        // Check if this creates a new multi-user scenario
        List<Classification> imageClassifications = classificationRepository.findByImageId(imageId);
        if (imageClassifications.size() >= 3) {
            // Update Fleiss for all users who classified this image
            imageClassifications.stream()
                    .map(c -> c.getUser().getUsername())
                    .distinct()
                    .forEach(this::updateFleissKappaForUser);
        }
    }
}