package com.swipelab.users.application;

import com.swipelab.gamification.events.GamificationUpdatedEvent;
import com.swipelab.users.domain.User;
import com.swipelab.users.infrastructure.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Integration test for the Gamification → User Sync event flow.
 *
 * Flow under test:
 * GamificationUpdatedEvent → "gamification-events" topic →
 * GamificationEventListener
 * ↓
 * User entity: score / badges / rank updated in DB
 *
 * A real in-memory ApplicationEventPublisher handles the event routing.
 */
@SpringBootTest
@ActiveProfiles("integration")

class GamificationSyncIntegrationTest {

    private static final String SYNC_USER = "gamification_sync_user";

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteById(SYNC_USER);

        User user = User.builder()
                .username(SYNC_USER)
                .email(SYNC_USER + "@test.com")
                .score(0L)
                .badges(null)
                .rank("UNRANKED")
                .build();
        userRepository.save(user);
    }

    // -------------------------------------------------------------------------
    // Test 1: GamificationUpdatedEvent → score synced to User table
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("GamificationUpdatedEvent on 'gamification-events' → User score is synced in DB")
    void whenGamificationUpdatePublished_thenUserScoreIsSynced() {
        GamificationUpdatedEvent event = GamificationUpdatedEvent.builder()
                .username(SYNC_USER)
                .score(500L)
                .badges("FirstClassification")
                .rank("BRONZE")
                .build();

        eventPublisher.publishEvent(event);

        await()
                .atMost(10, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    User updated = userRepository.findById(SYNC_USER).orElseThrow();
                    assertThat(updated.getScore())
                            .as("User score should be synced to 500")
                            .isEqualTo(500L);
                });
    }

    // -------------------------------------------------------------------------
    // Test 2: GamificationUpdatedEvent → badges synced to User table
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("GamificationUpdatedEvent on 'gamification-events' → User badges are synced in DB")
    void whenGamificationUpdatePublished_thenUserBadgesAreSynced() {
        GamificationUpdatedEvent event = GamificationUpdatedEvent.builder()
                .username(SYNC_USER)
                .score(1000L)
                .badges("Gold Badge,Streak Master")
                .rank("GOLD")
                .build();

        eventPublisher.publishEvent(event);

        await()
                .atMost(10, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    User updated = userRepository.findById(SYNC_USER).orElseThrow();
                    assertThat(updated.getBadges())
                            .as("User badges should be synced")
                            .isEqualTo("Gold Badge,Streak Master");
                    assertThat(updated.getRank())
                            .as("User rank should be synced to GOLD")
                            .isEqualTo("GOLD");
                });
    }

    // -------------------------------------------------------------------------
    // Test 3: Multiple gamification events → only the latest state is reflected
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Two GamificationUpdatedEvents in sequence → latest values are in DB")
    void whenMultipleGamificationUpdatesPublished_thenLatestValuesAreSynced() {
        // First event
        eventPublisher.publishEvent(GamificationUpdatedEvent.builder()
                .username(SYNC_USER)
                .score(100L)
                .badges("Beginner")
                .rank("BRONZE")
                .build());

        await()
                .atMost(10, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    User updated = userRepository.findById(SYNC_USER).orElseThrow();
                    assertThat(updated.getScore()).isEqualTo(100L);
                });

        // Second event with higher values
        eventPublisher.publishEvent(GamificationUpdatedEvent.builder()
                .username(SYNC_USER)
                .score(750L)
                .badges("Beginner,Intermediate")
                .rank("SILVER")
                .build());

        await()
                .atMost(10, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    User updated = userRepository.findById(SYNC_USER).orElseThrow();
                    assertThat(updated.getScore())
                            .as("Score should be from the second event (750)")
                            .isEqualTo(750L);
                    assertThat(updated.getRank())
                            .as("Rank should be from the second event (SILVER)")
                            .isEqualTo("SILVER");
                });
    }

    // -------------------------------------------------------------------------
    // Test 4: GamificationUpdatedEvent for unknown user → no error, graceful skip
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("GamificationUpdatedEvent for unknown user → listener skips gracefully without crash")
    void whenGamificationUpdatePublished_forUnknownUser_thenNoErrorOccurs() {
        GamificationUpdatedEvent event = GamificationUpdatedEvent.builder()
                .username("i_do_not_exist")
                .score(999L)
                .badges("Ghost")
                .rank("PHANTOM")
                .build();

        // Should not throw — listener uses ifPresent()
        eventPublisher.publishEvent(event);

        // Give the listener time to process; verify no data corruption
        await()
                .atMost(5, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    // The real user should still be unaffected
                    User realUser = userRepository.findById(SYNC_USER).orElseThrow();
                    assertThat(realUser.getScore())
                            .as("Real user should still have score 0 — unaffected by ghost event")
                            .isEqualTo(0L);
                });
    }
}
