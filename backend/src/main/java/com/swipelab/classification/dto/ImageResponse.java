package com.swipelab.classification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ImageResponse {
    private Long id;
    private String imageUrl;
    private String thumbnailUrl;
    private String caption;
    private Long taskId;
    private Integer priority;
    private Boolean isGoldStandard;
}



