package com.swipelab.users.application;

import com.swipelab.classification.domain.WarningLevel;
import com.swipelab.users.domain.AdminNotification;
import com.swipelab.users.domain.NotificationSeverity;
import com.swipelab.users.domain.NotificationType;
import com.swipelab.users.events.UserBannedBySystemEvent;
import com.swipelab.users.events.UserWarnedEvent;
import com.swipelab.users.infrastructure.AdminNotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Translates fraud-detection domain events into persistent AdminNotification records
 * surfaced on the Super Admin dashboard.
 *
 * Notification severity mapping:
 *   WARNING_1  → WARNING   (review recommended)
 *   WARNING_2  → CRITICAL  (final warning, action likely needed)
 *   BAN        → CRITICAL  (system has already acted)
 *   Recovery   → INFO      (positive outcome, no action required)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminNotificationService {

    private final AdminNotificationRepository adminNotificationRepository;

    // ── Warning notifications ─────────────────────────────────────────────────

    @Async
    @EventListener
    @Transactional
    public void onUserWarned(UserWarnedEvent event) {
        boolean isFinal = event.getLevel() == WarningLevel.WARNING_2;

        String title = isFinal
                ? "⚠️ Final warning issued: " + event.getUsername()
                : "⚠️ Suspicious labeler warned: " + event.getUsername();

        String message = String.format(
                "User \"%s\" received %s (cumulative strikes: %d). " +
                        "%d more strike(s) will trigger an automatic ban. Reason: %s",
                event.getUsername(),
                event.getLevel(),
                event.getStrikeCount(),
                event.getStrikesUntilBan(),
                event.getReason());

        persist(NotificationType.USER_WARNED,
                isFinal ? NotificationSeverity.CRITICAL : NotificationSeverity.WARNING,
                title,
                message,
                event.getUsername());

        log.info("Admin notification created for {} warning on user {}",
                event.getLevel(), event.getUsername());
    }

    // ── Ban notifications ─────────────────────────────────────────────────────

    @Async
    @EventListener
    @Transactional
    public void onUserBannedBySystem(UserBannedBySystemEvent event) {
        String title = "🚫 User auto-banned: " + event.getUsername();

        String message = String.format(
                "User \"%s\" was automatically banned by the system after accumulating %d strikes. " +
                        "Reason: %s. Banned at: %s",
                event.getUsername(),
                event.getTotalStrikes(),
                event.getReason(),
                event.getBannedAt());

        persist(NotificationType.USER_BANNED,
                NotificationSeverity.CRITICAL,
                title,
                message,
                event.getUsername());

        log.warn("Admin notification created for system ban of user {}", event.getUsername());
    }

    // ── Recovery notifications ────────────────────────────────────────────────

    /**
     * Called by WarningRecoveryService when a user's status is restored to ACTIVE.
     * Kept as a direct method (not event-driven) since recovery is triggered synchronously
     * inside GoldImageEvaluatorService's flow.
     */
    public void notifyUserRecovered(String username, int remainingStrikes) {
        String title = "✅ User recovered: " + username;

        String message = String.format(
                "User \"%s\" has regained ACTIVE status after sustained correct gold-image answers. " +
                        "Remaining strikes: %d.",
                username, remainingStrikes);

        persist(NotificationType.USER_RECOVERED,
                NotificationSeverity.INFO,
                title,
                message,
                username);

        log.info("Admin notification created for recovery of user {}", username);
    }

    // ─────────────────────────────────────────────────────────────────────────

    // ── Query methods (used by AdminNotificationController) ───────────────────

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public org.springframework.data.domain.Page<com.swipelab.dto.response.AdminNotificationResponse> getNotifications(
            Boolean isRead,
            com.swipelab.users.domain.NotificationType type,
            com.swipelab.users.domain.NotificationSeverity severity,
            org.springframework.data.domain.Pageable pageable) {

        org.springframework.data.domain.Page<AdminNotification> page;

        if (type != null) {
            page = adminNotificationRepository.findByTypeOrderByCreatedAtDesc(type, pageable);
        } else if (severity != null) {
            page = adminNotificationRepository.findBySeverityOrderByCreatedAtDesc(severity, pageable);
        } else if (isRead != null) {
            page = adminNotificationRepository.findByIsReadOrderByCreatedAtDesc(isRead, pageable);
        } else {
            page = adminNotificationRepository.findAllByOrderByCreatedAtDesc(pageable);
        }

        return page.map(this::toResponse);
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public long getUnreadCount() {
        return adminNotificationRepository.countByIsRead(false);
    }

    @org.springframework.transaction.annotation.Transactional
    public AdminNotification markAsRead(Long id) {
        AdminNotification notification = adminNotificationRepository.findById(id)
                .orElseThrow(() -> new com.swipelab.exception.ResourceNotFoundException(
                        "Notification not found: " + id));
        notification.setIsRead(true);
        return adminNotificationRepository.save(notification);
    }

    @org.springframework.transaction.annotation.Transactional
    public int markAllAsRead() {
        return adminNotificationRepository.markAllAsRead();
    }

    private com.swipelab.dto.response.AdminNotificationResponse toResponse(AdminNotification n) {
        return com.swipelab.dto.response.AdminNotificationResponse.builder()
                .id(n.getId())
                .type(n.getType())
                .severity(n.getSeverity())
                .title(n.getTitle())
                .message(n.getMessage())
                .targetUsername(n.getTargetUsername())
                .isRead(n.getIsRead())
                .createdAt(n.getCreatedAt())
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────

    private void persist(NotificationType type, NotificationSeverity severity,
                         String title, String message, String targetUsername) {
        adminNotificationRepository.save(AdminNotification.builder()
                .type(type)
                .severity(severity)
                .title(title)
                .message(message)
                .targetUsername(targetUsername)
                .isRead(false)
                .build());
    }
}
