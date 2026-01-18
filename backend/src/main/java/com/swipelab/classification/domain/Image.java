package com.swipelab.classification.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import com.swipelab.tasks.domain.Task;

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

    // Use src_path as requested, mapped to image_url for consistency or rename
    @Column(name = "src_path", nullable = false)
    private String srcPath;

    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    private String caption;

    @Column(name = "experiment_id")
    private Long experimentId;

    @Column(nullable = false)
    @Builder.Default
    private Integer priority = 0;

    // Removed isGoldStandard and correctLabel from here as they are moved to
    // GoldImage entity
    // or kept if hybrid approach. User requested GoldImage Repository, imply
    // separation.
    // However, existing code uses isGoldStandard. I will deprecate or remove them
    // to force using GoldImage.
    // For now, I'll comment them out to strictly follow the "GoldImage Repository"
    // design which implies a separate table/entity for gold data.

    /*
     * @Column(name = "is_gold_standard", nullable = false)
     * 
     * @Builder.Default
     * private Boolean isGoldStandard = false;
     * 
     * @ManyToOne(fetch = FetchType.LAZY)
     * 
     * @JoinColumn(name = "correct_label_id")
     * private Label correctLabel;
     */

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    // Helper to get ID as image_id
    public Long getImageId() {
        return id;
    }
}
