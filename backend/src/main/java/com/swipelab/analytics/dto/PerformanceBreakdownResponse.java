package com.swipelab.analytics.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class PerformanceBreakdownResponse {
    private List<CategoryStat> byCategory;

    @Data
    @Builder
    public static class CategoryStat {
        private String category;
        private Double accuracy;
        private Integer total;
    }
}
