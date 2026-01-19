package com.swipelab.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GamificationUserInfoResponse {
    private long score;
    private String badge;
    private int currentStreak;
}
