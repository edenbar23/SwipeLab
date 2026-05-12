package com.swipelab.dto.request;

import com.swipelab.model.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class InviteAdminRequest {
    @NotBlank(message = "Email is required for invitation")
    @Email(message = "Invalid email format")
    private String email;

    private UserRole role = UserRole.RESEARCHER;
}
