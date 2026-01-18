package com.swipelab.recipients.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "recipient_groups", uniqueConstraints = {
        @UniqueConstraint(columnNames = "name")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecipientGroup {

    // =========================
    // Identifier
    // =========================

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // =========================
    // Group Metadata
    // =========================

    @Column(nullable = false, unique = true)
    private String name;

    // =========================
    // Members
    // =========================

    /**
     * We store RecipientUsers (which only hold username)
     */
    @BatchSize(size = 50)
    @ManyToMany
    @JoinTable(name = "recipient_group_users", joinColumns = @JoinColumn(name = "group_id"), inverseJoinColumns = @JoinColumn(name = "username"))
    @Builder.Default
    private Set<RecipientUser> users = new HashSet<>();

    // =========================
    // Auditing
    // =========================

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // =========================
    // Domain Logic
    // =========================

    public int getUserCount() {
        return users.size();
    }

    public void addUser(RecipientUser user) {
        users.add(user);
    }

    public void removeUser(RecipientUser user) {
        users.remove(user);
    }

    public void addUsers(Set<RecipientUser> usersToAdd) {
        users.addAll(usersToAdd);
    }

    public void removeUsers(Set<RecipientUser> usersToRemove) {
        users.removeAll(usersToRemove);
    }
}
