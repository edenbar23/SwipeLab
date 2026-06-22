package com.swipelab.classification.application;

import com.swipelab.classification.domain.Classification;
import com.swipelab.classification.domain.FraudDetectionService;
import com.swipelab.classification.domain.Image;
import com.swipelab.classification.domain.ImageService;
import com.swipelab.classification.dto.UserClassification;
import com.swipelab.classification.dto.api.NextBatchResponse;
import com.swipelab.classification.dto.api.SubmitClassificationRequest;
import com.swipelab.classification.events.ClassificationSubmittedEvent;
import com.swipelab.classification.domain.FraudAnalysisResult;
import com.swipelab.classification.infrastructure.ClassificationRepository;
import com.swipelab.classification.infrastructure.ImageRepository;
import com.swipelab.classification.application.port.out.TaskProvider;
import com.swipelab.exception.ResourceNotFoundException;
import com.swipelab.exception.UserBannedException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ClassificationService {

    private final ClassificationRepository classificationRepository;
    private final ImageRepository imageRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final FraudDetectionService fraudDetectionService;
    private final ImageService imageService;
    private final TaskProvider taskProvider;
    private final GoldImageEvaluatorService goldImageEvaluatorService;

    @Transactional
    public NextBatchResponse submitClassification(String username, String userRole, Double userCredibility,
            SubmitClassificationRequest request) {

        // 1. Fraud Detection — passes role so researchers get a lenient threshold
        FraudAnalysisResult fraudResult = null;
        if (request.getResponseTimeMs() != null) {
            fraudResult = fraudDetectionService.analyzeClassification(
                    username, userRole, request.getResponseTimeMs(), request.getTaskId());
            if (fraudResult.isBanned()) {
                throw new UserBannedException("Account banned due to suspicious activity");
            }
        }

        // 2. Load Image
        Image image = imageRepository.findById(request.getImageId())
                .orElseThrow(() -> new ResourceNotFoundException("Image not found: " + request.getImageId()));

        // 3. Evaluate (Gold or Regular) and publish event
        Optional<GoldImageEvaluationResult> goldResult = goldImageEvaluatorService.evaluate(
                image, request.getTaskId(), username, request.getDecision());

        ClassificationSubmittedEvent event;
        if (goldResult.isPresent()) {
            GoldImageEvaluationResult result = goldResult.get();

            Classification classification = Classification.builder()
                    .username(username)
                    .userRole(userRole)
                    .taskId(request.getTaskId())
                    .image(image)
                    .querySpecies(result.species())
                    .userResponse(request.getDecision())
                    .build();
            Classification saved = classificationRepository.save(classification);

            event = ClassificationSubmittedEvent.builder()
                    .username(username)
                    .classificationId(saved.getId())
                    .imageId(image.getId())
                    .taskId(request.getTaskId())
                    .isCorrect(result.isCorrect())
                    .isGoldStandard(true)
                    .submittedAt(saved.getCreatedAt())
                    .species(result.species())
                    .userResponse(request.getDecision())
                    .responseTimeMs(request.getResponseTimeMs())
                    .userCredibility(userCredibility)
                    .userRole(userRole)
                    .build();
        } else {
            TaskProvider.TaskInfo taskInfo = taskProvider.getTaskInfo(request.getTaskId());
            String species = taskInfo.querySpecies();
            if (species == null || species.isBlank()) {
                if (request.getQuestion() != null && request.getQuestion().startsWith("Is this a ") && request.getQuestion().endsWith("?")) {
                    species = request.getQuestion().substring("Is this a ".length(), request.getQuestion().length() - 1);
                } else {
                    species = (taskInfo.targetSpeciesNames() != null && !taskInfo.targetSpeciesNames().isEmpty())
                            ? taskInfo.targetSpeciesNames().get(0)
                            : null;
                }
            }
            Classification classification = Classification.builder()
                    .username(username)
                    .userRole(userRole)
                    .taskId(request.getTaskId())
                    .image(image)
                    .querySpecies(species)
                    .userResponse(request.getDecision())
                    .build();
            Classification saved = classificationRepository.save(classification);

            event = ClassificationSubmittedEvent.builder()
                    .username(username)
                    .classificationId(saved.getId())
                    .imageId(image.getId())
                    .taskId(request.getTaskId())
                    .isCorrect(false)
                    .isGoldStandard(false)
                    .submittedAt(saved.getCreatedAt())
                    .species(species)
                    .userResponse(request.getDecision())
                    .responseTimeMs(request.getResponseTimeMs())
                    .userCredibility(userCredibility)
                    .userRole(userRole)
                    .build();
        }

        // 4. Publish Event
        eventPublisher.publishEvent(event);

        // 5. Return Next Batch
        NextBatchResponse response = imageService.getNextBatchForApi(request.getTaskId(), username, 10);
        if (fraudResult != null && fraudResult.getWarning() != null) {
            response.setWarning(fraudResult.getWarning());
        }
        return response;
    }

    @Transactional
    public void submitBatchResponses(String username, String userRole, Long taskId,
            List<UserClassification> responses) {
        for (UserClassification response : responses) {
            Image image = imageRepository.findById(response.getImageId())
                    .orElseThrow(() -> new ResourceNotFoundException("Image not found: " + response.getImageId()));

            Optional<GoldImageEvaluationResult> goldResult = goldImageEvaluatorService.evaluate(
                    image, taskId, username, response.getUserResponse());

            ClassificationSubmittedEvent event;
            if (goldResult.isPresent()) {
                GoldImageEvaluationResult result = goldResult.get();

                Classification classification = Classification.builder()
                        .username(username)
                        .userRole(userRole)
                        .taskId(taskId)
                        .image(image)
                        .querySpecies(result.species())
                        .userResponse(response.getUserResponse())
                        .build();
                Classification saved = classificationRepository.save(classification);

                event = ClassificationSubmittedEvent.builder()
                        .username(username)
                        .classificationId(saved.getId())
                        .imageId(image.getId())
                        .taskId(taskId)
                        .isCorrect(result.isCorrect())
                        .isGoldStandard(true)
                        .submittedAt(saved.getCreatedAt())
                        .species(result.species())
                        .userResponse(response.getUserResponse())
                        .userCredibility(null)
                        .userRole(userRole)
                        .build();
            } else {
                TaskProvider.TaskInfo taskInfo = taskProvider.getTaskInfo(taskId);
                String species = taskInfo.querySpecies();
                if (species == null || species.isBlank()) {
                    species = (taskInfo.targetSpeciesNames() != null && !taskInfo.targetSpeciesNames().isEmpty())
                            ? taskInfo.targetSpeciesNames().get(0)
                            : null;
                }
                Classification classification = Classification.builder()
                        .username(username)
                        .userRole(userRole)
                        .taskId(taskId)
                        .image(image)
                        .querySpecies(species)
                        .userResponse(response.getUserResponse())
                        .build();
                Classification saved = classificationRepository.save(classification);

                event = ClassificationSubmittedEvent.builder()
                        .username(username)
                        .classificationId(saved.getId())
                        .imageId(image.getId())
                        .taskId(taskId)
                        .isCorrect(false)
                        .isGoldStandard(false)
                        .submittedAt(saved.getCreatedAt())
                        .species(species)
                        .userResponse(response.getUserResponse())
                        .userCredibility(null)
                        .userRole(userRole)
                        .build();
            }

            eventPublisher.publishEvent(event);
        }
    }
}
