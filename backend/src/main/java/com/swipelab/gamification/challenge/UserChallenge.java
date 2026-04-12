package com.swipelab.gamification.challenge;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_challenge", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"username", "challenge_definition_id", "window_start"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserChallenge {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String username;

    // Use an association directly for ease of engine operations
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "challenge_definition_id", nullable = false)
    private ChallengeDefinition definition;

    @Column(name = "current_progress", nullable = false)
    private int currentProgress;

    @Column(nullable = false)
    private boolean completed;

    @Column(name = "window_start", nullable = false)
    private LocalDateTime windowStart;

    @Column(name = "window_end", nullable = false)
    private LocalDateTime windowEnd;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
