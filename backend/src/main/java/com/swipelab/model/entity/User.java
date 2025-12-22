package com.swipelab.model.entity;

import com.swipelab.model.enums.AuthProvider;
import com.swipelab.model.enums.UserRole;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(columnNames = "username"),
        @UniqueConstraint(columnNames = "email")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

        /**
         * Primary identifier (used in JWT subject)
         */
        @Id
        @NotBlank
        @Column(nullable = false, updatable = false)
        private String username;

        /**
         * Email is unique but NOT the primary key
         */
        @NotBlank
        @Email
        @Column(nullable = false)
        private String email;

        // For local authentication (null for OAuth users)
        @Column(name = "password_hash")
        private String passwordHash;

        @Column(name = "email_verified", nullable = false)
        @Builder.Default
        private Boolean emailVerified = false;

        @Enumerated(EnumType.STRING)
        @Column(nullable = false)
        @Builder.Default
        private AuthProvider provider = AuthProvider.LOCAL;

        // External provider's user ID (Google sub)
        @Column(name = "provider_id")
        private String providerId;

        @Enumerated(EnumType.STRING)
        @Column(nullable = false)
        @Builder.Default
        private UserRole role = UserRole.USER;

        // Refresh token rotation (hashed)
        @Column(name = "refresh_token_hash")
        private String refreshTokenHash;

        // Password reset
        @Column(name = "reset_password_token")
        private String resetPasswordToken;

        @Column(name = "reset_token_expiry")
        private LocalDateTime resetTokenExpiry;

        // Email verification
        @Column(name = "email_verification_token")
        private String emailVerificationToken;

        @Column(name = "verification_token_expiry")
        private LocalDateTime verificationTokenExpiry;

        // Profile
        @Column(name = "display_name")
        private String displayName;

        @Column(name = "profile_image_url")
        private String profileImageUrl;

        // ========== CREDIBILITY TRACKING ==========

        /**
         * Overall credibility score (can be used for weighted averaging later)
         */
        @Column(name = "credibility_score", nullable = false)
        @Builder.Default
        private Double credibilityScore = 0.0;

        /**
         * Cohen's Kappa score showing agreement with expert (RESEARCHER) classifications
         * Range: -1 to 1, where 1 is perfect agreement
         */
        @Column(name = "agreement_with_experts")
        @Builder.Default
        private Double agreementWithExperts = 0.0;

        /**
         * Score showing how often user agrees with majority vote
         * Range: 0 to 1, where 1 means always agrees with majority
         */
        @Column(name = "majority_agreement_score")
        @Builder.Default
        private Double majorityAgreementScore = 0.0;

        /**
         * Total number of classifications submitted by this user
         */
        @Column(name = "total_classifications")
        @Builder.Default
        private Integer totalClassifications = 0;

        /**
         * Number of correct classifications on gold standard images
         * (to be implemented when gold image feature is added)
         */
        @Column(name = "correct_gold_classifications")
        @Builder.Default
        private Integer correctGoldClassifications = 0;

        /**
         * Total number of gold standard images classified
         * (to be implemented when gold image feature is added)
         */
        @Column(name = "total_gold_classifications")
        @Builder.Default
        private Integer totalGoldClassifications = 0;

        // ========================================

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
        @Builder.Default
        private Boolean active = true;

        @Column(name = "account_locked", nullable = false)
        @Builder.Default
        private Boolean accountLocked = false;
}