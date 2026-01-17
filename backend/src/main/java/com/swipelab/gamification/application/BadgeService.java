package com.swipelab.gamification.application;

import com.swipelab.gamification.domain.Badge;
import com.swipelab.model.entity.User;
import com.swipelab.gamification.infrastructure.BadgeRepository;
import com.swipelab.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BadgeService {

    private final BadgeRepository badgeRepository;
    private final UserRepository userRepository;

    private static final String BADGE_FIRST_SWIPE = "First Swipe";
    private static final String BADGE_10_SWIPES = "10 Swipes";
    private static final String BADGE_100_SWIPES = "100 Swipes";

    // Streak Badges
    private static final String BADGE_3_DAY_STREAK = "3 Day Streak";
    private static final String BADGE_7_DAY_STREAK = "7 Day Streak";

    // Point Badges
    private static final String BADGE_1000_POINTS = "1000 Points";

    @Transactional
    public void checkForBadges(User user) {
        int totalSwipes = user.getTotalClassifications();
        int streak = user.getCurrentStreak();
        long points = user.getPoints();

        // Swipe Count Badges
        if (totalSwipes == 1) {
            awardBadge(user, BADGE_FIRST_SWIPE);
        } else if (totalSwipes == 10) {
            awardBadge(user, BADGE_10_SWIPES);
        } else if (totalSwipes == 100) {
            awardBadge(user, BADGE_100_SWIPES);
        }

        // Streak Badges
        if (streak >= 3) {
            awardBadge(user, BADGE_3_DAY_STREAK);
        }
        if (streak >= 7) {
            awardBadge(user, BADGE_7_DAY_STREAK);
        }

        // Point Badges
        if (points >= 1000) {
            awardBadge(user, BADGE_1000_POINTS);
        }
    }

    private void awardBadge(User user, String badgeName) {
        throw new UnsupportedOperationException("Unimplemented method 'awardBadge'");
        // Need to be updated throw user event
    }
}
