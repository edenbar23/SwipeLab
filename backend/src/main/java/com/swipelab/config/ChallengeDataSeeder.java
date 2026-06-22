package com.swipelab.config;

import com.swipelab.gamification.badge.BadgeDefinition;
import com.swipelab.gamification.badge.BadgeDefinitionRepository;
import com.swipelab.gamification.challenge.AggregationType;
import com.swipelab.gamification.challenge.ChallengeDefinition;
import com.swipelab.gamification.challenge.ChallengeDefinitionRepository;
import com.swipelab.gamification.challenge.MetricType;
import com.swipelab.gamification.challenge.TimeWindowType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Seeds the database with default Badge and Challenge definitions.
 * This runs across all profiles (including production) to ensure
 * the baseline gamification targets exist in the database.
 */
@Slf4j
@RequiredArgsConstructor
@Component
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
            BadgeDefinition legendBadge = BadgeDefinition.builder()
                    .title("LabSwiper Legend Badge")
                    .code("LEGEND_500")
                    .description("Reach 500 total classifications")
                    .iconUrl("/badges/legend.png")
                    .build();

            BadgeDefinition silverBadge = BadgeDefinition.builder()
                    .title("Silver Badge")
                    .code("SILVER_DAILY")
                    .description("Classify 20 images today")
                    .iconUrl("/badges/silver.png")
                    .build();

            BadgeDefinition firstSwipeBadge = BadgeDefinition.builder()
                    .title("First Swipe")
                    .code("FIRST_SWIPE")
                    .description("Classify your very first image")
                    .iconUrl("/badges/first_swipe.png")
                    .build();

            badgeDefinitionRepository.saveAll(List.of(legendBadge, silverBadge, firstSwipeBadge));

            if (challengeDefinitionRepository.count() == 0) {
                ChallengeDefinition legendChallenge = ChallengeDefinition.builder()
                        .name("Reach 500 total classifications")
                        .description("Lifetime challenge for classifications")
                        .metricType(MetricType.CLASSIFICATION)
                        .aggregationType(AggregationType.COUNT)
                        .targetValue(500)
                        .timeWindowType(TimeWindowType.LIFETIME)
                        .badgeId(legendBadge.getId())
                        .active(true)
                        .build();

                ChallengeDefinition dailyChallenge = ChallengeDefinition.builder()
                        .name("Classify 20 images today")
                        .description("Daily challenge for classifications")
                        .metricType(MetricType.CLASSIFICATION)
                        .aggregationType(AggregationType.COUNT)
                        .targetValue(20)
                        .timeWindowType(TimeWindowType.DAILY)
                        .badgeId(silverBadge.getId())
                        .active(true)
                        .build();

                ChallengeDefinition firstSwipeChallenge = ChallengeDefinition.builder()
                        .name("Classify 1 image")
                        .description("Classify your very first image to earn a badge quickly")
                        .metricType(MetricType.CLASSIFICATION)
                        .aggregationType(AggregationType.COUNT)
                        .targetValue(1)
                        .timeWindowType(TimeWindowType.LIFETIME)
                        .badgeId(firstSwipeBadge.getId())
                        .active(true)
                        .build();

                challengeDefinitionRepository.saveAll(List.of(legendChallenge, dailyChallenge, firstSwipeChallenge));
                log.info("✅ Seeded Initial Challenges and Badge Definitions.");
            }
        } else {
            log.info("✅ Challenges and Badges already exist, skipping seed.");
        }
    }
}
