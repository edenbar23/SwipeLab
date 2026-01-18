package com.swipelab.analytics.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "user_daily_stats", indexes = {
        @Index(name = "idx_uds_user_day", columnList = "user_id, day", unique = true)
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDailyStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "day", nullable = false)
    private LocalDate day;

    @Column(name = "total")
    @Builder.Default
    private Integer total = 0;

    @Column(name = "correct")
    @Builder.Default
    private Integer correct = 0;

    @Column(name = "accuracy")
    @Builder.Default
    private Double accuracy = 0.0;
}
