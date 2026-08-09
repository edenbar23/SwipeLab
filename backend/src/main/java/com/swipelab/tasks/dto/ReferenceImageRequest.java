package com.swipelab.tasks.dto;

import lombok.Data;

@Data
public class ReferenceImageRequest {

    private String contentType; // image/jpeg
    private String data;        // base64
    private String caption;
}

