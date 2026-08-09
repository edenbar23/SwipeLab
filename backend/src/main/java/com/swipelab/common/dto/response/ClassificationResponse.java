package com.swipelab.common.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ClassificationResponse {
    private Long id;
    private String userId;
    private Long imageId;
    private Long labelId;
    private Boolean isCorrect; // For Gold Standard feedback
    private LocalDateTime createdAt;
}

