package com.swipelab.classification.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "gold_images")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoldImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "image_id", nullable = false, unique = true)
    private Image image;

    @Column(nullable = false)
    private String species;

    @Enumerated(EnumType.STRING)
    @Column(name = "correct_answer", nullable = false)
    private UserResponse correctAnswer;

    public enum UserResponse {
        YES, NO, DONT_KNOW, TRASH
    }
}
