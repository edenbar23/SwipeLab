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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PointsServiceTest {

    @Mock
    private GamificationRepository gamificationRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private PointsService pointsService;

    private Gamification gamification;

    @BeforeEach
    void setUp() {
        gamification = new Gamification();
        gamification.setUsername("testuser");
        gamification.setScore(100L);
        gamification.setCurrentStreak(0);
    }

    @Test
    void addPoints_ShouldAddAmountToScoreAndSave() {
        when(gamificationRepository.findById("testuser")).thenReturn(Optional.of(gamification));

        pointsService.addPoints("testuser", 50);

        verify(gamificationRepository, times(1)).save(argThat(g -> g.getScore() == 150L));
        verify(kafkaTemplate, times(1)).send(eq("gamification-events"), eq("testuser"), any(GamificationUpdatedEvent.class));
    }
    
    @Test
    void addPoints_ShouldCreateNewGamification_WhenNotFound() {
        when(gamificationRepository.findById("newuser")).thenReturn(Optional.empty());

        pointsService.addPoints("newuser", 10);

        verify(gamificationRepository, times(1)).save(argThat(g -> g.getScore() == 10L && g.getUsername().equals("newuser")));
    }

    @Test
    void calculateAndAddPoints_ShouldAddBasePoints_WhenStreakIsLow() {
        gamification.setCurrentStreak(5);
        when(gamificationRepository.findById("testuser")).thenReturn(Optional.of(gamification));

        pointsService.calculateAndAddPoints("testuser", 100);

        verify(gamificationRepository, times(1)).save(argThat(g -> g.getScore() == 200L)); // 100 + 100*1.0
    }

    @Test
    void calculateAndAddPoints_ShouldAddWith10PercentMultiplier_WhenStreakIs7() {
        gamification.setCurrentStreak(7);
        when(gamificationRepository.findById("testuser")).thenReturn(Optional.of(gamification));

        pointsService.calculateAndAddPoints("testuser", 100);

        verify(gamificationRepository, times(1)).save(argThat(g -> g.getScore() == 210L)); // 100 + 100*1.1
    }

    @Test
    void calculateAndAddPoints_ShouldAddWith25PercentMultiplier_WhenStreakIs14() {
        gamification.setCurrentStreak(15);
        when(gamificationRepository.findById("testuser")).thenReturn(Optional.of(gamification));

        pointsService.calculateAndAddPoints("testuser", 100);

        verify(gamificationRepository, times(1)).save(argThat(g -> g.getScore() == 225L)); // 100 + 100*1.25
    }

    @Test
    void calculateAndAddPoints_ShouldAddWith50PercentMultiplier_WhenStreakIs30() {
        gamification.setCurrentStreak(35);
        when(gamificationRepository.findById("testuser")).thenReturn(Optional.of(gamification));

        pointsService.calculateAndAddPoints("testuser", 100);

        verify(gamificationRepository, times(1)).save(argThat(g -> g.getScore() == 250L)); // 100 + 100*1.5
    }
}
