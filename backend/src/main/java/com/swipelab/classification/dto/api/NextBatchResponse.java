package com.swipelab.classification.dto.api;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class NextBatchResponse {
    private List<BatchImageDto> images;
    private ClassificationWarningDto warning;
}
