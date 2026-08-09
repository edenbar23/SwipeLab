package com.swipelab.gamification.domain.challenge;

public enum AggregationType {
    COUNT,
    DISTINCT_COUNT,
    SUM,
    // Progress is SET to the reported amount (absolute current value), not accumulated.
    // Used for metrics like streak length or total points where each report is the full value.
    LATEST
}

