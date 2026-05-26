package com.swipelab.classification.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Immutable audit record for each time the fraud detection pipeline
 * flags a user's classification as suspicious.
 *
 * Severity STRIKE is silent (not shown to the user).
 * WARNING_1, WARNING_2, and BAN entries are tied to user-visible events.
 */
@Entity
@Table(name = "suspicious_activity_records",
        indexes = {
                @Index(name = "idx_sar_username", columnList = "username"),
                @Index(name = "idx_sar_severity", columnList = "severity"),
                @Index(name = "idx_sar_created_at", columnList = "created_at")
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SuspiciousActivityRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Decoupled reference — never a FK to avoid cross-module constraints. */
    @Column(name = "username", nullable = false, length = 255)
    private String username;

    @Column(name = "reason", nullable = false, length = 512)
    private String reason;

    /** Actual measured response time that triggered the suspicious flag. */
    @Column(name = "response_time_ms")
    private Long responseTimeMs;

    /** The task the user was labeling when the event was triggered. */
    @Column(name = "task_id")
    private Long taskId;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 50)
    private WarningLevel severity;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
