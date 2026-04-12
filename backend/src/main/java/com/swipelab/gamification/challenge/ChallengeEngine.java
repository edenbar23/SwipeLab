package com.swipelab.gamification.challenge;

import com.swipelab.gamification.badge.RewardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChallengeEngine {

    private final ChallengeDefinitionRepository challengeDefinitionRepository;
    private final UserChallengeRepository userChallengeRepository;
    private final RewardService rewardService;

    @Transactional
    public void processAction(String username, MetricType metricType, int amount, String distinctValue) {
        LocalDateTime now = LocalDateTime.now();
        
        List<ChallengeDefinition> activeChallenges = challengeDefinitionRepository
                .findActiveChallenges(now).stream()
                .filter(c -> c.getMetricType() == metricType)
                .toList();

        for (ChallengeDefinition definition : activeChallenges) {
            LocalDateTime[] window = calculateWindow(definition.getTimeWindowType(), now);
            LocalDateTime windowStart = window[0];
            LocalDateTime windowEnd = window[1];

            UserChallenge userChallenge = userChallengeRepository
                    .findByUsernameAndDefinitionIdAndWindowStart(username, definition.getId(), windowStart)
                    .orElseGet(() -> UserChallenge.builder()
                            .username(username)
                            .definition(definition)
                            .currentProgress(0)
                            .completed(false)
                            .windowStart(windowStart)
                            .windowEnd(windowEnd)
                            .build());

            if (!userChallenge.isCompleted()) {
                // Determine new progress
                if (definition.getAggregationType() == AggregationType.COUNT || 
                    definition.getAggregationType() == AggregationType.SUM) {
                    userChallenge.setCurrentProgress(userChallenge.getCurrentProgress() + amount);
                }
                // Distinct Count would involve saving the DistinctValue to UserChallengeDistinct
                // We're skipping distinct logic complexity for MVP unless requested, defaulting to count.

                if (userChallenge.getCurrentProgress() >= definition.getTargetValue()) {
                    userChallenge.setCompleted(true);
                    rewardService.grantBadge(userChallenge);
                }

                userChallengeRepository.save(userChallenge);
            }
        }
    }

    private LocalDateTime[] calculateWindow(TimeWindowType type, LocalDateTime now) {
        LocalDateTime start;
        LocalDateTime end;
        LocalDate today = now.toLocalDate();

        switch (type) {
            case DAILY:
                start = today.atStartOfDay();
                end = today.atTime(LocalTime.MAX);
                break;
            case WEEKLY:
                LocalDate startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                start = startOfWeek.atStartOfDay();
                end = startOfWeek.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)).atTime(LocalTime.MAX);
                break;
            case MONTHLY:
                start = today.withDayOfMonth(1).atStartOfDay();
                end = today.with(TemporalAdjusters.lastDayOfMonth()).atTime(LocalTime.MAX);
                break;
            case CUSTOM:
            case LIFETIME:
            default:
                // For lifetime, we can use an epoch start
                start = LocalDateTime.of(2000, 1, 1, 0, 0);
                end = LocalDateTime.of(2100, 1, 1, 0, 0);
                break;
        }
        return new LocalDateTime[]{start, end};
    }
}
