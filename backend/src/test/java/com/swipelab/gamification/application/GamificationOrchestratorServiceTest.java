package com.swipelab.gamification.application;

import com.swipelab.classification.domain.core.Classification.UserResponse;
import com.swipelab.classification.events.ClassificationSubmittedEvent;
import com.swipelab.gamification.challenge.ChallengeEngine;
import com.swipelab.gamification.challenge.MetricType;
import com.swipelab.gamification.domain.BadgeService;
import com.swipelab.gamification.domain.Gamification;
import com.swipelab.gamification.domain.PointsService;
import com.swipelab.gamification.domain.RankService;
import com.swipelab.gamification.domain.RankTier;
import com.swipelab.gamification.domain.StreakService;
import com.swipelab.gamification.infrastructure.GamificationRepository;
import com.swipelab.users.domain.User;
import com.swipelab.users.infrastructure.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GamificationOrchestratorServiceTest {

    @Mock private StreakService streakService;
    @Mock private PointsService pointsService;
    @Mock private BadgeService badgeService;
    @Mock private RankService rankService;
    @Mock private GamificationRepository gamificationRepository;
    @Mock private UserRepository userRepository;
    @Mock private ChallengeEngine challengeEngine;

    @InjectMocks
    private GamificationOrchestratorService gamificationOrchestratorService;

    // ── Existing tests (updated for new mocks) ────────────────────────────────

    @Test
    void onClassificationSubmitted_ShouldProcessGamification() {
        ClassificationSubmittedEvent event = ClassificationSubmittedEvent.builder()
                .username("testuser")
                .isCorrect(true)
                .isGoldStandard(true)
                .userResponse(UserResponse.YES)
                .submittedAt(LocalDateTime.now())
                .build();

        User user = new User();
        user.setUsername("testuser");
        user.setTotalClassifications(10);

        Gamification gamification = Gamification.builder()
                .username("testuser").yesTagCount(0).score(0L).build();

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(gamificationRepository.findById("testuser")).thenReturn(Optional.of(gamification));
        when(rankService.computeRank(anyInt())).thenReturn(RankTier.BEGINNER);

        gamificationOrchestratorService.onClassificationSubmitted(event);

        verify(streakService, times(1)).updateStreak("testuser");
        verify(pointsService, times(1)).calculateAndAddPoints("testuser", 10);
        verify(pointsService, times(1)).calculateAndAddPoints("testuser", 50);
        verify(badgeService, times(1)).checkForBadges("testuser", 10);
    }

    @Test
    void onClassificationSubmitted_ShouldFeedStreakAndPointsToChallengeEngine() {
        ClassificationSubmittedEvent event = ClassificationSubmittedEvent.builder()
                .username("carol")
                .isCorrect(false)
                .isGoldStandard(false)
                .userResponse(UserResponse.NO)
                .submittedAt(LocalDateTime.now())
                .build();

        User user = new User();
        user.setUsername("carol");
        user.setTotalClassifications(5);

        Gamification gamification = Gamification.builder()
                .username("carol").yesTagCount(0).score(1200L).currentStreak(4).build();

        when(userRepository.findByUsername("carol")).thenReturn(Optional.of(user));
        when(gamificationRepository.findById("carol")).thenReturn(Optional.of(gamification));

        gamificationOrchestratorService.onClassificationSubmitted(event);

        // The absolute streak and points are reported to the engine (LATEST metrics).
        verify(challengeEngine, times(1)).processAction("carol", MetricType.STREAK, 4, null);
        verify(challengeEngine, times(1)).processAction("carol", MetricType.XP_GAINED, 1200, null);
    }

    @Test
    void onClassificationSubmitted_ShouldNotAddBonus_WhenNotCorrectGoldStandard() {
        ClassificationSubmittedEvent event = ClassificationSubmittedEvent.builder()
                .username("testuser")
                .isCorrect(false)
                .isGoldStandard(true)
                .userResponse(UserResponse.NO)
                .submittedAt(LocalDateTime.now())
                .build();

        User user = new User();
        user.setUsername("testuser");
        user.setTotalClassifications(10);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        gamificationOrchestratorService.onClassificationSubmitted(event);

        verify(streakService, times(1)).updateStreak("testuser");
        verify(pointsService, times(1)).calculateAndAddPoints("testuser", 10);
        verify(pointsService, never()).calculateAndAddPoints(eq("testuser"), eq(50));
    }

    // ── New rank-related tests ────────────────────────────────────────────────

    @Test
    void onClassificationSubmitted_YesResponse_ShouldIncrementYesTagCountAndUpdateRank() {
        ClassificationSubmittedEvent event = ClassificationSubmittedEvent.builder()
                .username("alice")
                .isCorrect(false)
                .isGoldStandard(false)
                .userResponse(UserResponse.YES)
                .submittedAt(LocalDateTime.now())
                .build();

        Gamification gamification = Gamification.builder()
                .username("alice").yesTagCount(49).score(0L).build();

        when(userRepository.findByUsername("alice")).thenReturn(Optional.empty());
        when(gamificationRepository.findById("alice")).thenReturn(Optional.of(gamification));
        when(rankService.computeRank(50)).thenReturn(RankTier.EXPERT);

        gamificationOrchestratorService.onClassificationSubmitted(event);

        ArgumentCaptor<Gamification> captor = ArgumentCaptor.forClass(Gamification.class);
        verify(gamificationRepository).save(captor.capture());
        assertThat(captor.getValue().getYesTagCount()).isEqualTo(50);
        assertThat(captor.getValue().getRank()).isEqualTo(RankTier.EXPERT.name());
    }

    @Test
    void onClassificationSubmitted_NonYesResponse_ShouldNotTouchYesTagCount() {
        ClassificationSubmittedEvent event = ClassificationSubmittedEvent.builder()
                .username("bob")
                .isCorrect(false)
                .isGoldStandard(false)
                .userResponse(UserResponse.DONT_KNOW)
                .submittedAt(LocalDateTime.now())
                .build();

        when(userRepository.findByUsername("bob")).thenReturn(Optional.empty());

        gamificationOrchestratorService.onClassificationSubmitted(event);

        // GamificationRepository.save should NOT be called for rank update
        verify(gamificationRepository, never()).save(any(Gamification.class));
    }
}

