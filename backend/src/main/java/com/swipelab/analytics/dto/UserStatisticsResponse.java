package com.swipelab.analytics.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class UserStatisticsResponse {
    private Summary summary;
    private Trend trend;

    @Data
    @Builder
    public static class Summary {
        private Integer totalClassifications;
        private Integer correctClassifications;
        private Double accuracy;
        private Double contributionPercentage;
        private Rank rank; // {daily: 3, ...}
        private Integer rankPercentile;
    }

    @Data
    @Builder
    public static class Rank {
        private Integer daily;
        private Integer weekly;
        private Integer monthly;
        private Integer allTime;
    }

    @Data
    @Builder
    public static class Trend {
        private List<DayPoint> byDay;
    }

    @Data
    @Builder
    public static class DayPoint {
        private String date;
        private Double accuracy;
    }
}
