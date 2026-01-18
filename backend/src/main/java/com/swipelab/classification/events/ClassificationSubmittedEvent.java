package com.swipelab.classification.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassificationSubmittedEvent {
    private String username;
    private Long classificationId;
    private Long imageId;
    private Long taskId;
    private boolean isCorrect; // Nullable if not applicable, but boolean matches typical event needs
    private boolean isGoldStandard;
    private LocalDateTime submittedAt;

    // Analytics fields
    private String species;
    private Long responseTimeMs;
    private Double userCredibility;
}
