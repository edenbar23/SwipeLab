package com.swipelab.gamification.challenge;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "challenge_definition")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChallengeDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "metric_type", nullable = false)
    private MetricType metricType;

    @Enumerated(EnumType.STRING)
    @Column(name = "aggregation_type", nullable = false)
    private AggregationType aggregationType;

    @Column(name = "target_value", nullable = false)
    private int targetValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "time_window_type", nullable = false)
    private TimeWindowType timeWindowType;

    @Column(name = "badge_id", nullable = false)
    private UUID badgeId;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "available_from")
    private LocalDateTime availableFrom;

    @Column(name = "available_until")
    private LocalDateTime availableUntil;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
