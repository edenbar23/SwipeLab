package com.swipelab.analytics.api;

import com.swipelab.analytics.application.AnalyticsService;
import com.swipelab.analytics.dto.*;
import com.swipelab.analytics.dto.DashboardStatsResponse;
import com.swipelab.analytics.dto.UserPerformanceResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AnalyticsControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AnalyticsService analyticsService;

    @InjectMocks
    private AnalyticsController analyticsController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(analyticsController)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    private void setAuthority(String username, String role) {
        UserDetails user = new User(username, "pass",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role)));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }

    // ─── getPlatformOverview ──────────────────────────────────────────────────

    @Test
    void getPlatformOverview_happyFlow_returnsOk() throws Exception {
        setAuthority("researcher1", "RESEARCHER");

        PlatformOverviewResponse overview = PlatformOverviewResponse.builder()
                .today(PlatformOverviewResponse.ActivitySummary.builder()
                        .classifications(42L).uniqueImages(10L).uniqueUsers(5L)
                        .uniqueTasks(3L).uniqueExperiments(2L).build())
                .thisWeek(PlatformOverviewResponse.ActivitySummary.builder()
                        .classifications(200L).uniqueImages(50L).uniqueUsers(20L)
                        .uniqueTasks(5L).uniqueExperiments(4L).build())
                .thisMonth(PlatformOverviewResponse.ActivitySummary.builder()
                        .classifications(800L).uniqueImages(200L).uniqueUsers(50L)
                        .uniqueTasks(8L).uniqueExperiments(6L).build())
                .confidenceTrend(List.of(
                        PlatformOverviewResponse.ConfidenceTrendPoint.builder()
                                .date("2026-05-01").averageCredibility(0.78).classificationCount(30L)
                                .build()))
                .labelDistribution(List.of(
                        PlatformOverviewResponse.LabelDistributionPoint.builder()
                                .label("YES").count(300L).percentage(60.0).build()))
                .totals(PlatformOverviewResponse.PlatformTotals.builder()
                        .totalUsers(100L).totalClassifications(1000L)
                        .totalImages(500L).activeTasks(4L).build())
                .build();

        when(analyticsService.getPlatformOverview()).thenReturn(overview);

        mockMvc.perform(get("/api/v1/analytics/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.today.classifications").value(42))
                .andExpect(jsonPath("$.thisWeek.classifications").value(200))
                .andExpect(jsonPath("$.thisMonth.classifications").value(800))
                .andExpect(jsonPath("$.confidenceTrend[0].averageCredibility").value(0.78))
                .andExpect(jsonPath("$.totals.totalUsers").value(100));
    }

    // ─── getGlobalStats ───────────────────────────────────────────────────────

    @Test
    void getGlobalStats_happyFlow_returnsOk() throws Exception {
        setAuthority("researcher1", "RESEARCHER");

        DashboardStatsResponse stats = DashboardStatsResponse.builder()
                .totalUsers(50L).totalSwipes(2000L).activeTasks(3L).totalImages(400L)
                .build();
        when(analyticsService.getGlobalStats()).thenReturn(stats);

        mockMvc.perform(get("/api/v1/analytics/global-stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").value(50))
                .andExpect(jsonPath("$.totalSwipes").value(2000))
                .andExpect(jsonPath("$.activeTasks").value(3));
    }

    // ─── getUserPerformance ───────────────────────────────────────────────────

    @Test
    void getUserPerformance_happyFlow_returnsPopulatedList() throws Exception {
        setAuthority("researcher1", "RESEARCHER");

        List<UserPerformanceResponse> performers = List.of(
                UserPerformanceResponse.builder()
                        .username("alice").totalClassifications(100)
                        .credibilityScore(0.9).goldAccuracy(0.88).build());

        when(analyticsService.getUserPerformanceMetrics(null)).thenReturn(performers);

        mockMvc.perform(get("/api/v1/analytics/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("alice"))
                .andExpect(jsonPath("$[0].totalClassifications").value(100));
    }

    @Test
    void getUserPerformance_withTaskId_returnsFilteredList() throws Exception {
        setAuthority("researcher1", "RESEARCHER");

        List<UserPerformanceResponse> performers = List.of(
                UserPerformanceResponse.builder()
                        .username("bob").totalClassifications(30)
                        .credibilityScore(0.75).goldAccuracy(0.7).build());

        when(analyticsService.getUserPerformanceMetrics(eq(5L))).thenReturn(performers);

        mockMvc.perform(get("/api/v1/analytics/users").param("taskId", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("bob"));
    }

    @Test
    void getUserPerformance_edgeCase_emptyResult_returnsEmptyArray() throws Exception {
        setAuthority("researcher1", "RESEARCHER");

        when(analyticsService.getUserPerformanceMetrics(null)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/analytics/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    // ─── getTopPerformers ─────────────────────────────────────────────────────

    @Test
    void getTopPerformers_happyFlow_returnsOrderedList() throws Exception {
        setAuthority("researcher1", "RESEARCHER");

        List<UserPerformanceResponse> topPerformers = List.of(
                UserPerformanceResponse.builder().username("charlie")
                        .totalClassifications(500).credibilityScore(0.95).build(),
                UserPerformanceResponse.builder().username("dave")
                        .totalClassifications(300).credibilityScore(0.88).build());

        when(analyticsService.getTopPerformers(2)).thenReturn(topPerformers);

        mockMvc.perform(get("/api/v1/analytics/top-performers").param("limit", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("charlie"))
                .andExpect(jsonPath("$[1].username").value("dave"));
    }

    @Test
    void getTopPerformers_edgeCase_defaultLimit_uses10() throws Exception {
        setAuthority("researcher1", "RESEARCHER");

        when(analyticsService.getTopPerformers(10)).thenReturn(Collections.emptyList());

        // No explicit limit param → controller default = 10
        mockMvc.perform(get("/api/v1/analytics/top-performers"))
                .andExpect(status().isOk());
    }

    // ─── User stats (existing endpoints, sanity check) ────────────────────────

    @Test
    void getUserStatistics_happyFlow_returnsOk() throws Exception {
        setAuthority("alice", "USER");

        UserStatisticsResponse statsResponse = UserStatisticsResponse.builder()
                .summary(UserStatisticsResponse.Summary.builder()
                        .totalClassifications(50).correctClassifications(40)
                        .accuracy(0.8).contributionPercentage(0.0)
                        .rank(UserStatisticsResponse.Rank.builder()
                                .allTime(5).daily(0).weekly(0).monthly(0).build())
                        .rankPercentile(0)
                        .build())
                .trend(UserStatisticsResponse.Trend.builder()
                        .byDay(Collections.emptyList()).build())
                .build();

        when(analyticsService.getUserStatistics("alice")).thenReturn(statsResponse);

        mockMvc.perform(get("/api/v1/statistics/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.totalClassifications").value(50))
                .andExpect(jsonPath("$.summary.accuracy").value(0.8));
    }
}

