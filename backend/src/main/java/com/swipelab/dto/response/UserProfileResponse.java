package com.swipelab.dto.response;

import com.swipelab.model.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
public class UserProfileResponse {

    private String username;
    private String email;
    private String displayName;
    private String profileImageUrl;
    private UserRole role;

    // Gamification data
    private Long score;
    private List<String> badges;
    private String rank;
}
