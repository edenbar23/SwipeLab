package com.swipelab.classification.application;

import com.swipelab.classification.domain.threshold.ConsensusResult;
import com.swipelab.classification.domain.threshold.ThresholdPolicy;
import com.swipelab.classification.events.ClassificationSubmittedEvent;
import com.swipelab.classification.events.ImageConsensusReachedEvent;
import com.swipelab.classification.infrastructure.ConsensusResultRepository;
import com.swipelab.classification.application.port.out.TaskProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class ThresholdEventListener {

    private final ThresholdPolicy thresholdPolicy;
    private final ConsensusResultRepository consensusResultRepository;
    private final TaskProvider taskProvider;
    private final ApplicationEventPublisher eventPublisher;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onClassificationSubmitted(ClassificationSubmittedEvent event) {
        // Skip if this image+species already reached consensus
        if (consensusResultRepository.findByImageIdAndSpecies(event.getImageId(), event.getSpecies()).isPresent()) {
            return;
        }

        try {
            // Retrieve dynamic threshold from Task
            TaskProvider.TaskInfo taskInfo = taskProvider.getTaskInfo(event.getTaskId());
            Double targetThreshold = taskInfo.consensusThreshold();
            if (targetThreshold == null) {
                targetThreshold = 3.0; // Fallback
            }

            Optional<ThresholdPolicy.ThresholdResult> resultOpt = thresholdPolicy.evaluateThreshold(
                    event.getImageId(), event.getTaskId(), event.getSpecies(), targetThreshold);

            if (resultOpt.isPresent()) {
                ThresholdPolicy.ThresholdResult result = resultOpt.get();
                
                // Double check to avoid race conditions
                if (consensusResultRepository.findByImageIdAndSpecies(event.getImageId(), event.getSpecies()).isEmpty()) {
                    
                    ConsensusResult consensusResult = ConsensusResult.builder()
                            .imageId(event.getImageId())
                            .taskId(event.getTaskId())
                            .species(event.getSpecies())
                            .winningDecision(result.winningDecision())
                            .finalScore(result.finalScore())
                            .build();
                            
                    ConsensusResult saved = consensusResultRepository.save(consensusResult);
                    
                    log.info("Image {} reached consensus for species {} with decision {} and score {}", 
                            event.getImageId(), event.getSpecies(), result.winningDecision(), result.finalScore());
                    
                    eventPublisher.publishEvent(ImageConsensusReachedEvent.builder()
                            .imageId(saved.getImageId())
                            .taskId(saved.getTaskId())
                            .species(saved.getSpecies())
                            .winningDecision(saved.getWinningDecision())
                            .finalScore(saved.getFinalScore())
                            .reachedAt(saved.getReachedAt() != null ? saved.getReachedAt() : LocalDateTime.now())
                            .build());
                }
            }
        } catch (Exception e) {
            log.error("Failed to evaluate threshold for imageId {} and species {}", event.getImageId(), event.getSpecies(), e);
        }
    }
}
