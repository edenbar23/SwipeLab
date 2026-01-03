package com.swipelab.service.classification;

import com.swipelab.model.entity.User;
import com.swipelab.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class FraudDetectionService {

    private final UserRepository userRepository;

    // Thresholds based on Non-Functional Requirements
    private static final long MIN_RESPONSE_TIME_MS = 500;

    @Transactional
    public void analyzeClassification(User user, long responseTimeMs) {
        boolean isSuspicious = false;

        // 1. Response Time Analysis
        if (responseTimeMs < MIN_RESPONSE_TIME_MS) {
            log.warn("Suspiciously fast response detected for user {}: {}ms", user.getUsername(), responseTimeMs);
            isSuspicious = true;
        }

        // 2. Simple Pattern Detection Placeholder
        // Future logic for "all Yes" or "perfect trap accuracy" goes here

        if (isSuspicious) {
            flagUser(user, "Non-human response speed: " + responseTimeMs + "ms");
        }
    }

    private void flagUser(User user, String reason) {
        log.info("Flagging user {} for fraud. Reason: {}", user.getUsername(), reason);

        // Mark user as flagged for manual review
        user.setAccountLocked(true); // Temporary lock or use custom flag

        // Penalize credibility score
        Double currentScore = user.getCredibilityScore();
        user.setCredibilityScore(Math.max(0.0, currentScore - 10.0));

        userRepository.save(user);
    }
}