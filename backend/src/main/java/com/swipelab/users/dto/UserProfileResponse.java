package com.swipelab.users.dto;

import com.swipelab.model.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

@Data
@Builder
@AllArgsConstructor
public class UserProfileResponse {

    private String username;
    private String email;
    private String displayName;
    private String profileImageUrl;
    private UserRole role;
    private String provider;

    // Gamification data
    private Long score;
    private List<String> badges;
    private String rank;
    private int currentStreak;
    @JsonProperty("isSuperAdmin")
    private boolean isSuperAdmin;
    private boolean active;

    // Credibility — composite 0–100 score (40% gold + 35% majority + 25% expert kappa)
    private Double credibilityScore;
}

