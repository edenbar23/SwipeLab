package com.swipelab.gamification.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GamificationUpdatedEvent {
    private String username;
    private Long score;
    private String badges;
    private String rank;
}
