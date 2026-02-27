package com.swipelab.gamification.domain;

import com.swipelab.gamification.events.GamificationUpdatedEvent;
import com.swipelab.gamification.infrastructure.GamificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BadgeServiceTest {

    @Mock
    private GamificationRepository gamificationRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private BadgeService badgeService;

    private Gamification gamification;

    @BeforeEach
    void setUp() {
        gamification = new Gamification();
        gamification.setUsername("testuser");
        gamification.setCurrentStreak(0);
        gamification.setScore(0L);
        gamification.setBadge("");
    }

    @Test
    void checkForBadges_ShouldAwardFirstSwipeBadge() {
        when(gamificationRepository.findById("testuser")).thenReturn(Optional.of(gamification));

        badgeService.checkForBadges("testuser", 1);

        verify(gamificationRepository, times(1)).save(argThat(g -> g.getBadge().contains("First Swipe")));
        verify(kafkaTemplate, times(1)).send(eq("gamification-events"), eq("testuser"), any(GamificationUpdatedEvent.class));
    }

    @Test
    void checkForBadges_ShouldAward10SwipesBadge() {
        when(gamificationRepository.findById("testuser")).thenReturn(Optional.of(gamification));

        badgeService.checkForBadges("testuser", 10);

        verify(gamificationRepository, times(1)).save(argThat(g -> g.getBadge().contains("10 Swipes")));
    }

    @Test
    void checkForBadges_ShouldAward100SwipesBadge() {
        when(gamificationRepository.findById("testuser")).thenReturn(Optional.of(gamification));

        badgeService.checkForBadges("testuser", 100);

        verify(gamificationRepository, times(1)).save(argThat(g -> g.getBadge().contains("100 Swipes")));
    }

    @Test
    void checkForBadges_ShouldAwardStreakBadges() {
        gamification.setCurrentStreak(3);
        when(gamificationRepository.findById("testuser")).thenReturn(Optional.of(gamification));

        badgeService.checkForBadges("testuser", 5);

        verify(gamificationRepository, times(1)).save(argThat(g -> g.getBadge().contains("3 Day Streak")));
    }

    @Test
    void checkForBadges_ShouldAwardPointsBadge() {
        gamification.setScore(1000L);
        when(gamificationRepository.findById("testuser")).thenReturn(Optional.of(gamification));

        badgeService.checkForBadges("testuser", 5);

        verify(gamificationRepository, times(1)).save(argThat(g -> g.getBadge().contains("1000 Points")));
    }

    @Test
    void checkForBadges_ShouldNotAwardDuplicateBadge() {
        gamification.setBadge("First Swipe");
        when(gamificationRepository.findById("testuser")).thenReturn(Optional.of(gamification));

        badgeService.checkForBadges("testuser", 1);

        verify(gamificationRepository, times(1)).save(argThat(g -> g.getBadge().equals("First Swipe")));
    }
}
