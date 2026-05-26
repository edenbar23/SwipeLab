package com.swipelab.users.domain;

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

import com.swipelab.auth.infrastructure.AuthProvider;

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
        private com.swipelab.model.enums.UserStatus status = com.swipelab.model.enums.UserStatus.PENDING_VERIFICATION;

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
         * Cohen's Kappa score showing agreement with expert (RESEARCHER)
         * classifications
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

        // ========== GAMIFICATION CACHE ==========

        @Column(name = "score")
        @Builder.Default
        private Long score = 0L;

        @Column(name = "badges")
        private String badges;

        @Column(name = "rank_level")
        @Builder.Default
        private String rank = "UNRANKED";

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

        @Column(name = "is_flagged", nullable = false)
        @Builder.Default
        private Boolean isFlagged = false;

        // ========== SUSPICIOUS ACTIVITY TRACKING ==========

        /**
         * Total number of fraud-detection warnings issued to this user.
         * Incremented on WARNING_1 and WARNING_2 events.
         */
        @Column(name = "warning_count", nullable = false)
        @Builder.Default
        private Integer warningCount = 0;

        /**
         * Cumulative strike count. Each sliding-window violation increments this.
         * Drives the WARNING_1 → WARNING_2 → BAN escalation ladder.
         */
        @Column(name = "strike_count", nullable = false)
        @Builder.Default
        private Integer strikeCount = 0;

        /** Timestamp of the most recent warning issued to this user. */
        @Column(name = "last_warning_at")
        private LocalDateTime lastWarningAt;

        /**
         * Consecutive correct gold-image answers since the last warning or reset.
         * Used for organic recovery: reaching the configured threshold reduces strikeCount
         * and may restore ACTIVE status.
         */
        @Column(name = "consecutive_correct_golds", nullable = false)
        @Builder.Default
        private Integer consecutiveCorrectGolds = 0;



        public Double getCredibilityScore() {
                return credibilityScore;
        }

        public void setCredibilityScore(Double credibilityScore) {
                this.credibilityScore = credibilityScore;
        }

        public Double getAgreementWithExperts() {
                return agreementWithExperts;
        }

        public void setAgreementWithExperts(Double agreementWithExperts) {
                this.agreementWithExperts = agreementWithExperts;
        }

        public Double getMajorityAgreementScore() {
                return majorityAgreementScore;
        }

        public void setMajorityAgreementScore(Double majorityAgreementScore) {
                this.majorityAgreementScore = majorityAgreementScore;
        }

        public Integer getTotalClassifications() {
                return totalClassifications;
        }

        public void setTotalClassifications(Integer totalClassifications) {
                this.totalClassifications = totalClassifications;
        }

        public Integer getCorrectGoldClassifications() {
                return correctGoldClassifications;
        }

        public void setCorrectGoldClassifications(Integer correctGoldClassifications) {
                this.correctGoldClassifications = correctGoldClassifications;
        }

        public Integer getTotalGoldClassifications() {
                return totalGoldClassifications;
        }

        public void setTotalGoldClassifications(Integer totalGoldClassifications) {
                this.totalGoldClassifications = totalGoldClassifications;
        }

        public Long getScore() {
                return score;
        }

        public void setScore(Long score) {
                this.score = score;
        }

        public String getBadges() {
                return badges;
        }

        public void setBadges(String badges) {
                this.badges = badges;
        }

        public String getRank() {
                return rank;
        }

        public void setRank(String rank) {
                this.rank = rank;
        }
}