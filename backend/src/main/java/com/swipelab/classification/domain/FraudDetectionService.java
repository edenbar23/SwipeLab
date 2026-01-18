package com.swipelab.classification.domain;

import com.swipelab.users.events.FraudDetectedEvent;
import com.swipelab.eventing.kafka.KafkaEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class FraudDetectionService {

    private final KafkaEventPublisher eventPublisher;

    private static final String FRAUD_EVENTS_TOPIC = "fraud-events";

    // Thresholds based on Non-Functional Requirements
    private static final long MIN_RESPONSE_TIME_MS = 500;

    @Transactional
    public void analyzeClassification(String username, long responseTimeMs) {
        boolean isSuspicious = false;

        // 1. Response Time Analysis
        if (responseTimeMs < MIN_RESPONSE_TIME_MS) {
            log.warn("Suspiciously fast response detected for user {}: {}ms", username, responseTimeMs);
            isSuspicious = true;
        }

        // 2. Simple Pattern Detection Placeholder
        // Future logic for "all Yes" or "perfect trap accuracy" goes here

        if (isSuspicious) {
            flagUser(username, "Non-human response speed: " + responseTimeMs + "ms");
        }
    }

    private void flagUser(String username, String reason) {
        log.info("Flagging user {} for fraud. Reason: {}", username, reason);

        // Publish Fraud Detected Event
        eventPublisher.publish(FRAUD_EVENTS_TOPIC,
                FraudDetectedEvent.builder()
                        .username(username)
                        .reason(reason)
                        .detectedAt(LocalDateTime.now())
                        .build());
    }
}