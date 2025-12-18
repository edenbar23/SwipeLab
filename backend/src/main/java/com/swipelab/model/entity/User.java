package com.swipelab.model.entity;

import com.swipelab.model.enums.AuthProvider;
import com.swipelab.model.enums.UserRole;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(columnNames = "email")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Email
    @Column(nullable = false, unique = true)
    private String email;

    // For local authentication (null for OAuth users)
    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "email_verified", nullable = false)
    private Boolean emailVerified = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuthProvider provider = AuthProvider.LOCAL;

    // External provider's user ID (for OAuth)
    @Column(name = "provider_id")
    private String providerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role = UserRole.USER;

    // For refresh token rotation
    @Column(name = "refresh_token_hash")
    private String refreshTokenHash;

    // Password reset functionality
    @Column(name = "reset_password_token")
    private String resetPasswordToken;

    @Column(name = "reset_token_expiry")
    private LocalDateTime resetTokenExpiry;

    // Email verification token
    @Column(name = "email_verification_token")
    private String emailVerificationToken;

    @Column(name = "verification_token_expiry")
    private LocalDateTime verificationTokenExpiry;

    // User profile fields
    @Column(name = "display_name")
    private String displayName;

    @Column(name = "profile_image_url")
    private String profileImageUrl;

    // Credibility tracking
    @Column(name = "credibility_score")
    private Double credibilityScore = 0.0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    // Account status
    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "account_locked")
    private Boolean accountLocked = false;
}