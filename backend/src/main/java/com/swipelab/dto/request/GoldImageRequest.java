package com.swipelab.dto.request;

import com.swipelab.classification.domain.GoldImage;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GoldImageRequest {

    @NotNull(message = "Image ID is required")
    private Long imageId;

    @NotNull(message = "Species is required")
    private String species;

    @NotNull(message = "Correct Answer is required")
    private GoldImage.UserResponse correctAnswer;
}
