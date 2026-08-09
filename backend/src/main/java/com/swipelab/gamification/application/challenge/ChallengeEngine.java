package com.swipelab.gamification.application.challenge;

import com.swipelab.gamification.domain.challenge.*;
import com.swipelab.gamification.infrastructure.challenge.*;
import com.swipelab.gamification.application.badge.RewardService;
import com.swipelab.gamification.domain.challenge.*;
import com.swipelab.gamification.infrastructure.challenge.*;
import lombok.RequiredArgsConstructor;
import com.swipelab.gamification.domain.challenge.*;
import com.swipelab.gamification.infrastructure.challenge.*;
import lombok.extern.slf4j.Slf4j;
import com.swipelab.gamification.domain.challenge.*;
import com.swipelab.gamification.infrastructure.challenge.*;
import org.springframework.stereotype.Service;
import com.swipelab.gamification.domain.challenge.*;
import com.swipelab.gamification.infrastructure.challenge.*;
import org.springframework.transaction.annotation.Transactional;

import com.swipelab.gamification.domain.challenge.*;
import com.swipelab.gamification.infrastructure.challenge.*;
import java.time.DayOfWeek;
import com.swipelab.gamification.domain.challenge.*;
import com.swipelab.gamification.infrastructure.challenge.*;
import java.time.LocalDate;
import com.swipelab.gamification.domain.challenge.*;
import com.swipelab.gamification.infrastructure.challenge.*;
import java.time.LocalDateTime;
import com.swipelab.gamification.domain.challenge.*;
import com.swipelab.gamification.infrastructure.challenge.*;
import java.time.LocalTime;
import com.swipelab.gamification.domain.challenge.*;
import com.swipelab.gamification.infrastructure.challenge.*;
import java.time.temporal.TemporalAdjusters;
import com.swipelab.gamification.domain.challenge.*;
import com.swipelab.gamification.infrastructure.challenge.*;
import java.util.List;
import com.swipelab.gamification.domain.challenge.*;
import com.swipelab.gamification.infrastructure.challenge.*;
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
                } else if (definition.getAggregationType() == AggregationType.LATEST) {
                    // Absolute metrics (e.g. current streak length, total points): each report
                    // carries the full current value, so set progress to it rather than accumulate.
                    userChallenge.setCurrentProgress(amount);
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



