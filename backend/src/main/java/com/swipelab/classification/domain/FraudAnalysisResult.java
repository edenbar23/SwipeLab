package com.swipelab.classification.domain;

import com.swipelab.classification.dto.api.ClassificationWarningDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudAnalysisResult {
    private boolean banned;
    private ClassificationWarningDto warning;
}
