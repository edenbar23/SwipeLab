package com.swipelab.tasks.domain;

public enum TaskStatus {
    DRAFT,
    PROCESSING,
    ACTIVE,
    PAUSED,
    COMPLETED,
    ARCHIVED,
    /** Experiment crop download failed during async sync. Researcher must retry or check credentials. */
    FAILED
}
