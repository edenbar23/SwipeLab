package com.swipelab.analytics.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class TimeSeriesResponse {
    private String metric;
    private List<Point> points;

    @Data
    @Builder
    public static class Point {
        private String timestamp;
        private Double value;
    }
}
