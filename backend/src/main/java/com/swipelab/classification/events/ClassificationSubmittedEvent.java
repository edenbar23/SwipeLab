package com.swipelab.classification.events;

import com.swipelab.classification.domain.core.Classification.UserResponse;
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
    private boolean isCorrect;
    private boolean isGoldStandard;
    private LocalDateTime submittedAt;

    // The actual answer the user gave (YES / NO / DONT_KNOW / TRASH).
    // Required by gamification (rank) and collection listeners.
    private UserResponse userResponse;

    // Analytics fields
    private String species;
    private Long responseTimeMs;
    private Double userCredibility;

    // Role at the time of classification — used by credibility and analytics listeners
    // to distinguish expert (RESEARCHER) from regular user labels without an extra DB fetch.
    private String userRole;
}

