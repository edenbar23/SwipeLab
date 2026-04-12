package com.swipelab.gamification.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UserBadgeDto {
    private String title;
    private String description;
    private String iconUrl;
    private LocalDateTime earnedAt;
}
