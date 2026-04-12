package com.swipelab.gamification.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BadgeDto {
    private String title;
    private String iconUrl;
}
