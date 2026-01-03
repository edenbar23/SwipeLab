package com.swipelab.dto.request;

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

    @NotNull(message = "Label ID is required")
    private Long labelId;

    @NotNull(message = "Response time is required for fraud detection")
    private Long responseTimeMs;
}
