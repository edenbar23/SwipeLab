package com.swipelab.gamification.domain;

import com.swipelab.gamification.infrastructure.GamificationRepository;
import com.swipelab.gamification.events.GamificationUpdatedEvent;
import org.springframework.kafka.core.KafkaTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class BadgeService {

    private final GamificationRepository gamificationRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String BADGE_FIRST_SWIPE = "First Swipe";
    private static final String BADGE_10_SWIPES = "10 Swipes";
    private static final String BADGE_100_SWIPES = "100 Swipes";

    // Streak Badges
    private static final String BADGE_3_DAY_STREAK = "3 Day Streak";
    private static final String BADGE_7_DAY_STREAK = "7 Day Streak";

    // Point Badges
    private static final String BADGE_1000_POINTS = "1000 Points";

    @Transactional
    public void checkForBadges(String username, int totalSwipes) {
        Gamification gamification = gamificationRepository.findById(username)
                .orElse(Gamification.builder()
                        .username(username)
                        .currentStreak(0)
                        .longestStreak(0)
                        .score(0L)
                        .badge("")
                        .build());

        int streak = gamification.getCurrentStreak();
        long points = gamification.getScore();

        // Swipe Count Badges
        if (totalSwipes == 1) {
            awardBadge(gamification, BADGE_FIRST_SWIPE);
        } else if (totalSwipes == 10) {
            awardBadge(gamification, BADGE_10_SWIPES);
        } else if (totalSwipes == 100) {
            awardBadge(gamification, BADGE_100_SWIPES);
        }

        // Streak Badges
        if (streak >= 3) {
            awardBadge(gamification, BADGE_3_DAY_STREAK);
        }
        if (streak >= 7) {
            awardBadge(gamification, BADGE_7_DAY_STREAK);
        }

        // Point Badges
        if (points >= 1000) {
            awardBadge(gamification, BADGE_1000_POINTS);
        }

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

    private void awardBadge(Gamification gamification, String badgeName) {
        String currentBadges = gamification.getBadge();
        if (currentBadges == null) {
            currentBadges = "";
        }

        // Check if already has badge
        Set<String> badgeSet = new HashSet<>();
        if (!currentBadges.isEmpty()) {
            badgeSet.addAll(Arrays.asList(currentBadges.split(",")));
        }

        if (!badgeSet.contains(badgeName)) {
            if (!currentBadges.isEmpty()) {
                gamification.setBadge(currentBadges + "," + badgeName);
            } else {
                gamification.setBadge(badgeName);
            }
            log.info("Awarded badge '{}' to user {}", badgeName, gamification.getUsername());
        }
    }
}
