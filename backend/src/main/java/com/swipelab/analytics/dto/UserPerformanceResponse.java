package com.swipelab.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPerformanceResponse {
    private String username;
    private String displayName;
    private int totalClassifications;
    private int goldImageClassifications;
    private int correctGoldClassifications;
    private double goldAccuracy;
    private double credibilityScore;
    private int currentStreak;
    private long points;
}



