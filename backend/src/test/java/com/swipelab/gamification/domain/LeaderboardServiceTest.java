package com.swipelab.gamification.domain;

import com.swipelab.gamification.infrastructure.GamificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeaderboardServiceTest {

    @Mock
    private GamificationRepository gamificationRepository;

    @InjectMocks
    private LeaderboardService leaderboardService;

    private Gamification gamification1;
    private Gamification gamification2;

    @BeforeEach
    void setUp() {
        gamification1 = new Gamification();
        gamification1.setUsername("user1");
        gamification1.setScore(100L);
        gamification1.setCurrentStreak(5);

        gamification2 = new Gamification();
        gamification2.setUsername("user2");
        gamification2.setScore(200L);
        gamification2.setCurrentStreak(10);
    }

    @Test
    void getGlobalLeaderboard_ShouldReturnSortedList() {
        Page<Gamification> page = new PageImpl<>(Arrays.asList(gamification2, gamification1));
        when(gamificationRepository.findAll(any(PageRequest.class))).thenReturn(page);

        List<Gamification> result = leaderboardService.getGlobalLeaderboard(10);

        assertEquals(2, result.size());
        assertEquals("user2", result.get(0).getUsername());
    }

    @Test
    void getStreakLeaderboard_ShouldReturnSortedList() {
        Page<Gamification> page = new PageImpl<>(Arrays.asList(gamification2, gamification1));
        when(gamificationRepository.findAll(any(PageRequest.class))).thenReturn(page);

        List<Gamification> result = leaderboardService.getStreakLeaderboard(10);

        assertEquals(2, result.size());
        assertEquals("user2", result.get(0).getUsername());
    }

    @Test
    void getGamification_ShouldReturnExistingGamification() {
        when(gamificationRepository.findById("user1")).thenReturn(Optional.of(gamification1));

        Gamification result = leaderboardService.getGamification("user1");

        assertNotNull(result);
        assertEquals("user1", result.getUsername());
        assertEquals(100L, result.getScore());
    }

    @Test
    void getGamification_ShouldReturnNewGamification_WhenNotFound() {
        when(gamificationRepository.findById("nonexist")).thenReturn(Optional.empty());

        Gamification result = leaderboardService.getGamification("nonexist");

        assertNotNull(result);
        assertEquals("nonexist", result.getUsername());
        assertEquals(0L, result.getScore());
    }
}
