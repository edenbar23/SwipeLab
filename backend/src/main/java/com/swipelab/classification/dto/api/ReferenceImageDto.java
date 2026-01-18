package com.swipelab.classification.dto.api;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ReferenceImageDto {
    private String contentType;
    private String data;
    private String caption;
}
