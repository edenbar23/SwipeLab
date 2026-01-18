package com.swipelab.analytics.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "task_species_stats", indexes = {
        @Index(name = "idx_tss_task_species", columnList = "task_id, species", unique = true)
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskSpeciesStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_id", nullable = false)
    private Long taskId;

    @Column(name = "species", nullable = false)
    private String species;

    @Column(name = "classification_count")
    @Builder.Default
    private Integer classificationCount = 0;

    @Column(name = "agreement_rate")
    @Builder.Default
    private Double agreementRate = 0.0;

    @Column(name = "true_positive")
    @Builder.Default
    private Integer truePositive = 0;

    @Column(name = "false_positive")
    @Builder.Default
    private Integer falsePositive = 0;

    @Column(name = "false_negative")
    @Builder.Default
    private Integer falseNegative = 0;

    @Column(name = "true_negative")
    @Builder.Default
    private Integer trueNegative = 0;
}
