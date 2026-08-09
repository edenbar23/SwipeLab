package com.swipelab.gamification.events;

import com.swipelab.gamification.application.challenge.ChallengeEngine;
import com.swipelab.gamification.domain.challenge.MetricType;
import com.swipelab.classification.events.ClassificationSubmittedEvent;
import com.swipelab.gamification.application.challenge.ChallengeEngine;
import com.swipelab.gamification.domain.challenge.MetricType;
import com.swipelab.gamification.application.challenge.ChallengeEngine;
import com.swipelab.gamification.application.challenge.ChallengeEngine;
import com.swipelab.gamification.domain.challenge.MetricType;
import com.swipelab.gamification.domain.challenge.MetricType;
import com.swipelab.gamification.application.challenge.ChallengeEngine;
import com.swipelab.gamification.domain.challenge.MetricType;
import lombok.RequiredArgsConstructor;
import com.swipelab.gamification.application.challenge.ChallengeEngine;
import com.swipelab.gamification.domain.challenge.MetricType;
import lombok.extern.slf4j.Slf4j;
import com.swipelab.gamification.application.challenge.ChallengeEngine;
import com.swipelab.gamification.domain.challenge.MetricType;
import org.springframework.context.event.EventListener;
import com.swipelab.gamification.application.challenge.ChallengeEngine;
import com.swipelab.gamification.domain.challenge.MetricType;
import org.springframework.scheduling.annotation.Async;
import com.swipelab.gamification.application.challenge.ChallengeEngine;
import com.swipelab.gamification.domain.challenge.MetricType;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChallengeEventListener {

    private final ChallengeEngine challengeEngine;

    @Async
    @EventListener
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



