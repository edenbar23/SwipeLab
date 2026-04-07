package com.swipelab.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExternalLoginRequest {
    
    @NotBlank
    private String accessToken;
    
    @NotBlank
    private String username;
}
