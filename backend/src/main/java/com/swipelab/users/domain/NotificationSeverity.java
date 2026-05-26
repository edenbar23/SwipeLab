package com.swipelab.users.domain;

/**
 * Severity of an AdminNotification.
 * INFO     – informational, no action required.
 * WARNING  – suspicious activity, admin should review.
 * CRITICAL – immediate action required (e.g. auto-ban fired).
 */
public enum NotificationSeverity {
    INFO,
    WARNING,
    CRITICAL
}
