package com.swipelab.users.domain;

/**
 * Categories of events that produce an AdminNotification record.
 * Used for filtering and icon selection in the admin dashboard.
 */
public enum NotificationType {
    SUSPICIOUS_ACTIVITY,
    USER_WARNED,
    USER_BANNED,
    USER_RECOVERED,
    SYSTEM_ALERT
}
