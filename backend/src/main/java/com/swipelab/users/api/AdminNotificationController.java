package com.swipelab.users.api;

import com.swipelab.dto.response.AdminNotificationResponse;
import com.swipelab.users.application.AdminNotificationService;
import com.swipelab.users.domain.NotificationSeverity;
import com.swipelab.users.domain.NotificationType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Admin-only endpoints for managing system-generated notifications.
 *
 * All routes require Super Admin — Researchers do not have access to
 * the notification feed (they have a separate suspicious-activity audit log).
 *
 * GET  /api/admin/notifications              – paginated list (optional filters)
 * GET  /api/admin/notifications/unread-count – badge counter for the UI
 * PATCH /api/admin/notifications/{id}/read   – mark one as read
 * PATCH /api/admin/notifications/read-all    – dismiss all
 */
@RestController
@RequestMapping("/api/admin/notifications")
@PreAuthorize("@securityAuthorizationService.isSuperAdmin(authentication.name)")
@RequiredArgsConstructor
public class AdminNotificationController {

    private final AdminNotificationService adminNotificationService;

    /**
     * Returns paginated notifications.
     *
     * Optional query params:
     *   isRead   – true/false
     *   type     – matches NotificationType enum (USER_WARNED, USER_BANNED, …)
     *   severity – matches NotificationSeverity enum (INFO, WARNING, CRITICAL)
     *   page     – zero-based page index (default 0)
     *   size     – page size (default 20)
     */
    @GetMapping
    public ResponseEntity<Page<AdminNotificationResponse>> getNotifications(
            @RequestParam(required = false) Boolean isRead,
            @RequestParam(required = false) NotificationType type,
            @RequestParam(required = false) NotificationSeverity severity,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(
                adminNotificationService.getNotifications(isRead, type, severity, pageable));
    }

    /** Returns the count of unread notifications — used for the bell badge in the UI. */
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount() {
        return ResponseEntity.ok(Map.of("unreadCount", adminNotificationService.getUnreadCount()));
    }

    /** Marks a single notification as read. */
    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long id) {
        adminNotificationService.markAsRead(id);
        return ResponseEntity.noContent().build();
    }

    /** Marks all unread notifications as read in a single batch UPDATE. */
    @PatchMapping("/read-all")
    public ResponseEntity<Map<String, Integer>> markAllAsRead() {
        int updated = adminNotificationService.markAllAsRead();
        return ResponseEntity.ok(Map.of("updated", updated));
    }
}
