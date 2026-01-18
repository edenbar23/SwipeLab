package com.swipelab.classification.dto.api;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ImageDataDto {
    private String contentType;
    private String data; // Base64 encoded data
}
