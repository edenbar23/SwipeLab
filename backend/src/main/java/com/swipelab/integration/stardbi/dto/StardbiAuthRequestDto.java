package com.swipelab.integration.stardbi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StardbiAuthRequestDto {
    private String username;
    private String password;
}
