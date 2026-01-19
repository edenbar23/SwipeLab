package com.swipelab.analytics.application;

import com.swipelab.analytics.domain.*;
import com.swipelab.analytics.dto.*;
import com.swipelab.analytics.infrastructure.*;
import com.swipelab.dto.response.UserPerformanceResponse; // Existing DTO
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

        private final ClassificationFactRepository classificationFactRepository;
        private final UserDailyStatsRepository userDailyStatsRepository;
        private final TaskDailyStatsRepository taskDailyStatsRepository;
        private final TaskSpeciesStatsRepository taskSpeciesStatsRepository;
        private final UserRankingRepository userRankingRepository;

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
                // Summary
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

                // Rank
                UserRanking ranking = userRankingRepository.findByUserIdAndPeriod(userId, "ALL_TIME")
                                .orElse(UserRanking.builder().rank(0).percentile(0).build());

                // Trend
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

                if (userAcc == null)
                        userAcc = 0.0;
                if (expertAcc == null)
                        expertAcc = 0.0;

                return UserVsExpertsResponse.builder()
                                .user(UserVsExpertsResponse.Stat.builder().accuracy(userAcc).build())
                                .experts(UserVsExpertsResponse.Stat.builder().accuracy(expertAcc).build())
                                .difference(UserVsExpertsResponse.Stat.builder().accuracy(userAcc - expertAcc).build())
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
                List<UserDailyStats> stats = userDailyStatsRepository.findByUserIdAndDayAfterOrderByDayAsc(userId,
                                start);

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

        @Transactional(readOnly = true)
        public TaskAnalyticsResponse getTaskAnalytics(Long taskId) {
                Long completedImages = classificationFactRepository.countCompletedImages(taskId);
                List<ClassificationFact> facts = classificationFactRepository.findByTaskId(taskId);
                int totalClassifications = facts.size();

                TaskAnalyticsResponse.Progress progress = TaskAnalyticsResponse.Progress.builder()
                                .imagesClassified(completedImages.intValue())
                                .totalImages(1000)
                                .completedImages(completedImages.intValue())
                                .percentComplete(0.0)
                                .build();

                List<TaskSpeciesStats> speciesStats = taskSpeciesStatsRepository.findByTaskId(taskId);
                List<TaskAnalyticsResponse.SpeciesAnalytics> saList = speciesStats.stream()
                                .map(s -> TaskAnalyticsResponse.SpeciesAnalytics.builder()
                                                .name(s.getSpecies())
                                                .classificationCount(s.getClassificationCount())
                                                .agreementRate(s.getAgreementRate())
                                                // Confusion matrix placeholders
                                                .confusionMatrix(TaskAnalyticsResponse.ConfusionMatrix.builder()
                                                                .truePositive(s.getTruePositive())
                                                                .falsePositive(s.getFalsePositive())
                                                                .trueNegative(s.getTrueNegative())
                                                                .falseNegative(s.getFalseNegative())
                                                                .build())
                                                .build())
                                .collect(Collectors.toList());

                return TaskAnalyticsResponse.builder()
                                .taskId(taskId)
                                .status("ACTIVE")
                                .progress(progress)
                                .speciesAnalytics(saList)
                                .generatedAt(java.time.LocalDateTime.now().toString())
                                .consensus(TaskAnalyticsResponse.Consensus.builder().build())
                                .participation(TaskAnalyticsResponse.Participation.builder().build())
                                .quality(TaskAnalyticsResponse.Quality.builder().build())
                                .timeSeries(List.of())
                                .build();
        }

        // Support for Legacy/Admin endpoints (Stubs for now to fix build)

        @Transactional(readOnly = true)
        public List<UserPerformanceResponse> getUserPerformanceMetrics(Long taskId) {
                return List.of(); // Placeholder
        }

        @Transactional(readOnly = true)
        public List<UserPerformanceResponse> getTopPerformers(int limit) {
                return List.of(); // Placeholder
        }
}
