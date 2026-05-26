package com.swipelab.analytics.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "task_daily_stats", indexes = {
        @Index(name = "idx_tds_task_day", columnList = "task_id, `day`", unique = true)
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskDailyStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_id", nullable = false)
    private Long taskId;

    @Column(name = "`day`", nullable = false)
    private LocalDate day;

    @Column(name = "classifications")
    @Builder.Default
    private Integer classifications = 0;

    @Column(name = "completed_images")
    @Builder.Default
    private Integer completedImages = 0;

    @Column(name = "consensus_reached")
    @Builder.Default
    private Integer consensusReached = 0;
}
