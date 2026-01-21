package com.swipelab.classification.application;

import com.swipelab.classification.domain.Classification;
import com.swipelab.classification.domain.CredibilityRecord;
import com.swipelab.classification.domain.FraudDetectionService;
import com.swipelab.classification.domain.GoldImage;
import com.swipelab.classification.domain.Image;
import com.swipelab.classification.domain.ImageService;
import com.swipelab.classification.dto.UserClassification;
import com.swipelab.classification.dto.api.NextBatchResponse;
import com.swipelab.classification.dto.api.SubmitClassificationRequest;
import com.swipelab.classification.events.ClassificationSubmittedEvent;
import com.swipelab.classification.infrastructure.ClassificationRepository;
import com.swipelab.classification.infrastructure.CredibilityRepository;
import com.swipelab.classification.infrastructure.GoldImageRepository;
import com.swipelab.classification.infrastructure.ImageRepository;

import com.swipelab.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ClassificationService {

        // Constants
        private static final String CLASSIFICATION_EVENTS_TOPIC = "classification-events";
        private static final int DEFAULT_BATCH_SIZE = 10;

        private final ClassificationRepository classificationRepository;
        private final ImageRepository imageRepository;
        private final GoldImageRepository goldImageRepository;
        private final CredibilityRepository credibilityRepository;
        private final KafkaTemplate<String, Object> kafkaTemplate;

        private final FraudDetectionService fraudDetectionService;
        private final ImageService imageService;

        @Transactional
        public NextBatchResponse submitClassification(String username, String userRole, Double userCredibility,
                        SubmitClassificationRequest request) {
                // 1. Analyze Fraud (Response Time)
                if (request.getResponseTimeMs() != null) {
                        fraudDetectionService.analyzeClassification(username, request.getResponseTimeMs());
                }

                // 2. Process Classification
                processClassification(username, userRole, request.getTaskId(), request.getImageId(),
                                request.getDecision(), request.getResponseTimeMs(), userCredibility);

                // 3. Return Next Batch
                return imageService.getNextBatchForApi(request.getTaskId(), username, DEFAULT_BATCH_SIZE);
        }

        @Transactional
        public void submitBatchResponses(String username, String userRole, Long taskId,
                        List<UserClassification> responses) {
                // Kept for backward compatibility
                for (UserClassification response : responses) {
                        processClassification(username, userRole, taskId, response.getImageId(),
                                        response.getUserResponse(), null, null);
                }
        }

        /**
         * Private helper method to process a single classification.
         * Handles both gold standard and regular classifications.
         * Eliminates code duplication between submitClassification and
         * submitBatchResponses.
         *
         * @param username        Username of the classifier
         * @param userRole        Role of the user (USER, RESEARCHER, etc.)
         * @param taskId          ID of the task
         * @param imageId         ID of the image being classified
         * @param decision        User's classification decision (YES/NO)
         * @param responseTimeMs  Response time in milliseconds (nullable)
         * @param userCredibility User's credibility score (nullable)
         */
        private void processClassification(String username, String userRole, Long taskId, Long imageId,
                        Classification.UserResponse decision, Long responseTimeMs, Double userCredibility) {
                Image image = imageRepository.findById(imageId)
                                .orElseThrow(() -> new ResourceNotFoundException("Image not found: " + imageId));

                Optional<GoldImage> goldImageOpt = goldImageRepository.findByImageId(image.getId());

                String species = image.getTask() != null ? image.getTask().getQuerySpecies() : null;
                if (goldImageOpt.isPresent() && species == null) {
                        species = goldImageOpt.get().getSpecies();
                }

                if (goldImageOpt.isPresent()) {
                        // Gold standard image classification
                        GoldImage goldImage = goldImageOpt.get();
                        boolean isCorrect = goldImage.getCorrectAnswer().name().equals(decision.name());

                        credibilityRepository.save(CredibilityRecord.builder()
                                        .username(username)
                                        .taskId(taskId)
                                        .goldImage(goldImage)
                                        .querySpecies(goldImage.getSpecies())
                                        .userResponse(decision)
                                        .correctAnswer(goldImage.getCorrectAnswer())
                                        .build());

                        ClassificationSubmittedEvent event = ClassificationSubmittedEvent.builder()
                                        .username(username)
                                        .classificationId(null)
                                        .imageId(image.getId())
                                        .taskId(taskId)
                                        .isCorrect(isCorrect)
                                        .isGoldStandard(true)
                                        .submittedAt(java.time.LocalDateTime.now())
                                        .species(species)
                                        .responseTimeMs(responseTimeMs)
                                        .userCredibility(userCredibility)
                                        .userRole(userRole)
                                        .userResponse(decision.name())
                                        .build();

                        kafkaTemplate.send(CLASSIFICATION_EVENTS_TOPIC, event);

                } else {
                        // Regular classification
                        Classification classification = Classification.builder()
                                        .username(username)
                                        .userRole(userRole)
                                        .taskId(taskId)
                                        .image(image)
                                        .querySpecies(species)
                                        .userResponse(decision)
                                        .build();

                        Classification saved = classificationRepository.save(classification);

                        ClassificationSubmittedEvent event = ClassificationSubmittedEvent.builder()
                                        .username(username)
                                        .classificationId(saved.getId())
                                        .imageId(image.getId())
                                        .taskId(taskId)
                                        .isCorrect(false)
                                        .isGoldStandard(false)
                                        .submittedAt(saved.getCreatedAt())
                                        .species(species)
                                        .responseTimeMs(responseTimeMs)
                                        .userCredibility(userCredibility)
                                        .userRole(userRole)
                                        .userResponse(decision.name())
                                        .build();

                        kafkaTemplate.send(CLASSIFICATION_EVENTS_TOPIC, event);
                }
        }
}
