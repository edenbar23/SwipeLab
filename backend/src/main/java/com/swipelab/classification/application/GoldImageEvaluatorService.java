package com.swipelab.classification.application;

import com.swipelab.classification.domain.Classification;
import com.swipelab.classification.domain.CredibilityRecord;
import com.swipelab.classification.domain.GoldImage;
import com.swipelab.classification.domain.Image;
import com.swipelab.classification.infrastructure.CredibilityRepository;
import com.swipelab.classification.infrastructure.GoldImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Responsible exclusively for evaluating a user response against a gold-standard image.
 *
 * Extracted from ClassificationService to follow the Single Responsibility Principle.
 * If the image is not a gold image, returns empty.
 * If it is, evaluates correctness, persists a CredibilityRecord, and returns the result.
 */
@Service
@RequiredArgsConstructor
public class GoldImageEvaluatorService {

    private final GoldImageRepository goldImageRepository;
    private final CredibilityRepository credibilityRepository;

    /**
     * Evaluates the user's decision against a gold image (if one exists for the given image).
     *
     * @return Optional.empty() if the image is not a gold standard image,
     *         otherwise a GoldImageEvaluationResult with correctness and species.
     */
    public Optional<GoldImageEvaluationResult> evaluate(
            Image image,
            Long taskId,
            String username,
            Classification.UserResponse decision) {

        Optional<GoldImage> goldImageOpt = goldImageRepository.findByImageIdAndActiveTrue(image.getId());
        if (goldImageOpt.isEmpty()) {
            return Optional.empty();
        }

        GoldImage goldImage = goldImageOpt.get();
        boolean isCorrect = goldImage.getCorrectAnswer().name().equals(decision.name());
        String species = goldImage.getSpecies();

        credibilityRepository.save(CredibilityRecord.builder()
                .username(username)
                .taskId(taskId)
                .goldImage(goldImage)
                .querySpecies(species)
                .userResponse(decision)
                .correctAnswer(goldImage.getCorrectAnswer())
                .build());

        return Optional.of(new GoldImageEvaluationResult(isCorrect, species));
    }
}
