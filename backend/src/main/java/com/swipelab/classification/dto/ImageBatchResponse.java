package com.swipelab.classification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ImageBatchResponse {
    private List<ImageResponse> images;
    private Long nextCursor; // Optional: pagination
}



