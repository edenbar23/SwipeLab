package com.swipelab.classification.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "classifications")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Classification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Decoupled from User entity
    @Column(name = "username", nullable = false)
    private String username;

    @Column(name = "user_role")
    private String userRole; // Stores the role at the time of classification

    // Decoupled from Task entity
    @Column(name = "task_id", nullable = false)
    private Long taskId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "image_id", nullable = false)
    private Image image;

    @Column(name = "query_species")
    private String querySpecies;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_response", nullable = false)
    private UserResponse userResponse;

    // Kept label for compatibility if needed, but schema says user_response ENUM
    // Schema: user_response -- ENUM: YES, NO, DON'T KNOW, TRASH
    // I previously had `Label label`. I will comment it out or adapt it.
    // The request implies switching to simpler ENUM response.

    /*
     * @ManyToOne(fetch = FetchType.LAZY)
     * 
     * @JoinColumn(name = "label_id", nullable = false)
     * private Label label;
     */

    // Adding Label back purely to satisfy existing code for now?
    // No, I should stick to the requested schema.
    // If I break existing code, I must fix it.
    // The previous Label entity might be obsolete or just reference data.
    // The prompt says "user_response -- ENUM".

    // I will add the Enum definition here or import it if shared.
    // GoldImage has it too. I should probably move it to a shared place or define
    // it here.
    // For now, defining it here to match GoldImage.UserResponse or reusing it.
    // Better to define it in a separate file or reuse logic.
    // I'll assume same ENUM as GoldImage.

    public enum UserResponse {
        YES, NO, DONT_KNOW, TRASH
    }

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
