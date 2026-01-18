package com.swipelab.gamification.domain;

import com.swipelab.gamification.infrastructure.GamificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PointsService {

    private final GamificationRepository gamificationRepository;

    @Transactional
    public void addPoints(String username, int amount) {
        Gamification gamification = gamificationRepository.findById(username)
                .orElse(Gamification.builder()
                        .username(username)
                        .currentStreak(0)
                        .longestStreak(0)
                        .score(0L)
                        .build());

        gamification.setScore(gamification.getScore() + amount);
        gamificationRepository.save(gamification);
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
    }
}
