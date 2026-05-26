package com.swipelab.analytics.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "classification_facts", indexes = {
        @Index(name = "idx_cf_task_id", columnList = "task_id"),
        @Index(name = "idx_cf_user_id", columnList = "user_id"),
        @Index(name = "idx_cf_day", columnList = "`day`")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassificationFact {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "classification_id")
    private Long classificationId; // Null for gold images check if we want to track it

    @Column(name = "task_id", nullable = false)
    private Long taskId;

    @Column(name = "image_id", nullable = false)
    private Long imageId;

    @Column(name = "user_id", nullable = false)
    private String userId; // Username

    @Column(name = "species")
    private String species;

    @Column(name = "is_correct")
    private Boolean isCorrect; // Null if no gold standard comparison available (normal classification) ->
                               // Wait, user wants booleans.
    // Normal classifications: unknown correctness initially.
    // But API requirements show "accuracy". Accuracy implies ground truth.
    // For normal classifications, we might not know correctness immediately.
    // Gold images have known correctness.
    // Expert classifications define "truth".
    // "Consensus" defines truth eventually.
    // For now, I will map what I have. If unknown, maybe null.

    @Column(name = "is_expert")
    private Boolean isExpert;

    @Column(name = "credibility_at_time")
    private Double credibilityAtTime;

    @Column(name = "response_time_ms")
    private Long responseTimeMs;

    // Consensus score might be updated later? Or computed?
    // User schema says: consensus_score FLOAT.
    @Column(name = "consensus_score")
    private Double consensusScore;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "`day`", nullable = false)
    private LocalDate day;
}
