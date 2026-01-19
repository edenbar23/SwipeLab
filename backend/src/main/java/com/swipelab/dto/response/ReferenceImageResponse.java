package com.swipelab.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ReferenceImageResponse {

    private String contentType; // image/jpeg
    private String data; // base64 or URL (depending on endpoint)
    private String caption;
    private String imageUrl;
}
