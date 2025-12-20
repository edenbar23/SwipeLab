package com.swipelab.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "images")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Image {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String url;

    @Column(nullable = false)
    @Builder.Default
    private Integer priority = 0;

    @Column(name = "is_gold_standard", nullable = false)
    @Builder.Default
    private Boolean isGoldStandard = false;

    // Optional: Correct label if it's a gold standard image
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "correct_label_id")
    private Label correctLabel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
