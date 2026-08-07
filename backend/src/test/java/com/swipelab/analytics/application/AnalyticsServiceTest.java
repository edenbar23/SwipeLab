package com.swipelab.analytics.application;

import com.swipelab.analytics.domain.UserDailyStats;
import com.swipelab.analytics.dto.PlatformOverviewResponse;
import com.swipelab.analytics.dto.TaskAnalyticsResponse;
import com.swipelab.analytics.dto.UserProgressResponse;
import com.swipelab.analytics.infrastructure.*;
import com.swipelab.classification.domain.core.Classification.UserResponse;
import com.swipelab.classification.infrastructure.ClassificationRepository;
import com.swipelab.classification.infrastructure.ImageRepository;
import com.swipelab.dto.response.DashboardStatsResponse;
import com.swipelab.dto.response.UserPerformanceResponse;
import com.swipelab.tasks.domain.TaskStatus;
import com.swipelab.tasks.infrastructure.TaskRepository;
import com.swipelab.users.infrastructure.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock private ClassificationFactRepository classificationFactRepository;
    @Mock private ClassificationRepository classificationRepository;
    @Mock private UserDailyStatsRepository userDailyStatsRepository;
    @Mock private TaskDailyStatsRepository taskDailyStatsRepository;
    @Mock private TaskSpeciesStatsRepository taskSpeciesStatsRepository;
    @Mock private UserRankingRepository userRankingRepository;
    @Mock private UserRepository userRepository;
    @Mock private TaskRepository taskRepository;
    @Mock private ImageRepository imageRepository;

    @InjectMocks
    private AnalyticsService analyticsService;

    // ─── getUserProgress ──────────────────────────────────────────────────────

    @Test
    void getUserProgress_happyFlow_returnsCorrectAccuracy() {
        // 10 total, 8 correct → 80 % accuracy
        Object[] row = {10L, 8L};
        when(userDailyStatsRepository.getProgressSince(eq("alice"), any(LocalDate.class)))
                .thenReturn(row);

        UserProgressResponse result = analyticsService.getUserProgress("alice");

        assertEquals(10, result.getCompleted());
        assertEquals(0.8, result.getAccuracy(), 0.001);
    }

    @Test
    void getUserProgress_edgeCase_noData_returnsZeros() {
        // Repository returns null result when the user has no data
        when(userDailyStatsRepository.getProgressSince(eq("ghost"), any(LocalDate.class)))
                .thenReturn(null);

        UserProgressResponse result = analyticsService.getUserProgress("ghost");

        assertEquals(0, result.getCompleted());
        assertEquals(0.0, result.getAccuracy());
    }

    // ─── getPlatformOverview ──────────────────────────────────────────────────

    @Test
    void getPlatformOverview_happyFlow_returnsPopulatedSnapshot() {
        // Stub activity aggregates
        when(classificationFactRepository.countClassificationsSince(any())).thenReturn(42L);
        when(classificationFactRepository.countDistinctImagesSince(any())).thenReturn(10L);
        when(classificationFactRepository.countDistinctUsersSince(any())).thenReturn(5L);
        when(classificationFactRepository.countDistinctTasksSince(any())).thenReturn(3L);
        when(classificationFactRepository.countDistinctExperimentsSince(any())).thenReturn(2L);

        // Stub confidence trend: one row (today, 0.75 avg, 20 count)
        LocalDate today = LocalDate.now();
        Object[] trendRow = {today, 0.75, 20L};
        List<Object[]> trendRows = new java.util.ArrayList<>();
        trendRows.add(trendRow);
        when(classificationFactRepository.getDailyCredibilityTrend(any())).thenReturn(trendRows);

        // Stub label distribution: YES=30, NO=12
        Object[] yesRow = {UserResponse.YES, 30L};
        Object[] noRow  = {UserResponse.NO,  12L};
        List<Object[]> labelRows = new java.util.ArrayList<>();
        labelRows.add(yesRow);
        labelRows.add(noRow);
        when(classificationRepository.getLabelDistributionSince(any(LocalDateTime.class)))
                .thenReturn(labelRows);

        // Stub totals
        when(userRepository.count()).thenReturn(100L);
        when(classificationRepository.count()).thenReturn(500L);
        when(imageRepository.count()).thenReturn(200L);
        when(taskRepository.countByStatus(TaskStatus.ACTIVE)).thenReturn(4L);

        PlatformOverviewResponse response = analyticsService.getPlatformOverview();

        assertNotNull(response);

        // Activity summaries are called three times (today/week/month)
        verify(classificationFactRepository, times(3)).countClassificationsSince(any());

        // Each window should show the mocked count
        assertEquals(42L, response.getToday().getClassifications());
        assertEquals(42L, response.getThisWeek().getClassifications());
        assertEquals(42L, response.getThisMonth().getClassifications());

        // Confidence trend
        assertEquals(1, response.getConfidenceTrend().size());
        assertEquals(today.toString(), response.getConfidenceTrend().get(0).getDate());
        assertEquals(0.75, response.getConfidenceTrend().get(0).getAverageCredibility(), 0.001);

        // Label distribution — all 4 labels present, even if not in query result
        assertEquals(4, response.getLabelDistribution().size());

        // Totals
        assertEquals(100L, response.getTotals().getTotalUsers());
        assertEquals(4L, response.getTotals().getActiveTasks());
    }

    @Test
    void getPlatformOverview_edgeCase_noDataYet_returnsZeroFilled() {
        when(classificationFactRepository.countClassificationsSince(any())).thenReturn(0L);
        when(classificationFactRepository.countDistinctImagesSince(any())).thenReturn(0L);
        when(classificationFactRepository.countDistinctUsersSince(any())).thenReturn(0L);
        when(classificationFactRepository.countDistinctTasksSince(any())).thenReturn(0L);
        when(classificationFactRepository.countDistinctExperimentsSince(any())).thenReturn(0L);
        when(classificationFactRepository.getDailyCredibilityTrend(any())).thenReturn(Collections.emptyList());
        when(classificationRepository.getLabelDistributionSince(any(LocalDateTime.class))).thenReturn(Collections.emptyList());
        when(userRepository.count()).thenReturn(0L);
        when(classificationRepository.count()).thenReturn(0L);
        when(imageRepository.count()).thenReturn(0L);
        when(taskRepository.countByStatus(TaskStatus.ACTIVE)).thenReturn(0L);

        PlatformOverviewResponse response = analyticsService.getPlatformOverview();

        assertNotNull(response);
        assertEquals(0L, response.getToday().getClassifications());
        assertTrue(response.getConfidenceTrend().isEmpty());

        // All 4 label slots must still be present with count 0
        assertEquals(4, response.getLabelDistribution().size());
        response.getLabelDistribution().forEach(p -> assertEquals(0L, p.getCount()));
    }

    // ─── getUserPerformanceMetrics ────────────────────────────────────────────

    @Test
    void getUserPerformanceMetrics_happyFlow_mapsAggregationCorrectly() {
        // (userId, total, correct, avgCredibility)
        Object[] row = {"alice", 50L, 40L, 0.85};
        List<Object[]> perfRows = new java.util.ArrayList<>();
        perfRows.add(row);
        when(classificationFactRepository.getUserPerformanceAggregation(eq(1L)))
                .thenReturn(perfRows);

        List<UserPerformanceResponse> result = analyticsService.getUserPerformanceMetrics(1L);

        assertEquals(1, result.size());
        UserPerformanceResponse resp = result.get(0);
        assertEquals("alice", resp.getUsername());
        assertEquals(50, resp.getTotalClassifications());
        assertEquals(0.85, resp.getCredibilityScore(), 0.001);
        assertEquals(0.8, resp.getGoldAccuracy(), 0.001); // 40/50
    }

    @Test
    void getUserPerformanceMetrics_edgeCase_nullTaskId_returnsAllUsers() {
        when(classificationFactRepository.getUserPerformanceAggregation(null))
                .thenReturn(Collections.emptyList());

        List<UserPerformanceResponse> result = analyticsService.getUserPerformanceMetrics(null);

        assertTrue(result.isEmpty());
        verify(classificationFactRepository).getUserPerformanceAggregation(null);
    }

    // ─── getTopPerformers ─────────────────────────────────────────────────────

    @Test
    void getTopPerformers_happyFlow_returnsLimitedList() {
        Object[] row = {"bob", 100L, 90L, 0.92};
        List<Object[]> topRows = new java.util.ArrayList<>();
        topRows.add(row);
        when(classificationFactRepository.getTopPerformersAggregation(any(Pageable.class)))
                .thenReturn(topRows);

        List<UserPerformanceResponse> result = analyticsService.getTopPerformers(10);

        assertEquals(1, result.size());
        assertEquals("bob", result.get(0).getUsername());
    }

    @Test
    void getTopPerformers_edgeCase_zeroLimit_returnsEmptyList() {
        List<UserPerformanceResponse> result = analyticsService.getTopPerformers(0);

        assertTrue(result.isEmpty());
        verifyNoInteractions(classificationFactRepository);
    }

    // ─── getGlobalStats ───────────────────────────────────────────────────────

    @Test
    void getGlobalStats_happyFlow_returnsCorrectCounts() {
        when(userRepository.count()).thenReturn(200L);
        when(classificationRepository.count()).thenReturn(1000L);
        when(taskRepository.countByStatus(TaskStatus.ACTIVE)).thenReturn(5L);
        when(imageRepository.count()).thenReturn(300L);

        DashboardStatsResponse stats = analyticsService.getGlobalStats();

        assertEquals(200L, stats.getTotalUsers());
        assertEquals(1000L, stats.getTotalSwipes());
        assertEquals(5L, stats.getActiveTasks());
        assertEquals(300L, stats.getTotalImages());
    }

    // ─── getTaskAnalytics ─────────────────────────────────────────────────────

    @Test
    void getTaskAnalytics_happyFlow_computesProgressFromImageCounts() {
        // 200 total crops in the task, 50 of them classified → 25 % complete
        when(classificationFactRepository.countCompletedImages(1L)).thenReturn(50L);
        when(classificationFactRepository.findByTaskId(1L)).thenReturn(Collections.emptyList());
        when(taskSpeciesStatsRepository.findByTaskId(1L)).thenReturn(Collections.emptyList());
        when(imageRepository.countByTaskId(1L)).thenReturn(200L);

        TaskAnalyticsResponse response = analyticsService.getTaskAnalytics(1L);

        TaskAnalyticsResponse.Progress progress = response.getProgress();
        assertEquals(200, progress.getTotalImages());
        assertEquals(50, progress.getImagesClassified());
        assertEquals(50, progress.getCompletedImages());
        assertEquals(25.0, progress.getPercentComplete(), 0.001);
    }

    @Test
    void getTaskAnalytics_edgeCase_noImages_returnsZeroPercent() {
        // No crops imported yet → avoid division by zero, report 0 %
        when(classificationFactRepository.countCompletedImages(2L)).thenReturn(0L);
        when(classificationFactRepository.findByTaskId(2L)).thenReturn(Collections.emptyList());
        when(taskSpeciesStatsRepository.findByTaskId(2L)).thenReturn(Collections.emptyList());
        when(imageRepository.countByTaskId(2L)).thenReturn(0L);

        TaskAnalyticsResponse response = analyticsService.getTaskAnalytics(2L);

        TaskAnalyticsResponse.Progress progress = response.getProgress();
        assertEquals(0, progress.getTotalImages());
        assertEquals(0, progress.getImagesClassified());
        assertEquals(0.0, progress.getPercentComplete(), 0.001);
    }
}
