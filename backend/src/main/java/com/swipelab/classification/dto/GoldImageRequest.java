package com.swipelab.classification.dto;

import com.swipelab.classification.domain.goldimage.GoldImage;
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



