package com.swipelab.gamification.event;

import com.swipelab.classification.events.ClassificationSubmittedEvent;
import com.swipelab.gamification.challenge.ChallengeEngine;
import com.swipelab.gamification.challenge.MetricType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChallengeEventListener {

    private final ChallengeEngine challengeEngine;

    @KafkaListener(topics = "classification-events", groupId = "gamification-challenges-group")
    public void handleClassificationSubmitted(ClassificationSubmittedEvent event) {
        log.debug("Processing ClassificationSubmittedEvent for challenges for user: {}", event.getUsername());
        
        // When a classification is submitted, we record 1 count for CLASSIFICATION metric
        // The distinct value could be the species if we implement DISTINCT_COUNT
        challengeEngine.processAction(
                event.getUsername(), 
                MetricType.CLASSIFICATION, 
                1, 
                event.getSpecies()
        );
    }
}
