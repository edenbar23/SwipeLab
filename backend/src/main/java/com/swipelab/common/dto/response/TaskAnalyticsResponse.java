package com.swipelab.common.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskAnalyticsResponse {
    private Long taskId;
    private String taskName;
    private String status;
    private int totalImages;
    private int classifiedImages;
    private double completionPercentage;
    private double averageConsensus;
    private int lowConsensusCount; // Images with < 60% agreement
    private int highConsensusCount; // Images with >= 80% agreement
    private Map<String, Long> labelDistribution;
    private int totalClassifications;
    private int uniqueClassifiers;
}

