package com.swipelab.dto.response;

import com.swipelab.model.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class UserProfileResponse {

    private String email;
    private String username;
    private UserRole role;
}
