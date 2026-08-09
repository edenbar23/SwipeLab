package com.swipelab.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank
    private String username;   // unique identifier

    @NotBlank
    private String password;
}




