package com.swipelab.gamification.badge;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "badge_award", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"username", "challenge_definition_id", "window_start"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BadgeAward {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String username;

    @Column(name = "badge_id", nullable = false)
    private UUID badgeId;

    @Column(name = "challenge_definition_id", nullable = false)
    private UUID challengeDefinitionId;

    @Column(name = "window_start", nullable = false)
    private LocalDateTime windowStart;

    @Column(name = "window_end", nullable = false)
    private LocalDateTime windowEnd;

    @Column(name = "awarded_at", nullable = false, updatable = false)
    private LocalDateTime awardedAt;

    @PrePersist
    protected void onAwarded() {
        if (awardedAt == null) {
            awardedAt = LocalDateTime.now();
        }
    }
}
