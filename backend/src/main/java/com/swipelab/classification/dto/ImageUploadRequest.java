package com.swipelab.classification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ImageUploadRequest {

    @NotBlank(message = "Image URL is required")
    private String imageUrl;

    private String caption;

    @NotNull(message = "Task ID is required")
    private Long taskId;

    @Builder.Default
    private Integer priority = 0;

    @Builder.Default
    private Boolean isGoldStandard = false;

    private Long correctLabelId; // Optional, required if isGoldStandard is true
}



