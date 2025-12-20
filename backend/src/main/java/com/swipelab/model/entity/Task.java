package com.swipelab.model.entity;

import com.swipelab.model.enums.TaskStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

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

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private LocalDateTime deadline;

    @Column(name = "min_classifications_per_image")
    @Builder.Default
    private Integer minClassificationsPerImage = 3;

    @Column(name = "consensus_threshold")
    @Builder.Default
    private Double consensusThreshold = 80.0;

    @ElementCollection
    @CollectionTable(name = "task_experiments", joinColumns = @JoinColumn(name = "task_id"))
    @Column(name = "experiment_id")
    @Builder.Default
    private List<Long> experiments = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "task_recipient_groups", joinColumns = @JoinColumn(name = "task_id"))
    @Column(name = "recipient_group_id")
    @Builder.Default
    private List<Long> recipientGroups = new ArrayList<>();

    @ManyToMany
    @JoinTable(name = "task_target_species", joinColumns = @JoinColumn(name = "task_id"), inverseJoinColumns = @JoinColumn(name = "label_id"))
    @Builder.Default
    private List<Label> targetSpecies = new ArrayList<>();

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private TaskStatus status = TaskStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Image> images = new ArrayList<>();

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
