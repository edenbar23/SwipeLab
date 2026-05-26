package com.swipelab.classification.dto.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassificationWarningDto {
    private String level; // "WARNING_1" or "WARNING_2"
    private String message;
    private int strikeCount;
    private int strikesUntilBan;
}
