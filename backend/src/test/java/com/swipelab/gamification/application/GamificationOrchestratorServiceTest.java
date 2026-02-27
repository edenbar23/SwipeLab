package com.swipelab.gamification.application;

import com.swipelab.classification.events.ClassificationSubmittedEvent;
import com.swipelab.gamification.domain.BadgeService;
import com.swipelab.gamification.domain.PointsService;
import com.swipelab.gamification.domain.StreakService;
import com.swipelab.users.domain.User;
import com.swipelab.users.infrastructure.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GamificationOrchestratorServiceTest {

    @Mock
    private StreakService streakService;

    @Mock
    private PointsService pointsService;

    @Mock
    private BadgeService badgeService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private GamificationOrchestratorService gamificationOrchestratorService;

    @Test
    void onClassificationSubmitted_ShouldProcessGamification() {
        ClassificationSubmittedEvent event = ClassificationSubmittedEvent.builder()
                .username("testuser")
                .isCorrect(true)
                .isGoldStandard(true)
                .submittedAt(LocalDateTime.now())
                .build();

        User user = new User();
        user.setUsername("testuser");
        user.setTotalClassifications(10);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        gamificationOrchestratorService.onClassificationSubmitted(event);

        verify(streakService, times(1)).updateStreak("testuser");
        verify(pointsService, times(1)).calculateAndAddPoints("testuser", 10);
        verify(pointsService, times(1)).calculateAndAddPoints("testuser", 50); // Bonus for correct gold
        verify(badgeService, times(1)).checkForBadges("testuser", 10);
    }
    
    @Test
    void onClassificationSubmitted_ShouldNotAddBonus_WhenNotCorrectGoldStandard() {
        ClassificationSubmittedEvent event = ClassificationSubmittedEvent.builder()
                .username("testuser")
                .isCorrect(false)
                .isGoldStandard(true)
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
}
