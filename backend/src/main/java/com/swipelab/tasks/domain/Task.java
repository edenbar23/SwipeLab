package com.swipelab.tasks.domain;

import com.swipelab.tasks.domain.TaskStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import com.swipelab.users.domain.User;
import com.swipelab.classification.domain.Label;
import com.swipelab.classification.domain.Image;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tasks")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Task {

    // =========================
    // Identifiers & Metadata
    // =========================

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    private String description;

    @Column(name = "query_species")
    private String querySpecies;

    @Column(name = "question")
    private String question;

    private LocalDateTime deadline;

    // =========================
    // Task Configuration
    // =========================

    @Column(name = "min_classifications_per_image", nullable = false)
    @Builder.Default
    private Integer minClassificationsPerImage = 3;

    @Column(name = "consensus_threshold", nullable = false)
    @Builder.Default
    private Double consensusThreshold = 80.0;

    // =========================
    // Relationships
    // =========================

    @BatchSize(size = 20)
    @ElementCollection
    @CollectionTable(name = "task_experiments", joinColumns = @JoinColumn(name = "task_id"))
    @Column(name = "experiment_id")
    @Builder.Default
    private List<Long> experiments = new ArrayList<>();

    @BatchSize(size = 20)
    @ElementCollection
    @CollectionTable(name = "task_recipient_groups", joinColumns = @JoinColumn(name = "task_id"))
    @Column(name = "recipient_group_id")
    @Builder.Default
    private List<Long> recipientGroups = new ArrayList<>();

    @BatchSize(size = 20)
    @ElementCollection
    @CollectionTable(name = "task_assigned_users", joinColumns = @JoinColumn(name = "task_id"))
    @Column(name = "username")
    @Builder.Default
    private List<String> assignedUsernames = new ArrayList<>();

    @Column(name = "is_public", nullable = false)
    @Builder.Default
    private Boolean isPublic = false;

    @BatchSize(size = 20)
    @ManyToMany
    @JoinTable(name = "task_target_species", joinColumns = @JoinColumn(name = "task_id"), inverseJoinColumns = @JoinColumn(name = "label_id"))
    @Builder.Default
    private List<Label> targetSpecies = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Image> images = new ArrayList<>();

    // =========================
    // Status & Lifecycle
    // =========================

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private TaskStatus status = TaskStatus.DRAFT;

    // =========================
    // Auditing
    // =========================

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // =========================
    // Domain Logic (State Machine)
    // =========================

    /**
     * Allowed:
     * DRAFT -> ACTIVE
     * PAUSED -> ACTIVE
     */
    public void activate() {
        if (status == TaskStatus.DRAFT || status == TaskStatus.PAUSED) {
            this.status = TaskStatus.ACTIVE;
        } else {
            throw new IllegalStateException(
                    "Cannot activate task from status: " + status);
        }
    }

    /**
     * Allowed:
     * ACTIVE -> PAUSED
     */
    public void pause() {
        if (status == TaskStatus.ACTIVE) {
            this.status = TaskStatus.PAUSED;
        } else {
            throw new IllegalStateException(
                    "Only ACTIVE tasks can be paused");
        }
    }

    /**
     * Allowed:
     * ACTIVE -> ARCHIVED
     * PAUSED -> ARCHIVED
     */
    public void archive() {
        if (status == TaskStatus.ACTIVE || status == TaskStatus.PAUSED) {
            this.status = TaskStatus.ARCHIVED;
        } else {
            throw new IllegalStateException(
                    "Cannot archive task from status: " + status);
        }
    }

    // =========================
    // Convenience / Safety
    // =========================

    public boolean isActive() {
        return status == TaskStatus.ACTIVE;
    }

    public boolean isArchived() {
        return status == TaskStatus.ARCHIVED;
    }
}
