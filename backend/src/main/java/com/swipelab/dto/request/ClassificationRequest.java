package com.swipelab.dto.request;

import com.swipelab.classification.domain.Classification;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ClassificationRequest {

    @NotNull(message = "Image ID is required")
    private Long imageId;

    @NotNull(message = "User Response is required")
    private Classification.UserResponse userResponse;

    private Long responseTimeMs;
}
