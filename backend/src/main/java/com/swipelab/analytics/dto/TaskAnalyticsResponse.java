package com.swipelab.analytics.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class TaskAnalyticsResponse {
    private Long taskId;
    private String status;
    private Progress progress;
    private Consensus consensus;
    private List<SpeciesAnalytics> speciesAnalytics;
    private Participation participation;
    private Quality quality;
    private List<TimeSeriesPoint> timeSeries;
    private String generatedAt;

    @Data
    @Builder
    public static class Progress {
        private Integer totalImages;
        private Integer imagesClassified;
        private Integer completedImages;
        private Double percentComplete;
    }

    @Data
    @Builder
    public static class Consensus {
        private Double overallAverage;
        private Integer lowConsensusImages;
        private Double threshold;
    }

    @Data
    @Builder
    public static class SpeciesAnalytics {
        private String name;
        private Integer classificationCount;
        private Double agreementRate;
        private ConfusionMatrix confusionMatrix;
    }

    @Data
    @Builder
    public static class ConfusionMatrix {
        private Integer truePositive;
        private Integer falsePositive;
        private Integer falseNegative;
        private Integer trueNegative;
    }

    @Data
    @Builder
    public static class Participation {
        private Integer activeUsers;
        private Integer totalClassifications;
        private Integer averageClassificationsPerUser;
        private Long medianResponseTimeMs;
    }

    @Data
    @Builder
    public static class Quality {
        private Double averageCredibility;
        private Double expertAgreement;
        private Integer lowQualityUsers;
    }

    @Data
    @Builder
    public static class TimeSeriesPoint {
        private String date;
        private Integer classifications;
        private Integer consensusReached;
    }
}
