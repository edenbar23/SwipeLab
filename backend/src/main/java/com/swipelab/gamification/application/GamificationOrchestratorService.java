package com.swipelab.gamification.application;

import com.swipelab.classification.domain.core.Classification.UserResponse;
import com.swipelab.classification.events.ClassificationSubmittedEvent;
import com.swipelab.gamification.challenge.ChallengeEngine;
import com.swipelab.gamification.challenge.MetricType;
import com.swipelab.gamification.domain.BadgeService;
import com.swipelab.gamification.domain.Gamification;
import com.swipelab.gamification.domain.PointsService;
import com.swipelab.gamification.domain.RankService;
import com.swipelab.gamification.domain.RankTier;
import com.swipelab.gamification.domain.StreakService;
import com.swipelab.gamification.infrastructure.GamificationRepository;
import com.swipelab.users.domain.User;
import com.swipelab.users.infrastructure.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class GamificationOrchestratorService {

    private final StreakService streakService;
    private final PointsService pointsService;
    private final BadgeService badgeService;
    private final RankService rankService;
    private final GamificationRepository gamificationRepository;
    private final UserRepository userRepository;
    private final ChallengeEngine challengeEngine;

    @Async
    @EventListener
    @Transactional
    public void onClassificationSubmitted(ClassificationSubmittedEvent event) {
        log.info("Processing gamification for classification submission: {}", event);

        String username = event.getUsername();

        // 1. Update Streak
        streakService.updateStreak(username);

        // 2. Award Points (10 base, 50 bonus for correct gold)
        pointsService.calculateAndAddPoints(username, 10);
        if (event.isGoldStandard() && event.isCorrect()) {
            pointsService.calculateAndAddPoints(username, 50);
        }

        // 3. Check for Badges
        User user = userRepository.findByUsername(username).orElse(null);
        if (user != null) {
            badgeService.checkForBadges(username, user.getTotalClassifications());
        }

        // 4. Update YES tag count and recompute rank (only for YES responses)
        if (event.getUserResponse() == UserResponse.YES) {
            updateYesTagCountAndRank(username);
        }

        // 5. Feed the challenge engine the absolute streak & points so the streak/points
        //    badges can be awarded (LATEST metrics). CLASSIFICATION challenges are driven
        //    separately by ChallengeEventListener, so they are not reported here.
        gamificationRepository.findById(username).ifPresent(g -> {
            int currentStreak = g.getCurrentStreak() != null ? g.getCurrentStreak() : 0;
            long score = g.getScore() != null ? g.getScore() : 0L;
            challengeEngine.processAction(username, MetricType.STREAK, currentStreak, null);
            challengeEngine.processAction(username, MetricType.XP_GAINED, (int) Math.min(score, Integer.MAX_VALUE), null);
        });
    }

    private void updateYesTagCountAndRank(String username) {
        Gamification gamification = gamificationRepository.findById(username)
                .orElse(Gamification.builder()
                        .username(username)
                        .yesTagCount(0)
                        .score(0L)
                        .currentStreak(0)
                        .longestStreak(0)
                        .rank(RankTier.UNRANKED.name())
                        .build());

        int newCount = (gamification.getYesTagCount() == null ? 0 : gamification.getYesTagCount()) + 1;
        gamification.setYesTagCount(newCount);

        RankTier newTier = rankService.computeRank(newCount);
        gamification.setRank(newTier.name());

        gamificationRepository.save(gamification);
        log.info("User {} yes_tag_count={} rank={}", username, newCount, newTier.name());
    }
}

