package com.swipelab.analytics.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_ranking", indexes = {
        @Index(name = "idx_ur_period_user", columnList = "period, user_id", unique = true)
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRanking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "period", nullable = false)
    private String period; // DAILY, WEEKLY, MONTHLY, ALL_TIME

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "rank_val")
    private Integer rank; // 'rank' is SQL keyword

    @Column(name = "accuracy")
    private Double accuracy;

    @Column(name = "percentile")
    private Integer percentile;
}
