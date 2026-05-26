package com.swipelab.classification.domain;

import com.swipelab.classification.domain.WarningLevel;
import com.swipelab.model.enums.UserRole;
import com.swipelab.model.enums.UserStatus;
import com.swipelab.users.domain.User;
import com.swipelab.users.events.UserWarnedEvent;
import com.swipelab.users.events.UserBannedBySystemEvent;
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
 * Integration test for the graduated fraud detection event flow.
 *
 * Flow under test:
 *   UserWarnedEvent    → UserEventListener.onUserWarned    → WARNED status + credibility penalty
 *   UserBannedBySystemEvent → UserEventListener.onUserBannedBySystem → BANNED status + locked
 *
 * Real in-memory ApplicationEventPublisher; H2 as database (integration profile).
 */
@SpringBootTest
@ActiveProfiles("integration")
class FraudDetectionFlowIntegrationTest {

    private static final String TEST_USER = "fraud_test_user";
    private static final double INITIAL_CREDIBILITY = 80.0;

    @Autowired private ApplicationEventPublisher eventPublisher;
    @Autowired private UserRepository userRepository;
    @Autowired private FraudDetectionService fraudDetectionService;
    @Autowired private PlatformTransactionManager transactionManager;

    private TransactionTemplate txTemplate;

    @BeforeEach
    void setUp() {
        txTemplate = new TransactionTemplate(transactionManager);
        txTemplate.execute(status -> {
            userRepository.deleteById(TEST_USER);
            return null;
        });

        User user = User.builder()
                .username(TEST_USER)
                .email(TEST_USER + "@test.com")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .credibilityScore(INITIAL_CREDIBILITY)
                .accountLocked(false)
                .strikeCount(0)
                .warningCount(0)
                .consecutiveCorrectGolds(0)
                .build();

        txTemplate.execute(status -> {
            userRepository.save(user);
            return null;
        });
    }

    private User readUser(String username) {
        return txTemplate.execute(status -> userRepository.findById(username).orElse(null));
    }

    // ── Test 1: UserWarnedEvent → WARNED status + credibility penalty ─────────

    @Test
    @DisplayName("UserWarnedEvent(WARNING_1) → user status = WARNED, credibility reduced by 5")
    void whenUserWarnedEvent_thenStatusIsWarnedAndCredibilityReduced() {
        UserWarnedEvent event = UserWarnedEvent.builder()
                .username(TEST_USER)
                .level(WarningLevel.WARNING_1)
                .reason("Fast response pattern")
                .strikeCount(5)
                .strikesUntilBan(10)
                .detectedAt(LocalDateTime.now())
                .build();

        eventPublisher.publishEvent(event);

        await()
                .atMost(15, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    User updated = readUser(TEST_USER);
                    assertThat(updated).isNotNull();
                    assertThat(updated.getStatus())
                            .as("User should be WARNED after WARNING_1 event")
                            .isEqualTo(UserStatus.WARNED);
                    assertThat(updated.getCredibilityScore())
                            .as("Credibility should be reduced by 5 (WARNING_1 penalty)")
                            .isEqualTo(INITIAL_CREDIBILITY - 5.0);
                });
    }

    // ── Test 2: UserBannedBySystemEvent → BANNED + locked ────────────────────

    @Test
    @DisplayName("UserBannedBySystemEvent → user status = BANNED, accountLocked = true")
    void whenUserBannedBySystemEvent_thenStatusIsBannedAndAccountLocked() {
        UserBannedBySystemEvent event = UserBannedBySystemEvent.builder()
                .username(TEST_USER)
                .reason("Exceeded 15 strikes")
                .totalStrikes(15)
                .bannedAt(LocalDateTime.now())
                .build();

        eventPublisher.publishEvent(event);

        await()
                .atMost(15, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    User updated = readUser(TEST_USER);
                    assertThat(updated).isNotNull();
                    assertThat(updated.getStatus())
                            .as("User should be BANNED after system ban event")
                            .isEqualTo(UserStatus.BANNED);
                    assertThat(updated.getAccountLocked())
                            .as("Account should be locked after system ban")
                            .isTrue();
                    assertThat(updated.getActive())
                            .as("User should be inactive after system ban")
                            .isFalse();
                });
    }

    // ── Test 3: WARNING_2 → larger credibility penalty ────────────────────────

    @Test
    @DisplayName("UserWarnedEvent(WARNING_2) → credibility reduced by 15")
    void whenWarning2Event_thenLargerPenaltyApplied() {
        UserWarnedEvent event = UserWarnedEvent.builder()
                .username(TEST_USER)
                .level(WarningLevel.WARNING_2)
                .reason("Repeated fast-response pattern")
                .strikeCount(10)
                .strikesUntilBan(5)
                .detectedAt(LocalDateTime.now())
                .build();

        eventPublisher.publishEvent(event);

        await()
                .atMost(15, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    User updated = readUser(TEST_USER);
                    assertThat(updated).isNotNull();
                    assertThat(updated.getCredibilityScore())
                            .as("Credibility should be reduced by 15 (WARNING_2 penalty)")
                            .isEqualTo(INITIAL_CREDIBILITY - 15.0);
                });
    }
}
