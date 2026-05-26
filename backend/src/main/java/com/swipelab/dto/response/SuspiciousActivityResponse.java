package com.swipelab.dto.response;

import com.swipelab.classification.domain.WarningLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * API response for a single SuspiciousActivityRecord.
 * Exposed on GET /api/admin/suspicious-activity.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SuspiciousActivityResponse {

    private Long id;
    private String username;
    private String reason;
    private Long responseTimeMs;
    private Long taskId;
    private WarningLevel severity;
    private LocalDateTime createdAt;
}
