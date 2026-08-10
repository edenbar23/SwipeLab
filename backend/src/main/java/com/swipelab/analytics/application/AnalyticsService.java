package com.swipelab.analytics.application;

import com.swipelab.analytics.domain.*;
import com.swipelab.analytics.dto.*;
import com.swipelab.analytics.infrastructure.*;
import com.swipelab.classification.domain.core.Classification.UserResponse;
import com.swipelab.classification.infrastructure.ClassificationRepository;
import com.swipelab.classification.infrastructure.ImageRepository;
import com.swipelab.config.CacheConfig;
import com.swipelab.analytics.dto.DashboardStatsResponse;
import com.swipelab.analytics.dto.UserPerformanceResponse;
import com.swipelab.tasks.domain.TaskStatus;
import com.swipelab.tasks.infrastructure.TaskRepository;
import com.swipelab.users.infrastructure.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final ClassificationFactRepository classificationFactRepository;
    private final ClassificationRepository classificationRepository;
    private final UserDailyStatsRepository userDailyStatsRepository;
    private final TaskDailyStatsRepository taskDailyStatsRepository;
    private final TaskSpeciesStatsRepository taskSpeciesStatsRepository;
    private final UserRankingRepository userRankingRepository;
    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final ImageRepository imageRepository;

    // ─── User-scoped endpoints ────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public UserProgressResponse getUserProgress(String userId) {
        LocalDate thirtyDaysAgo = LocalDate.now().minusDays(30);
        Object result = userDailyStatsRepository.getProgressSince(userId, thirtyDaysAgo);

        long total = 0;
        long correct = 0;

        if (result instanceof Object[]) {
            Object[] row = (Object[]) result;
            if (row.length >= 2) {
                total = row[0] != null ? ((Number) row[0]).longValue() : 0;
                correct = row[1] != null ? ((Number) row[1]).longValue() : 0;
            }
        }

        double accuracy = total > 0 ? (double) correct / total : 0.0;

        return UserProgressResponse.builder()
                .completed((int) total)
                .accuracy(accuracy)
                .build();
    }

    @Transactional(readOnly = true)
    public UserStatisticsResponse getUserStatistics(String userId) {
        Object totalStats = userDailyStatsRepository.getTotalStats(userId);

        long total = 0;
        long correct = 0;
        if (totalStats instanceof Object[]) {
            Object[] row = (Object[]) totalStats;
            if (row.length >= 2) {
                total = row[0] != null ? ((Number) row[0]).longValue() : 0;
                correct = row[1] != null ? ((Number) row[1]).longValue() : 0;
            }
        }
        double accuracy = total > 0 ? (double) correct / total : 0.0;

        UserRanking ranking = userRankingRepository.findByUserIdAndPeriod(userId, "ALL_TIME")
                .orElse(UserRanking.builder().rank(0).percentile(0).build());

        LocalDate thirtyDaysAgo = LocalDate.now().minusDays(30);
        List<UserDailyStats> dailyStats = userDailyStatsRepository.findByUserIdAndDayAfterOrderByDayAsc(userId,
                thirtyDaysAgo);

        List<UserStatisticsResponse.DayPoint> trend = dailyStats.stream()
                .map(ds -> UserStatisticsResponse.DayPoint.builder()
                        .date(ds.getDay().toString())
                        .accuracy(ds.getAccuracy())
                        .build())
                .collect(Collectors.toList());

        return UserStatisticsResponse.builder()
                .summary(UserStatisticsResponse.Summary.builder()
                        .totalClassifications((int) total)
                        .correctClassifications((int) correct)
                        .accuracy(accuracy)
                        .contributionPercentage(0.0)
                        .rank(UserStatisticsResponse.Rank.builder()
                                .allTime(ranking.getRank())
                                .daily(0)
                                .weekly(0)
                                .monthly(0)
                                .build())
                        .rankPercentile(ranking.getPercentile())
                        .currentStreak(0)
                        .longestStreak(0)
                        .build())
                .trend(UserStatisticsResponse.Trend.builder()
                        .byDay(trend)
                        .build())
                .build();
    }

    @Transactional(readOnly = true)
    public UserVsExpertsResponse getUserVsExperts(String userId) {
        Double userAcc = classificationFactRepository.getUserAccuracy(userId);
        Double expertAcc = classificationFactRepository.getGlobalExpertAccuracy();

        if (userAcc == null) userAcc = 0.0;
        if (expertAcc == null) expertAcc = 0.0;

        return UserVsExpertsResponse.builder()
                .user(UserVsExpertsResponse.Stat.builder().accuracy(userAcc).build())
                .experts(UserVsExpertsResponse.Stat.builder().accuracy(expertAcc).build())
                .difference(UserVsExpertsResponse.Stat.builder().accuracy(userAcc - expertAcc).build())
                .build();
    }

    @Transactional(readOnly = true)
    public UserVsUsersResponse getUserVsUsers(String userId) {
        UserRanking ranking = userRankingRepository.findByUserIdAndPeriod(userId, "ALL_TIME")
                .orElse(UserRanking.builder().rank(0).percentile(0).build());
        Double averageAccuracy = classificationFactRepository.getGlobalAverageAccuracy();
        if (averageAccuracy == null) averageAccuracy = 0.0;

        return UserVsUsersResponse.builder()
                .percentile((double) ranking.getPercentile())
                .averageUserAccuracy(averageAccuracy)
                .build();
    }


    @Transactional(readOnly = true)
    public PerformanceBreakdownResponse getPerformanceBreakdown(String userId) {
        List<Object[]> rows = classificationFactRepository.getSpeciesBreakdown(userId);

        List<PerformanceBreakdownResponse.CategoryStat> list = new ArrayList<>();
        for (Object[] row : rows) {
            String species = (String) row[0];
            long count = ((Number) row[1]).longValue();
            double acc = ((Number) row[2]).doubleValue();

            list.add(PerformanceBreakdownResponse.CategoryStat.builder()
                    .category(species)
                    .total((int) count)
                    .accuracy(acc)
                    .build());
        }

        return PerformanceBreakdownResponse.builder()
                .byCategory(list)
                .build();
    }

    @Transactional(readOnly = true)
    public TimeSeriesResponse getTimeSeries(String userId, String metric, String period) {
        LocalDate start = LocalDate.now().minusDays(30);
        List<UserDailyStats> stats = userDailyStatsRepository.findByUserIdAndDayAfterOrderByDayAsc(userId, start);

        List<TimeSeriesResponse.Point> points = stats.stream()
                .map(s -> TimeSeriesResponse.Point.builder()
                        .timestamp(s.getDay().toString())
                        .value(s.getAccuracy())
                        .build())
                .collect(Collectors.toList());

        return TimeSeriesResponse.builder()
                .metric(metric)
                .points(points)
                .build();
    }

    // ─── Task-scoped endpoints (Researcher) ──────────────────────────────────

    @Transactional(readOnly = true)
    public TaskAnalyticsResponse getTaskAnalytics(Long taskId) {
        Long completedImages = classificationFactRepository.countCompletedImages(taskId);
        List<ClassificationFact> facts = classificationFactRepository.findByTaskId(taskId);
        int totalClassifications = facts.size();

        int totalImages = (int) imageRepository.countByTaskId(taskId);
        double percentComplete = totalImages > 0
                ? (double) completedImages / totalImages * 100
                : 0.0;

        TaskAnalyticsResponse.Progress progress = TaskAnalyticsResponse.Progress.builder()
                .imagesClassified(completedImages.intValue())
                .totalImages(totalImages)
                .completedImages(completedImages.intValue())
                .percentComplete(percentComplete)
                .build();

        List<TaskSpeciesStats> speciesStats = taskSpeciesStatsRepository.findByTaskId(taskId);
        List<TaskAnalyticsResponse.SpeciesAnalytics> saList = speciesStats.stream()
                .map(s -> TaskAnalyticsResponse.SpeciesAnalytics.builder()
                        .name(s.getSpecies())
                        .classificationCount(s.getClassificationCount())
                        .agreementRate(s.getAgreementRate())
                        .confusionMatrix(TaskAnalyticsResponse.ConfusionMatrix.builder()
                                .truePositive(s.getTruePositive())
                                .falsePositive(s.getFalsePositive())
                                .trueNegative(s.getTrueNegative())
                                .falseNegative(s.getFalseNegative())
                                .build())
                        .build())
                .collect(Collectors.toList());

        // ─── Participation ──────────────────
        long activeUsers = facts.stream().map(ClassificationFact::getUserId).distinct().count();
        int avgClassifications = activeUsers > 0 ? (int) (totalClassifications / activeUsers) : 0;
        
        List<Long> responseTimes = facts.stream()
                .map(ClassificationFact::getResponseTimeMs)
                .filter(java.util.Objects::nonNull)
                .sorted()
                .collect(Collectors.toList());
        long medianResponseTime = 0L;
        if (!responseTimes.isEmpty()) {
            int mid = responseTimes.size() / 2;
            medianResponseTime = responseTimes.size() % 2 == 1 
                ? responseTimes.get(mid) 
                : (responseTimes.get(mid - 1) + responseTimes.get(mid)) / 2;
        }

        TaskAnalyticsResponse.Participation participation = TaskAnalyticsResponse.Participation.builder()
                .activeUsers((int) activeUsers)
                .totalClassifications(totalClassifications)
                .averageClassificationsPerUser(avgClassifications)
                .medianResponseTimeMs(medianResponseTime)
                .build();

        // ─── Quality ────────────────────────
        double sumCredibility = 0;
        int credibilityCount = 0;
        Map<String, List<Double>> userCredibility = new HashMap<>();
        for (ClassificationFact f : facts) {
            if (f.getCredibilityAtTime() != null) {
                sumCredibility += f.getCredibilityAtTime();
                credibilityCount++;
                userCredibility.computeIfAbsent(f.getUserId(), k -> new ArrayList<>()).add(f.getCredibilityAtTime());
            }
        }
        double avgCredibility = credibilityCount > 0 ? sumCredibility / credibilityCount : 0.0;
        
        int lowQualityCount = 0;
        for (List<Double> creds : userCredibility.values()) {
            double uAvg = creds.stream().mapToDouble(Double::doubleValue).average().orElse(100.0);
            if (uAvg < 50.0) {
                lowQualityCount++;
            }
        }

        TaskAnalyticsResponse.Quality quality = TaskAnalyticsResponse.Quality.builder()
                .averageCredibility(avgCredibility)
                .lowQualityUsers(lowQualityCount)
                .build();

        // ─── Consensus ──────────────────────
        Map<Long, Double> imageConsensus = new HashMap<>();
        for (ClassificationFact f : facts) {
            if (f.getConsensusScore() != null) {
                imageConsensus.merge(f.getImageId(), f.getConsensusScore(), Math::max);
            }
        }
        double sumConsensus = 0;
        int lowConsensusImages = 0;
        for (Double score : imageConsensus.values()) {
            sumConsensus += score;
            if (score < 0.8) {
                lowConsensusImages++;
            }
        }
        double avgConsensus = imageConsensus.isEmpty() ? 0.0 : (sumConsensus / imageConsensus.size()) * 100.0;

        TaskAnalyticsResponse.Consensus consensus = TaskAnalyticsResponse.Consensus.builder()
                .overallAverage(avgConsensus)
                .lowConsensusImages(lowConsensusImages)
                .threshold(0.8)
                .build();

        return TaskAnalyticsResponse.builder()
                .taskId(taskId)
                .status("ACTIVE")
                .progress(progress)
                .speciesAnalytics(saList)
                .generatedAt(java.time.LocalDateTime.now().toString())
                .consensus(consensus)
                .participation(participation)
                .quality(quality)
                .timeSeries(List.of())
                .build();
    }

    // ─── Platform-wide overview endpoint (Researcher / SuperAdmin) ───────────

    /**
     * Returns a comprehensive platform snapshot with time-windowed activity,
     * confidence trend (last 30 days), and label distribution (last 30 days).
     * All date windows use server time (Asia/Jerusalem).
     */
    @Cacheable(value = CacheConfig.CACHE_PLATFORM_OVERVIEW)
    @Transactional(readOnly = true)
    public PlatformOverviewResponse getPlatformOverview() {
        LocalDate today = LocalDate.now();
        LocalDate startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate startOfMonth = today.with(TemporalAdjusters.firstDayOfMonth());
        LocalDate thirtyDaysAgo = today.minusDays(30);

        return PlatformOverviewResponse.builder()
                .today(buildActivitySummary(today))
                .thisWeek(buildActivitySummary(startOfWeek))
                .thisMonth(buildActivitySummary(startOfMonth))
                .confidenceTrend(buildConfidenceTrend(thirtyDaysAgo))
                .labelDistribution(buildLabelDistribution(thirtyDaysAgo.atStartOfDay()))
                .totals(buildPlatformTotals())
                .build();
    }

    private PlatformOverviewResponse.ActivitySummary buildActivitySummary(LocalDate since) {
        return PlatformOverviewResponse.ActivitySummary.builder()
                .classifications(classificationFactRepository.countClassificationsSince(since))
                .uniqueImages(classificationFactRepository.countDistinctImagesSince(since))
                .uniqueUsers(classificationFactRepository.countDistinctUsersSince(since))
                .uniqueTasks(classificationFactRepository.countDistinctTasksSince(since))
                .uniqueExperiments(classificationFactRepository.countDistinctExperimentsSince(since))
                .build();
    }

    private List<PlatformOverviewResponse.ConfidenceTrendPoint> buildConfidenceTrend(LocalDate since) {
        List<Object[]> rows = classificationFactRepository.getDailyCredibilityTrend(since);
        List<PlatformOverviewResponse.ConfidenceTrendPoint> points = new ArrayList<>();
        for (Object[] row : rows) {
            LocalDate date = (LocalDate) row[0];
            double avgCredibility = row[1] != null ? ((Number) row[1]).doubleValue() : 0.0;
            long count = row[2] != null ? ((Number) row[2]).longValue() : 0L;
            points.add(PlatformOverviewResponse.ConfidenceTrendPoint.builder()
                    .date(date.toString())
                    .averageCredibility(avgCredibility)
                    .classificationCount(count)
                    .build());
        }
        return points;
    }

    private List<PlatformOverviewResponse.LabelDistributionPoint> buildLabelDistribution(LocalDateTime since) {
        List<Object[]> rows = classificationRepository.getLabelDistributionSince(since);

        // Compute total for percentage
        long total = rows.stream()
                .mapToLong(r -> r[1] != null ? ((Number) r[1]).longValue() : 0L)
                .sum();

        // Pre-fill all known labels with 0 so the response always includes every label
        Map<String, Long> distribution = new HashMap<>();
        Arrays.stream(UserResponse.values()).forEach(ur -> distribution.put(ur.name(), 0L));

        for (Object[] row : rows) {
            String label = row[0] != null ? row[0].toString() : "UNKNOWN";
            long count = row[1] != null ? ((Number) row[1]).longValue() : 0L;
            distribution.put(label, count);
        }

        return distribution.entrySet().stream()
                .map(e -> PlatformOverviewResponse.LabelDistributionPoint.builder()
                        .label(e.getKey())
                        .count(e.getValue())
                        .percentage(total > 0 ? (double) e.getValue() / total * 100 : 0.0)
                        .build())
                .collect(Collectors.toList());
    }

    private PlatformOverviewResponse.PlatformTotals buildPlatformTotals() {
        return PlatformOverviewResponse.PlatformTotals.builder()
                .totalUsers(userRepository.count())
                .totalClassifications(classificationRepository.count())
                .totalImages(imageRepository.count())
                .activeTasks(taskRepository.countByStatus(TaskStatus.ACTIVE))
                .build();
    }

    // ─── Admin leaderboard / user-performance endpoints (implemented) ─────────

    @Transactional(readOnly = true)
    public List<UserPerformanceResponse> getUserPerformanceMetrics(Long taskId) {
        List<Object[]> rows = classificationFactRepository.getUserPerformanceAggregation(taskId);
        return rows.stream()
                .map(this::mapToUserPerformanceResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<UserPerformanceResponse> getTopPerformers(int limit) {
        if (limit <= 0) {
            return List.of();
        }
        List<Object[]> rows = classificationFactRepository
                .getTopPerformersAggregation(PageRequest.of(0, limit));
        return rows.stream()
                .map(this::mapToUserPerformanceResponse)
                .collect(Collectors.toList());
    }

    /**
     * Maps a ClassificationFact aggregation row
     * (userId, totalCount, correctCount, avgCredibility) to UserPerformanceResponse.
     */
    private UserPerformanceResponse mapToUserPerformanceResponse(Object[] row) {
        String username = (String) row[0];
        int total = row[1] != null ? ((Number) row[1]).intValue() : 0;
        int correct = row[2] != null ? ((Number) row[2]).intValue() : 0;
        int goldTotal = row[3] != null ? ((Number) row[3]).intValue() : 0;
        double avgCredibility = row[4] != null ? ((Number) row[4]).doubleValue() : 0.0;
        double accuracy = goldTotal > 0 ? (double) correct / goldTotal : 0.0;

        return UserPerformanceResponse.builder()
                .username(username)
                .displayName(username)
                .totalClassifications(total)
                .goldImageClassifications(goldTotal)
                .correctGoldClassifications(correct)
                .goldAccuracy(accuracy)
                .credibilityScore(avgCredibility)
                .currentStreak(0)
                .points(0)
                .build();
    }

    // ─── Dashboard stats (wired to StatisticsService logic inline) ───────────

    @Transactional(readOnly = true)
    public DashboardStatsResponse getGlobalStats() {
        return DashboardStatsResponse.builder()
                .totalUsers(userRepository.count())
                .totalSwipes(classificationRepository.count())
                .activeTasks(taskRepository.countByStatus(TaskStatus.ACTIVE))
                .totalImages(imageRepository.count())
                .build();
    }
}






