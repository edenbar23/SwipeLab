package com.swipelab.gamification.application;

import com.swipelab.model.entity.User;
import com.swipelab.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PointsService {

    private final UserRepository userRepository;

    @Transactional
    public void addPoints(User user, int amount) {
        user.setPoints(user.getPoints() + amount);
        userRepository.save(user);
    }

    @Transactional
    public void calculateAndAddPoints(User user, int basePoints) {
        int streak = user.getCurrentStreak();
        double multiplier = 1.0;

        if (streak >= 30) {
            multiplier = 1.5; // +50% for 30+ days
        } else if (streak >= 14) {
            multiplier = 1.25; // +25% for 14+ days
        } else if (streak >= 7) {
            multiplier = 1.1; // +10% for 7+ days
        }

        int finalPoints = (int) Math.round(basePoints * multiplier);
        addPoints(user, finalPoints);
    }
}
