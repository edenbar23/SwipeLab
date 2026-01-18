package com.swipelab.classification.dto.api;

import com.swipelab.classification.domain.Classification;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SubmitClassificationRequest {
    @NotNull
    private Long imageId;

    @NotNull
    private Long taskId;

    private String question;

    @NotNull
    private Classification.UserResponse decision;

    private Long responseTimeMs;
}
