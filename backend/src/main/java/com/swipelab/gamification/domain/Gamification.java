package com.swipelab.gamification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "gamification")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Gamification {

    @Id
    @Column(nullable = false, unique = true)
    private String username;

    // Streaks
    @Column(name = "start_streak")
    private LocalDateTime startStreak;

    @Column(name = "end_streak")
    private LocalDateTime endStreak;

    @Column(name = "current_streak")
    @Builder.Default
    private Integer currentStreak = 0;

    @Column(name = "longest_streak")
    @Builder.Default
    private Integer longestStreak = 0;

    // Score (formerly points)
    @Column(name = "score")
    @Builder.Default
    private Long score = 0L;

    // Badges (Comma separated string as requested)
    @Column(name = "badge")
    private String badge;

    // Rank (Level based on challenges)
    @Column(name = "rank_level")
    @Builder.Default
    private String rank = "UNRANKED";

    public Long getScore() {
        return score;
    }

}
