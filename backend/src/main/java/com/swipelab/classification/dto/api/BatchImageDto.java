package com.swipelab.classification.dto.api;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class BatchImageDto {
    private Long imageId;
    private Long taskId;
    private String question;
    private ImageDataDto image;
    private List<ReferenceImageDto> referenceImages;
}
