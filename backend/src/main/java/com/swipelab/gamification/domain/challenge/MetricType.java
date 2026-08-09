package com.swipelab.gamification.domain.challenge;

public enum MetricType {
    CLASSIFICATION,
    XP_GAINED,
    LOGIN,
    TASK_COMPLETED,
    // Reports the user's current streak length (absolute value, not incremental).
    STREAK
}

