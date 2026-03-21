package com.swipelab.integration.stardbi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StardbiRefreshTokenRequestDto {
    private String refresh;
}
