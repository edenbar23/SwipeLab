package com.swipelab.analytics.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DashboardStatsResponse {
    private long totalUsers;
    private long totalSwipes;
    private long activeTasks;
    private long totalImages;
}



