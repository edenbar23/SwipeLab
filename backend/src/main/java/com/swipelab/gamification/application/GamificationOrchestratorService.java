package com.swipelab.gamification.application;

import com.swipelab.classification.events.ClassificationSubmittedEvent;
import com.swipelab.gamification.domain.BadgeService;
import com.swipelab.gamification.domain.PointsService;
import com.swipelab.gamification.domain.StreakService;
import com.swipelab.users.domain.User;
import com.swipelab.users.infrastructure.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class GamificationOrchestratorService {

    private final StreakService streakService;
    private final PointsService pointsService;
    private final BadgeService badgeService;
    private final UserRepository userRepository;

    @KafkaListener(topics = "classification-events", groupId = "gamification-group")
    @Transactional
    public void onClassificationSubmitted(ClassificationSubmittedEvent event) {
        log.info("Processing gamification for classification submission: {}", event);

        String username = event.getUsername();

        // 1. Update Streak
        streakService.updateStreak(username);

        // 2. Award Points
        // Logic: 10 points per classification? Bonus for Gold?
        // Matches logic removed from ClassificationService: 10 base, 50 bonus for
        // correct gold.

        // Replicating previous logic:
        // Always 10 base points (with multiplier checking inside calculateAndAddPoints)
        pointsService.calculateAndAddPoints(username, 10);

        if (event.isGoldStandard() && event.isCorrect()) {
            pointsService.calculateAndAddPoints(username, 50);
        }

        // 3. Check for Badges
        // Need totalSwipes.
        // We can fetch from User or trust the event if we added it. Event doesn't have
        // it.
        // Fetching user to get totalClassifications.
        // Note: totalClassifications is updated in ClassificationService BEFORE event
        // likely.

        User user = userRepository.findByUsername(username).orElse(null);
        if (user != null) {
            badgeService.checkForBadges(username, user.getTotalClassifications());
        }
    }
}
