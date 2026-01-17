package com.swipelab.classification.application;

// Service for handling image classifications
import com.swipelab.dto.request.ClassificationRequest;
import com.swipelab.dto.response.ClassificationResponse;
import com.swipelab.exception.ResourceNotFoundException;
import com.swipelab.classification.domain.Classification;
import com.swipelab.classification.domain.Image;
import com.swipelab.classification.domain.Label;
import com.swipelab.model.entity.User;
import com.swipelab.classification.infrastructure.ClassificationRepository;
import com.swipelab.classification.infrastructure.ImageRepository;
import com.swipelab.classification.infrastructure.LabelRepository;
import com.swipelab.repository.UserRepository;
import com.swipelab.classification.application.FraudDetectionService;
import com.swipelab.gamification.application.BadgeService;
import com.swipelab.gamification.application.PointsService;
import com.swipelab.gamification.application.StreakService;
import com.swipelab.service.user.CredibilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ClassificationService {

    private final ClassificationRepository classificationRepository;
    private final ImageRepository imageRepository;
    private final LabelRepository labelRepository;
    private final UserRepository userRepository;
    private final BadgeService badgeService;
    private final PointsService pointsService;
    private final StreakService streakService;
    private final FraudDetectionService fraudDetectionService;
    private final CredibilityService credibilityService;

    @Transactional
    public ClassificationResponse submitClassification(String username, ClassificationRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

        Image image = imageRepository.findById(request.getImageId())
                .orElseThrow(() -> new ResourceNotFoundException("Image not found with id: " + request.getImageId()));

        Label label = labelRepository.findById(request.getLabelId())
                .orElseThrow(() -> new ResourceNotFoundException("Label not found with id: " + request.getLabelId()));

        // Check if already classified
        if (classificationRepository.existsByUser_UsernameAndImage_Id(username, request.getImageId())) {
            throw new IllegalArgumentException("User has already classified this image");
        }

        Classification classification = Classification.builder()
                .user(user)
                .image(image)
                .label(label)
                .build();

        Classification saved = classificationRepository.save(classification);

        fraudDetectionService.analyzeClassification(user, request.getResponseTimeMs());

        credibilityService.updateUserCredibility(username, request.getImageId());
        // Update User Stats
        user.setTotalClassifications(user.getTotalClassifications() + 1);

        // Gamification: Update Streak
        streakService.updateStreak(user);

        // Gamification: Award points with multiplier (must be after streak update)
        pointsService.calculateAndAddPoints(user, 10);

        // Determine correctness if it's a gold standard image
        Boolean isCorrect = null;
        if (Boolean.TRUE.equals(image.getIsGoldStandard()) && image.getCorrectLabel() != null) {
            isCorrect = image.getCorrectLabel().getId().equals(label.getId());
            if (Boolean.TRUE.equals(isCorrect)) {
                pointsService.calculateAndAddPoints(user, 50); // Bonus for correct gold standard
            }
        }

        // Check for badges
        badgeService.checkForBadges(user);

        // Save user with updated stats (critical fix!)
        userRepository.save(user);

        return mapToResponse(saved, isCorrect);
    }

    private ClassificationResponse mapToResponse(Classification classification, Boolean isCorrect) {
        return ClassificationResponse.builder()
                .id(classification.getId())
                .userId(classification.getUser().getUsername())
                .imageId(classification.getImage().getId())
                .labelId(classification.getLabel().getId())
                .isCorrect(isCorrect)
                .createdAt(classification.getCreatedAt())
                .build();
    }
}
