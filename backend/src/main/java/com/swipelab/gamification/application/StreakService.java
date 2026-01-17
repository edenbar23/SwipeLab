package com.swipelab.gamification.application;

import com.swipelab.model.entity.User;
import com.swipelab.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class StreakService {

    private final UserRepository userRepository;

    @Transactional
    public void updateStreak(User user) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime lastUpdate = user.getLastStreakUpdate();

        // If never updated, initialize streak to 1
        if (lastUpdate == null) {
            user.setCurrentStreak(1);
            user.setLastStreakUpdate(now);
            userRepository.save(user);
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
            user.setCurrentStreak(user.getCurrentStreak() + 1);
        } else {
            // Missed a day (or more), reset to 1
            user.setCurrentStreak(1);
        }

        user.setLastStreakUpdate(now);
        userRepository.save(user);
    }
}
