package com.swipelab.gamification.domain;

import com.swipelab.gamification.infrastructure.GamificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.swipelab.gamification.events.GamificationUpdatedEvent;
import org.springframework.kafka.core.KafkaTemplate;

@Service
@RequiredArgsConstructor
public class PointsService {

    private final GamificationRepository gamificationRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Transactional
    public void addPoints(String username, int amount) {
        Gamification gamification = gamificationRepository.findById(username)
                .orElse(Gamification.builder()
                        .username(username)
                        .currentStreak(0)
                        .longestStreak(0)
                        .score(0L)
                        .rank("UNRANKED")
                        .build());

        gamification.setScore(gamification.getScore() + amount);
        gamificationRepository.save(gamification);
        publishGamificationUpdate(gamification);
    }

    @Transactional
    public void calculateAndAddPoints(String username, int basePoints) {
        Gamification gamification = gamificationRepository.findById(username)
                .orElse(Gamification.builder()
                        .username(username)
                        .currentStreak(0)
                        .longestStreak(0)
                        .score(0L)
                        .build());

        int streak = gamification.getCurrentStreak();
        double multiplier = 1.0;

        if (streak >= 30) {
            multiplier = 1.5; // +50% for 30+ days
        } else if (streak >= 14) {
            multiplier = 1.25; // +25% for 14+ days
        } else if (streak >= 7) {
            multiplier = 1.1; // +10% for 7+ days
        }

        int finalPoints = (int) Math.round(basePoints * multiplier);
        gamification.setScore(gamification.getScore() + finalPoints);
        gamificationRepository.save(gamification);
        publishGamificationUpdate(gamification);
    }

    private void publishGamificationUpdate(Gamification gamification) {
        GamificationUpdatedEvent event = GamificationUpdatedEvent.builder()
                .username(gamification.getUsername())
                .score(gamification.getScore())
                .badges(gamification.getBadge())
                .rank(gamification.getRank())
                .build();
        kafkaTemplate.send("gamification-events", gamification.getUsername(), event);
    }
}
