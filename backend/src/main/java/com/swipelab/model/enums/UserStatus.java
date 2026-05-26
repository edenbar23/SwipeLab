package com.swipelab.model.enums;

public enum UserStatus {
    PENDING_VERIFICATION,
    PENDING_INVITE,
    ACTIVE,
    /** User received a fraud detection warning but can still label. Credibility weight is halved. */
    WARNED,
    BANNED
}
