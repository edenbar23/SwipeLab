package com.swipelab.classification.domain;

import com.swipelab.auth.application.SecurityAuthorizationService;
import com.swipelab.classification.infrastructure.SuspiciousActivityRepository;
import com.swipelab.classification.dto.api.ClassificationWarningDto;
import com.swipelab.model.enums.UserRole;
import com.swipelab.users.domain.User;
import com.swipelab.users.events.UserBannedBySystemEvent;
import com.swipelab.users.events.UserWarnedEvent;
import com.swipelab.users.infrastructure.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Analyses each submitted classification for suspicious behaviour.
 *
 * Strategy: sliding-window accumulator
 * ─────────────────────────────────────
 * 1. A single fast response is NOT a strike — the user may just know the answer.
 * 2. Only when N fast responses occur within the configured window does the
 *    system record a STRIKE and increment the user's cumulative strike counter.
 * 3. The cumulative counter drives a 3-level escalation ladder:
 *      STRIKE (silent) → WARNING_1 → WARNING_2 → BAN (automated)
 *
 * Role-aware thresholds:
 *   Researchers are domain experts who legitimately respond faster.
 *   They use a lower min-response-time-ms to avoid false positives.
 *
 * Super Admin is fully immune from fraud detection.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FraudDetectionService {

    private final ApplicationEventPublisher eventPublisher;
    private final SuspiciousActivityRepository suspiciousActivityRepository;
    private final UserRepository userRepository;
    private final SecurityAuthorizationService securityAuthorizationService;

    // ── Thresholds (bound from application.yml) ───────────────────────────────

    @Value("${app.fraud-detection.min-response-time-ms:300}")
    private long minResponseTimeMs;

    @Value("${app.fraud-detection.researcher-min-response-time-ms:150}")
    private long researcherMinResponseTimeMs;

    @Value("${app.fraud-detection.suspicious-count-for-strike:3}")
    private int suspiciousCountForStrike;

    @Value("${app.fraud-detection.sliding-window-minutes:10}")
    private int slidingWindowMinutes;

    @Value("${app.fraud-detection.strikes-for-warning-1:5}")
    private int strikesForWarning1;

    @Value("${app.fraud-detection.strikes-for-warning-2:10}")
    private int strikesForWarning2;

    @Value("${app.fraud-detection.strikes-for-ban:15}")
    private int strikesForBan;

    @Value("${app.fraud-detection.warning-cooldown-minutes:30}")
    private int warningCooldownMinutes;

    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Entry point called by ClassificationService after every submission.
     *
     * @param username       the submitting user
     * @param userRole       role string matching the UserRole enum name
     * @param responseTimeMs client-measured time from image display to swipe (ms)
     * @param taskId         task being classified (for the audit record)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public FraudAnalysisResult analyzeClassification(String username, String userRole,
                                      long responseTimeMs, Long taskId) {

        // Super Admin is fully immune — never flag platform operators.
        if (securityAuthorizationService.isSuperAdmin(username)) {
            log.debug("Fraud detection skipped for super admin: {}", username);
            return new FraudAnalysisResult(false, null);
        }

        // Pick the response-time threshold based on role.
        // Researchers are domain experts and legitimately swipe faster.
        boolean isResearcher = UserRole.RESEARCHER.name().equalsIgnoreCase(userRole);
        long threshold = isResearcher ? researcherMinResponseTimeMs : minResponseTimeMs;

        if (responseTimeMs >= threshold) {
            return new FraudAnalysisResult(false, null); // Response time is within acceptable range — nothing to do.
        }

        log.debug("Fast response: user={}, role={}, {}ms < threshold {}ms",
                username, userRole, responseTimeMs, threshold);

        // Persist raw suspicious event for the audit trail (severity=STRIKE).
        persistRecord(username, taskId, responseTimeMs,
                "Fast response: " + responseTimeMs + "ms (threshold " + threshold + "ms)",
                WarningLevel.STRIKE);

        // Count raw suspicious events for this user within the sliding window.
        LocalDateTime windowStart = LocalDateTime.now().minusMinutes(slidingWindowMinutes);
        long recentCount = suspiciousActivityRepository
                .countByUsernameAndSeverityAndCreatedAtAfter(username, WarningLevel.STRIKE, windowStart);

        if (recentCount < suspiciousCountForStrike) {
            log.debug("User {} has {} suspicious events in window (need {}); silent accumulation",
                    username, recentCount, suspiciousCountForStrike);
            return new FraudAnalysisResult(false, null);
        }

        // Window threshold crossed — escalate to a formal STRIKE.
        return escalate(username, taskId, responseTimeMs, threshold);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Increments the user's cumulative strike count and determines which
     * event (if any) to publish on the escalation ladder.
     */
    private FraudAnalysisResult escalate(String username, Long taskId, long responseTimeMs, long threshold) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException(
                        "User not found during fraud escalation: " + username));

        int newStrikeCount = (user.getStrikeCount() == null ? 0 : user.getStrikeCount()) + 1;
        user.setStrikeCount(newStrikeCount);
        userRepository.save(user);

        log.info("Strike #{} recorded for user {} (task={})", newStrikeCount, username, taskId);

        String reason = String.format(
                "Accumulated %d strikes — latest fast response: %dms (threshold %dms)",
                newStrikeCount, responseTimeMs, threshold);

        if (newStrikeCount >= strikesForBan) {
            publishBan(username, reason, newStrikeCount);
            return new FraudAnalysisResult(true, null);
        } else if (newStrikeCount >= strikesForWarning2 && (user.getWarningCount() == null || user.getWarningCount() < 2)) {
            ClassificationWarningDto warning = publishWarning(user, username, WarningLevel.WARNING_2, reason, newStrikeCount);
            return new FraudAnalysisResult(false, warning);
        } else if (newStrikeCount >= strikesForWarning1 && (user.getWarningCount() == null || user.getWarningCount() < 1)) {
            ClassificationWarningDto warning = publishWarning(user, username, WarningLevel.WARNING_1, reason, newStrikeCount);
            return new FraudAnalysisResult(false, warning);
        } else {
            log.debug("Strike #{} for user {} — below warning threshold, silent", newStrikeCount, username);
            return new FraudAnalysisResult(false, null);
        }
    }

    private ClassificationWarningDto publishWarning(User user, String username, WarningLevel level,
                                String reason, int strikeCount) {
        // Respect the cooldown window to avoid spamming warnings in a single bad session.
        if (user.getLastWarningAt() != null) {
            LocalDateTime cooldownEnd = user.getLastWarningAt().plusMinutes(warningCooldownMinutes);
            if (LocalDateTime.now().isBefore(cooldownEnd)) {
                log.debug("Warning suppressed for {} — within cooldown until {}", username, cooldownEnd);
                return null;
            }
        }

        int strikesUntilBan = Math.max(0, strikesForBan - strikeCount);

        log.warn("Issuing {} to user {} (strikes={}, strikesUntilBan={})",
                level, username, strikeCount, strikesUntilBan);

        eventPublisher.publishEvent(UserWarnedEvent.builder()
                .username(username)
                .level(level)
                .reason(reason)
                .strikeCount(strikeCount)
                .strikesUntilBan(strikesUntilBan)
                .detectedAt(LocalDateTime.now())
                .build());

        return ClassificationWarningDto.builder()
                .level(level.name())
                .message(reason)
                .strikeCount(strikeCount)
                .strikesUntilBan(strikesUntilBan)
                .build();
    }

    private void publishBan(String username, String reason, int totalStrikes) {
        log.warn("Auto-banning user {} after {} strikes", username, totalStrikes);

        eventPublisher.publishEvent(UserBannedBySystemEvent.builder()
                .username(username)
                .reason(reason)
                .totalStrikes(totalStrikes)
                .bannedAt(LocalDateTime.now())
                .build());
    }

    private void persistRecord(String username, Long taskId, long responseTimeMs,
                               String reason, WarningLevel severity) {
        suspiciousActivityRepository.save(SuspiciousActivityRecord.builder()
                .username(username)
                .taskId(taskId)
                .responseTimeMs(responseTimeMs)
                .reason(reason)
                .severity(severity)
                .build());
    }
}