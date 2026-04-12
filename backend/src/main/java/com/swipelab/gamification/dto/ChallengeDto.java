package com.swipelab.gamification.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class ChallengeDto {
    private UUID challengeId;
    private String name;
    private String description;
    private int progress;
    private int target;
    private boolean completed;
    private LocalDateTime windowStart;
    private LocalDateTime windowEnd;
    private BadgeDto badge;
}
