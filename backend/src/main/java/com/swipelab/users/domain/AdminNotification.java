package com.swipelab.users.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Persistent notification surfaced in the Super Admin dashboard.
 *
 * Created by AdminNotificationService whenever:
 *   - A user receives a fraud detection warning.
 *   - A user is automatically banned by the system.
 *   - A warned user recovers their ACTIVE status.
 *
 * is_read is toggled by the admin via PATCH /api/admin/notifications/{id}/read.
 */
@Entity
@Table(name = "admin_notifications",
        indexes = {
                @Index(name = "idx_an_is_read", columnList = "is_read"),
                @Index(name = "idx_an_type", columnList = "type"),
                @Index(name = "idx_an_target_username", columnList = "target_username"),
                @Index(name = "idx_an_created_at", columnList = "created_at")
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50)
    private NotificationType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 20)
    private NotificationSeverity severity;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    /** Username of the user this notification is about (nullable for SYSTEM_ALERT). */
    @Column(name = "target_username", length = 255)
    private String targetUsername;

    /** Whether the admin has dismissed/read this notification. */
    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private Boolean isRead = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
