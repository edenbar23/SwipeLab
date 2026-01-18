package com.swipelab.classification.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "credibility_records")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CredibilityRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Decoupled from User and Task entities
    @Column(name = "username", nullable = false)
    private String username;

    @Column(name = "task_id", nullable = false)
    private Long taskId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "image_id", nullable = false)
    private GoldImage goldImage; // "FK to GoldImage"

    @Column(name = "query_species")
    private String querySpecies;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_response", nullable = false)
    private Classification.UserResponse userResponse;

    @Enumerated(EnumType.STRING)
    @Column(name = "correct_answer", nullable = false)
    private GoldImage.UserResponse correctAnswer;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
