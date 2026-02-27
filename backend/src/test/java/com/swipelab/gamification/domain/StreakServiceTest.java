package com.swipelab.gamification.domain;

import com.swipelab.gamification.infrastructure.GamificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StreakServiceTest {

    @Mock
    private GamificationRepository gamificationRepository;

    @InjectMocks
    private StreakService streakService;

    private Gamification gamification;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.now();
        gamification = new Gamification();
        gamification.setUsername("testuser");
        gamification.setCurrentStreak(0);
        gamification.setLongestStreak(0);
        gamification.setScore(0L);
    }

    @Test
    void updateStreak_ShouldStartNewStreak_WhenNoPreviousUpdate() {
        when(gamificationRepository.findById("testuser")).thenReturn(Optional.of(gamification));

        streakService.updateStreak("testuser");

        verify(gamificationRepository, times(1)).save(argThat(g -> 
                g.getCurrentStreak() == 1 && 
                g.getLongestStreak() == 1 &&
                g.getEndStreak() != null
        ));
    }

    @Test
    void updateStreak_ShouldDoNothing_WhenUpdatedToday() {
        gamification.setCurrentStreak(5);
        gamification.setLongestStreak(5);
        gamification.setEndStreak(now); // Updated just now (today)
        when(gamificationRepository.findById("testuser")).thenReturn(Optional.of(gamification));

        streakService.updateStreak("testuser");

        verify(gamificationRepository, never()).save(any(Gamification.class));
    }

    @Test
    void updateStreak_ShouldIncrementStreak_WhenUpdatedYesterday() {
        gamification.setCurrentStreak(5);
        gamification.setLongestStreak(5);
        gamification.setEndStreak(now.minusDays(1)); // Updated yesterday
        when(gamificationRepository.findById("testuser")).thenReturn(Optional.of(gamification));

        streakService.updateStreak("testuser");

        verify(gamificationRepository, times(1)).save(argThat(g -> 
                g.getCurrentStreak() == 6 && 
                g.getLongestStreak() == 6
        ));
    }

    @Test
    void updateStreak_ShouldIncrementCurrentButNotLongest_WhenUpdatedYesterdayButLongestIsHigher() {
        gamification.setCurrentStreak(5);
        gamification.setLongestStreak(10);
        gamification.setEndStreak(now.minusDays(1)); // Updated yesterday
        when(gamificationRepository.findById("testuser")).thenReturn(Optional.of(gamification));

        streakService.updateStreak("testuser");

        verify(gamificationRepository, times(1)).save(argThat(g -> 
                g.getCurrentStreak() == 6 && 
                g.getLongestStreak() == 10
        ));
    }

    @Test
    void updateStreak_ShouldResetStreak_WhenUpdatedMoreThanOneDayAgo() {
        gamification.setCurrentStreak(5);
        gamification.setLongestStreak(10);
        gamification.setEndStreak(now.minusDays(2)); // Missed a day
        when(gamificationRepository.findById("testuser")).thenReturn(Optional.of(gamification));

        streakService.updateStreak("testuser");

        verify(gamificationRepository, times(1)).save(argThat(g -> 
                g.getCurrentStreak() == 1 && 
                g.getLongestStreak() == 10
        ));
    }
}
