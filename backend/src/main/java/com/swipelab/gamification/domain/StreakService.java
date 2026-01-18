package com.swipelab.gamification.domain;

import com.swipelab.gamification.infrastructure.GamificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class StreakService {

    private final GamificationRepository gamificationRepository;

    @Transactional
    public void updateStreak(String username) {
        LocalDateTime now = LocalDateTime.now();
        Gamification gamification = gamificationRepository.findById(username)
                .orElse(Gamification.builder()
                        .username(username)
                        .currentStreak(0) // Will be set to 1 below
                        .longestStreak(0)
                        .score(0L)
                        .build());

        LocalDateTime lastUpdate = gamification.getEndStreak();

        // If never updated
        if (lastUpdate == null) {
            startNewStreak(gamification, now);
            gamificationRepository.save(gamification);
            return;
        }

        LocalDate today = now.toLocalDate();
        LocalDate lastUpdateDate = lastUpdate.toLocalDate();

        // If updated today, do nothing
        if (today.equals(lastUpdateDate)) {
            return;
        }

        // If updated yesterday, increment streak
        if (lastUpdateDate.equals(today.minusDays(1))) {
            gamification.setCurrentStreak(gamification.getCurrentStreak() + 1);
            gamification.setEndStreak(now);

            // Update longest streak if needed
            if (gamification.getCurrentStreak() > gamification.getLongestStreak()) {
                gamification.setLongestStreak(gamification.getCurrentStreak());
            }
        } else {
            // Missed a day (or more), reset to 1
            startNewStreak(gamification, now);
        }

        gamificationRepository.save(gamification);
    }

    private void startNewStreak(Gamification gamification, LocalDateTime now) {
        gamification.setCurrentStreak(1);
        gamification.setStartStreak(now);
        gamification.setEndStreak(now);
        if (gamification.getLongestStreak() == 0) {
            gamification.setLongestStreak(1);
        }
    }
}
