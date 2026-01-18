package com.swipelab.dto.response;

import com.swipelab.classification.domain.GoldImage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GoldImageResponse {
    private Long id;
    private Long imageId;
    private String species;
    private GoldImage.UserResponse correctAnswer;
}
