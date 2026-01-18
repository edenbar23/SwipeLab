package com.swipelab.classification.application;

import com.swipelab.classification.domain.*;
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
                Image image = imageRepository.findById(request.getImageId())
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Image not found: " + request.getImageId()));

                Optional<GoldImage> goldImageOpt = goldImageRepository.findByImageId(image.getId());

                String species = image.getTask() != null ? image.getTask().getQuerySpecies() : null;
                if (goldImageOpt.isPresent() && species == null) {
                        species = goldImageOpt.get().getSpecies();
                }

                if (goldImageOpt.isPresent()) {
                        GoldImage goldImage = goldImageOpt.get();
                        boolean isCorrect = goldImage.getCorrectAnswer().name()
                                        .equals(request.getDecision().name());

                        credibilityRepository.save(CredibilityRecord.builder()
                                        .username(username)
                                        .taskId(request.getTaskId())
                                        .goldImage(goldImage)
                                        .querySpecies(goldImage.getSpecies())
                                        .userResponse(request.getDecision())
                                        .correctAnswer(goldImage.getCorrectAnswer())
                                        .build());

                        ClassificationSubmittedEvent event = ClassificationSubmittedEvent.builder()
                                        .username(username)
                                        .classificationId(null)
                                        .imageId(image.getId())
                                        .taskId(request.getTaskId())
                                        .isCorrect(isCorrect)
                                        .isGoldStandard(true)
                                        .submittedAt(java.time.LocalDateTime.now())
                                        .species(species)
                                        .responseTimeMs(request.getResponseTimeMs())
                                        .userCredibility(userCredibility)
                                        .build();

                        kafkaTemplate.send("classification-events", event);

                } else {
                        Classification classification = Classification.builder()
                                        .username(username)
                                        .userRole(userRole)
                                        .taskId(request.getTaskId())
                                        .image(image)
                                        .querySpecies(species)
                                        .userResponse(request.getDecision())
                                        .build();

                        Classification saved = classificationRepository.save(classification);

                        ClassificationSubmittedEvent event = ClassificationSubmittedEvent.builder()
                                        .username(username)
                                        .classificationId(saved.getId())
                                        .imageId(image.getId())
                                        .taskId(request.getTaskId())
                                        .isCorrect(false)
                                        .isGoldStandard(false)
                                        .submittedAt(saved.getCreatedAt())
                                        .species(species)
                                        .responseTimeMs(request.getResponseTimeMs())
                                        .userCredibility(userCredibility)
                                        .build();

                        kafkaTemplate.send("classification-events", event);
                }

                // 3. Return Next Batch
                return imageService.getNextBatchForApi(request.getTaskId(), username, 10);
        }

        @Transactional
        public void submitBatchResponses(String username, String userRole, Long taskId,
                        List<UserClassification> responses) {
                // Kept for backward compatibility
                for (UserClassification response : responses) {
                        Image image = imageRepository.findById(response.getImageId())
                                        .orElseThrow(() -> new ResourceNotFoundException(
                                                        "Image not found: " + response.getImageId()));

                        String species = image.getTask() != null ? image.getTask().getQuerySpecies() : null;

                        Optional<GoldImage> goldImageOpt = goldImageRepository.findByImageId(image.getId());

                        if (goldImageOpt.isPresent() && species == null) {
                                species = goldImageOpt.get().getSpecies();
                        }

                        if (goldImageOpt.isPresent()) {
                                GoldImage goldImage = goldImageOpt.get();
                                boolean isCorrect = goldImage.getCorrectAnswer().name()
                                                .equals(response.getUserResponse().name());

                                credibilityRepository.save(CredibilityRecord.builder()
                                                .username(username)
                                                .taskId(taskId)
                                                .goldImage(goldImage)
                                                .querySpecies(goldImage.getSpecies())
                                                .userResponse(response.getUserResponse())
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
                                                .userCredibility(null) // Not available in batch yet
                                                .build();

                                kafkaTemplate.send("classification-events", event);

                        } else {
                                Classification classification = Classification.builder()
                                                .username(username)
                                                .userRole(userRole)
                                                .taskId(taskId)
                                                .image(image)
                                                .querySpecies(species)
                                                .userResponse(response.getUserResponse())
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
                                                .userCredibility(null)
                                                .build();

                                kafkaTemplate.send("classification-events", event);
                        }
                }
        }
}
