package com.swipelab.dto.response;

import com.swipelab.users.domain.NotificationSeverity;
import com.swipelab.users.domain.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * API response for a single AdminNotification record.
 * Exposed on GET /api/admin/notifications.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminNotificationResponse {

    private Long id;
    private NotificationType type;
    private NotificationSeverity severity;
    private String title;
    private String message;
    private String targetUsername;
    private Boolean isRead;
    private LocalDateTime createdAt;
}
