package com.swipelab.classification.domain;

import com.swipelab.users.domain.User;
import com.swipelab.users.events.FraudDetectedEvent;
import com.swipelab.users.infrastructure.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Integration test for the Fraud Detection → User Lock event flow.
 *
 * Flow under test:
 * FraudDetectedEvent → "fraud-events" topic → UserEventListener
 * ↓
 * User account locked + credibility score penalized
 * ↓
 * UserStatusChangedEvent → "user-events" topic
 *
 * A real in-memory ApplicationEventPublisher handles the event routing.
 * H2 is used as the database (integration profile).
 */
@SpringBootTest
@ActiveProfiles("integration")

class FraudDetectionFlowIntegrationTest {

        private static final String FRAUD_USER = "fraud_test_user";
        private static final double INITIAL_CREDIBILITY = 80.0;

        @Autowired
        private ApplicationEventPublisher eventPublisher;

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private FraudDetectionService fraudDetectionService;

        @Autowired
        private PlatformTransactionManager transactionManager;

        private TransactionTemplate txTemplate;

        @BeforeEach
        void setUp() {
                txTemplate = new TransactionTemplate(transactionManager);
                txTemplate.execute(status -> {
                        userRepository.deleteById(FRAUD_USER);
                        return null;
                });

                User user = User.builder()
                                .username(FRAUD_USER)
                                .email(FRAUD_USER + "@test.com")
                                .credibilityScore(INITIAL_CREDIBILITY)
                                .accountLocked(false)
                                .build();
                txTemplate.execute(status -> {
                        userRepository.save(user);
                        return null;
                });
        }

        /**
         * Helper to read user outside the test transaction to see event listener's
         * commits
         */
        private User readUser(String username) {
                return txTemplate.execute(status -> userRepository.findById(username).orElse(null));
        }

        // -------------------------------------------------------------------------
        // Test 1: FraudDetectedEvent → user account is locked
        // -------------------------------------------------------------------------

        @Test
        @DisplayName("FraudDetectedEvent on 'fraud-events' → user account is locked in DB")
        void whenFraudEventPublished_thenUserAccountIsLocked() {
                FraudDetectedEvent event = FraudDetectedEvent.builder()
                                .username(FRAUD_USER)
                                .reason("Non-human response speed: 100ms")
                                .detectedAt(LocalDateTime.now())
                                .build();

                eventPublisher.publishEvent(event);

                await()
                                .atMost(15, TimeUnit.SECONDS)
                                .pollInterval(500, TimeUnit.MILLISECONDS)
                                .untilAsserted(() -> {
                                        User updated = readUser(FRAUD_USER);
                                        assertThat(updated).isNotNull();
                                        assertThat(updated.getAccountLocked())
                                                        .as("Account should be locked after fraud detection")
                                                        .isTrue();
                                });
        }

        // -------------------------------------------------------------------------
        // Test 2: FraudDetectedEvent → credibility score penalized by 10
        // -------------------------------------------------------------------------

        @Test
        @DisplayName("FraudDetectedEvent on 'fraud-events' → credibility score is penalized by 10 points")
        void whenFraudEventPublished_thenCredibilityScoreIsPenalized() {
                FraudDetectedEvent event = FraudDetectedEvent.builder()
                                .username(FRAUD_USER)
                                .reason("Automated bot behavior detected")
                                .detectedAt(LocalDateTime.now())
                                .build();

                eventPublisher.publishEvent(event);

                await()
                                .atMost(15, TimeUnit.SECONDS)
                                .pollInterval(500, TimeUnit.MILLISECONDS)
                                .untilAsserted(() -> {
                                        User updated = readUser(FRAUD_USER);
                                        assertThat(updated).isNotNull();
                                        assertThat(updated.getCredibilityScore())
                                                        .as("Credibility score should be reduced by 10 after fraud")
                                                        .isEqualTo(INITIAL_CREDIBILITY - 10.0);
                                });
        }

        // -------------------------------------------------------------------------
        // Test 3: FraudDetectedEvent → credibility score never goes below 0
        // -------------------------------------------------------------------------

        @Test
        @DisplayName("FraudDetectedEvent with very low credibility → score floors at 0.0")
        void whenFraudEventPublished_thenCredibilityScoreDoesNotGoBelowZero() {
                // Reset user with a low score using explicit transaction
                txTemplate.execute(status -> {
                        userRepository.deleteById(FRAUD_USER);
                        User user = User.builder()
                                        .username(FRAUD_USER)
                                        .email(FRAUD_USER + "@test.com")
                                        .credibilityScore(3.0) // Less than the 10-point penalty
                                        .accountLocked(false)
                                        .build();
                        userRepository.save(user);
                        return null;
                });

                FraudDetectedEvent event = FraudDetectedEvent.builder()
                                .username(FRAUD_USER)
                                .reason("Click farm activity")
                                .detectedAt(LocalDateTime.now())
                                .build();

                eventPublisher.publishEvent(event);

                await()
                                .atMost(15, TimeUnit.SECONDS)
                                .pollInterval(500, TimeUnit.MILLISECONDS)
                                .untilAsserted(() -> {
                                        User updated = readUser(FRAUD_USER);
                                        assertThat(updated).isNotNull();
                                        assertThat(updated.getCredibilityScore())
                                                        .as("Score should be floored at exactly 0.0")
                                                        .isEqualTo(0.0);
                                });
        }

        // -------------------------------------------------------------------------
        // Test 4: FraudDetectionService.analyzeClassification with fast response
        // Actually publishes to fraud-events (tests the PRODUCER side)
        // -------------------------------------------------------------------------

        @Test
        @DisplayName("analyzeClassification with response < 500ms → FraudDetectedEvent is published and user is locked")
        void whenAnalyzeClassificationCalledWithFastResponse_thenUserIsEventuallyLocked() {
                // 200ms is below the 500ms threshold — should trigger fraud
                fraudDetectionService.analyzeClassification(FRAUD_USER, 200L);

                await()
                                .atMost(15, TimeUnit.SECONDS)
                                .pollInterval(500, TimeUnit.MILLISECONDS)
                                .untilAsserted(() -> {
                                        User updated = readUser(FRAUD_USER);
                                        assertThat(updated).isNotNull();
                                        assertThat(updated.getAccountLocked())
                                                        .as("Fast response (200ms) should trigger fraud and lock account")
                                                        .isTrue();
                                });
        }
}
