package com.swipelab.tasks.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import com.swipelab.users.domain.User;

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
     * We store Users, not usernames.
     * Usernames are DTO-level concerns.
     */
    @BatchSize(size = 50)
    @ManyToMany
    @JoinTable(name = "recipient_group_users", joinColumns = @JoinColumn(name = "group_id"), inverseJoinColumns = @JoinColumn(name = "user_id"))
    @Builder.Default
    private Set<User> users = new HashSet<>();

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

    public void addUser(User user) {
        users.add(user);
    }

    public void removeUser(User user) {
        users.remove(user);
    }

    public void addUsers(Set<User> usersToAdd) {
        users.addAll(usersToAdd);
    }

    public void removeUsers(Set<User> usersToRemove) {
        users.removeAll(usersToRemove);
    }
}
