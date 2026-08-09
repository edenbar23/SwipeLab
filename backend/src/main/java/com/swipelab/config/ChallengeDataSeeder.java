package com.swipelab.config;

import com.swipelab.gamification.domain.badge.BadgeDefinition;
import com.swipelab.gamification.infrastructure.badge.BadgeDefinitionRepository;
import com.swipelab.gamification.domain.challenge.AggregationType;
import com.swipelab.gamification.domain.challenge.ChallengeDefinition;
import com.swipelab.gamification.infrastructure.challenge.ChallengeDefinitionRepository;
import com.swipelab.gamification.domain.challenge.MetricType;
import com.swipelab.gamification.domain.challenge.TimeWindowType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Seeds the database with default Badge and Challenge definitions.
 * This runs across all profiles (including production) to ensure
 * the baseline gamification targets exist in the database.
 *
 * Runs first (Order 0) so badge definitions exist before the profile seeders
 * (E2eDataSeeder/MockDataSeeder, Order 1) award them to seeded users.
 */
@Slf4j
@RequiredArgsConstructor
@Component
@Order(0)
public class ChallengeDataSeeder implements CommandLineRunner {

    private final BadgeDefinitionRepository badgeDefinitionRepository;
    private final ChallengeDefinitionRepository challengeDefinitionRepository;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("🌱 Checking Gamification Challenge and Badge Definitions...");
        seedChallenges();
    }

    private void seedChallenges() {
        if (badgeDefinitionRepository.count() == 0) {
            // ── Badge definitions (the 6 LabSwiper badges shown on the Profile screen) ──
            // iconUrl filenames match the frontend asset map in app/constants/badgeIcons.ts.
            BadgeDefinition firstSwipeBadge = BadgeDefinition.builder()
                    .title("First Swipe")
                    .code("FIRST_SWIPE")
                    .description("Awarded for completing your first classification")
                    .iconUrl("badge_first_swipe.png")
                    .build();

            BadgeDefinition tenSwipesBadge = BadgeDefinition.builder()
                    .title("10 Swipes")
                    .code("SWIPES_10")
                    .description("Awarded for completing 10 classifications")
                    .iconUrl("badge_10_swipes.png")
                    .build();

            BadgeDefinition hundredSwipesBadge = BadgeDefinition.builder()
                    .title("100 Swipes")
                    .code("SWIPES_100")
                    .description("Awarded for completing 100 classifications")
                    .iconUrl("badge_100_swipes.png")
                    .build();

            BadgeDefinition streak3Badge = BadgeDefinition.builder()
                    .title("3 Day Streak")
                    .code("STREAK_3")
                    .description("Awarded for maintaining a 3-day streak")
                    .iconUrl("badge_streak_3.png")
                    .build();

            BadgeDefinition streak7Badge = BadgeDefinition.builder()
                    .title("7 Day Streak")
                    .code("STREAK_7")
                    .description("Awarded for maintaining a 7-day streak")
                    .iconUrl("badge_streak_7.png")
                    .build();

            BadgeDefinition points1000Badge = BadgeDefinition.builder()
                    .title("1000 Points")
                    .code("POINTS_1000")
                    .description("Awarded for earning 1000 points")
                    .iconUrl("badge_points_1000.png")
                    .build();

            badgeDefinitionRepository.saveAll(List.of(
                    firstSwipeBadge, tenSwipesBadge, hundredSwipesBadge,
                    streak3Badge, streak7Badge, points1000Badge));

            if (challengeDefinitionRepository.count() == 0) {
                // ── Challenges that award each badge ──
                // Swipe badges accumulate classifications (COUNT). Streak/points badges report
                // an absolute current value each time (LATEST), so progress is set, not summed.
                ChallengeDefinition firstSwipeChallenge = ChallengeDefinition.builder()
                        .name("Classify 1 image")
                        .description("Classify your very first image")
                        .metricType(MetricType.CLASSIFICATION)
                        .aggregationType(AggregationType.COUNT)
                        .targetValue(1)
                        .timeWindowType(TimeWindowType.LIFETIME)
                        .badgeId(firstSwipeBadge.getId())
                        .active(true)
                        .build();

                ChallengeDefinition tenSwipesChallenge = ChallengeDefinition.builder()
                        .name("Classify 10 images")
                        .description("Complete 10 classifications")
                        .metricType(MetricType.CLASSIFICATION)
                        .aggregationType(AggregationType.COUNT)
                        .targetValue(10)
                        .timeWindowType(TimeWindowType.LIFETIME)
                        .badgeId(tenSwipesBadge.getId())
                        .active(true)
                        .build();

                ChallengeDefinition hundredSwipesChallenge = ChallengeDefinition.builder()
                        .name("Classify 100 images")
                        .description("Complete 100 classifications")
                        .metricType(MetricType.CLASSIFICATION)
                        .aggregationType(AggregationType.COUNT)
                        .targetValue(100)
                        .timeWindowType(TimeWindowType.LIFETIME)
                        .badgeId(hundredSwipesBadge.getId())
                        .active(true)
                        .build();

                ChallengeDefinition streak3Challenge = ChallengeDefinition.builder()
                        .name("Maintain a 3-day streak")
                        .description("Classify on 3 consecutive days")
                        .metricType(MetricType.STREAK)
                        .aggregationType(AggregationType.LATEST)
                        .targetValue(3)
                        .timeWindowType(TimeWindowType.LIFETIME)
                        .badgeId(streak3Badge.getId())
                        .active(true)
                        .build();

                ChallengeDefinition streak7Challenge = ChallengeDefinition.builder()
                        .name("Maintain a 7-day streak")
                        .description("Classify on 7 consecutive days")
                        .metricType(MetricType.STREAK)
                        .aggregationType(AggregationType.LATEST)
                        .targetValue(7)
                        .timeWindowType(TimeWindowType.LIFETIME)
                        .badgeId(streak7Badge.getId())
                        .active(true)
                        .build();

                ChallengeDefinition points1000Challenge = ChallengeDefinition.builder()
                        .name("Earn 1000 points")
                        .description("Reach 1000 total points")
                        .metricType(MetricType.XP_GAINED)
                        .aggregationType(AggregationType.LATEST)
                        .targetValue(1000)
                        .timeWindowType(TimeWindowType.LIFETIME)
                        .badgeId(points1000Badge.getId())
                        .active(true)
                        .build();

                challengeDefinitionRepository.saveAll(List.of(
                        firstSwipeChallenge, tenSwipesChallenge, hundredSwipesChallenge,
                        streak3Challenge, streak7Challenge, points1000Challenge));
                log.info("✅ Seeded Initial Challenges and Badge Definitions.");
            }
        } else {
            log.info("✅ Challenges and Badges already exist, skipping seed.");
        }
    }
}

