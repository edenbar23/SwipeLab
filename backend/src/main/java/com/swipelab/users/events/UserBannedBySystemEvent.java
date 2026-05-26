package com.swipelab.users.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Published by FraudDetectionService when a user's cumulative strike count
 * crosses the auto-ban threshold.
 *
 * Consumed by:
 *   - UserEventListener  → sets status=BANNED, active=false, accountLocked=true
 *   - AdminNotificationService → creates a CRITICAL admin dashboard notification
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserBannedBySystemEvent {

    private String username;

    /** Human-readable reason for the ban (e.g. "Exceeded 15 strikes"). */
    private String reason;

    /** Total accumulated strikes at the moment of the ban. */
    private int totalStrikes;

    private LocalDateTime bannedAt;
}
