package com.swipelab.analytics.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserProgressResponse {
    private Integer completed;
    private Double accuracy;
}
