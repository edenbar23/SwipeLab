package com.swipelab.analytics.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserVsExpertsResponse {
    private Stat user;
    private Stat experts;
    private Stat difference;

    @Data
    @Builder
    public static class Stat {
        private Double accuracy;
    }
}
