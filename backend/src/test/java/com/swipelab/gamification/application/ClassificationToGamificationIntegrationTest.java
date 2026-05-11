package com.swipelab.gamification.application;

import com.swipelab.classification.events.ClassificationSubmittedEvent;
import com.swipelab.gamification.domain.Gamification;
import com.swipelab.gamification.infrastructure.GamificationRepository;
import com.swipelab.users.domain.User;
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
 * Integration test: verifies that a ClassificationSubmittedEvent published to
 * "classification-events" is actually consumed by
 * GamificationOrchestratorService
 * and results in points / streaks being recorded in the database.
 *
 * Uses Spring's in-memory ApplicationEvents — no external message broker needed.
 */
@SpringBootTest
@ActiveProfiles("integration")

class ClassificationToGamificationIntegrationTest {

        private static final String TEST_USER = "gamification_test_user";

        @Autowired
        private ApplicationEventPublisher eventPublisher;

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private GamificationRepository gamificationRepository;

        @Autowired
        private PlatformTransactionManager transactionManager;

        private TransactionTemplate txTemplate;

        @BeforeEach
        void setUp() {
                txTemplate = new TransactionTemplate(transactionManager);
                // Clean up any previous test data in their own transactions
                txTemplate.execute(s -> {
                        gamificationRepository.deleteById(TEST_USER);
                        return null;
                });
                txTemplate.execute(s -> {
                        userRepository.deleteById(TEST_USER);
                        return null;
                });

                // Seed a user required by GamificationOrchestratorService
                User user = User.builder()
                                .username(TEST_USER)
                                .email(TEST_USER + "@test.com")
                                .totalClassifications(5)
                                .build();
                txTemplate.execute(s -> {
                        userRepository.save(user);
                        return null;
                });

                // Seed a Gamification record so PointsService / StreakService can find it
                Gamification gamification = Gamification.builder()
                                .username(TEST_USER)
                                .score(0L)
                                .currentStreak(0)
                                .longestStreak(0)
                                .build();
                txTemplate.execute(s -> {
                        gamificationRepository.save(gamification);
                        return null;
                });
        }

        /**
         * Read gamification in a fresh transaction to see committed event listener
         * writes
         */
        private Gamification readGamification() {
                return txTemplate.execute(s -> gamificationRepository.findById(TEST_USER).orElse(null));
        }

        // -------------------------------------------------------------------------
        // Test 1: Regular (non-gold) classification → base 10 points awarded
        // -------------------------------------------------------------------------

        @Test
        @DisplayName("Regular classification event → base 10 points are awarded in Gamification table")
        void whenRegularClassificationPublished_thenBasePointsAreAwarded() {
                ClassificationSubmittedEvent event = ClassificationSubmittedEvent.builder()
                                .username(TEST_USER)
                                .classificationId(1L)
                                .imageId(100L)
                                .taskId(1L)
                                .isCorrect(false)
                                .isGoldStandard(false)
                                .submittedAt(LocalDateTime.now())
                                .species("Lion")
                                .responseTimeMs(2000L)
                                .userCredibility(0.7)
                                .build();

                eventPublisher.publishEvent(event);

                // Wait up to 10 seconds for the consumer to process the message
                await()
                                .atMost(15, TimeUnit.SECONDS)
                                .pollInterval(500, TimeUnit.MILLISECONDS)
                                .untilAsserted(() -> {
                                        Gamification result = readGamification();
                                        assertThat(result).isNotNull();
                                        assertThat(result.getScore())
                                                        .as("User should have received at least 10 base points")
                                                        .isGreaterThanOrEqualTo(10L);
                                });
        }

        // -------------------------------------------------------------------------
        // Test 2: Gold standard + correct → base 10 + bonus 50 = 60 points
        // -------------------------------------------------------------------------

        @Test
        @DisplayName("Gold + correct classification event → 60 total points awarded (10 base + 50 bonus)")
        void whenGoldCorrectClassificationPublished_thenBonusPointsAreAwarded() {
                ClassificationSubmittedEvent event = ClassificationSubmittedEvent.builder()
                                .username(TEST_USER)
                                .classificationId(null)
                                .imageId(200L)
                                .taskId(1L)
                                .isCorrect(true)
                                .isGoldStandard(true)
                                .submittedAt(LocalDateTime.now())
                                .species("Elephant")
                                .responseTimeMs(3000L)
                                .userCredibility(0.9)
                                .build();

                eventPublisher.publishEvent(event);

                // 10 base + 50 gold bonus = 60 points expected
                await()
                                .atMost(15, TimeUnit.SECONDS)
                                .pollInterval(500, TimeUnit.MILLISECONDS)
                                .untilAsserted(() -> {
                                        Gamification result = readGamification();
                                        assertThat(result).isNotNull();
                                        assertThat(result.getScore())
                                                        .as("User should have received 60 points (10 base + 50 gold bonus)")
                                                        .isGreaterThanOrEqualTo(60L);
                                });
        }

        // -------------------------------------------------------------------------
        // Test 3: Event triggers streak update
        // -------------------------------------------------------------------------

        @Test
        @DisplayName("Classification event → streak counter is incremented")
        void whenClassificationPublished_thenStreakIsUpdated() {
                ClassificationSubmittedEvent event = ClassificationSubmittedEvent.builder()
                                .username(TEST_USER)
                                .classificationId(2L)
                                .imageId(300L)
                                .taskId(1L)
                                .isCorrect(true)
                                .isGoldStandard(false)
                                .submittedAt(LocalDateTime.now())
                                .species("Zebra")
                                .responseTimeMs(1800L)
                                .userCredibility(0.8)
                                .build();

                eventPublisher.publishEvent(event);

                await()
                                .atMost(15, TimeUnit.SECONDS)
                                .pollInterval(500, TimeUnit.MILLISECONDS)
                                .untilAsserted(() -> {
                                        Gamification result = readGamification();
                                        assertThat(result).isNotNull();
                                        assertThat(result.getCurrentStreak())
                                                        .as("Streak should be at least 1 after a classification")
                                                        .isGreaterThanOrEqualTo(1);
                                });
        }
}
