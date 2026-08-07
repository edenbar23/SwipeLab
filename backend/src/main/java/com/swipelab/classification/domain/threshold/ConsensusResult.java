package com.swipelab.classification.domain.threshold;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "consensus_results", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"image_id", "species"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsensusResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "image_id", nullable = false)
    private Long imageId;

    @Column(name = "task_id", nullable = false)
    private Long taskId;

    @Column(nullable = false)
    private String species;

    @Column(name = "winning_decision", nullable = false)
    private String winningDecision;

    @Column(name = "final_score", nullable = false)
    private Double finalScore;

    @CreationTimestamp
    @Column(name = "reached_at", updatable = false)
    private LocalDateTime reachedAt;
}
