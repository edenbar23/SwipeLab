package com.swipelab.users.events;

import com.swipelab.classification.domain.WarningLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Published by FraudDetectionService when a user's cumulative strike count
 * crosses the WARNING_1 or WARNING_2 threshold.
 *
 * Consumed by:
 *   - UserEventListener  → updates user status + credibility penalty
 *   - AdminNotificationService → creates an admin dashboard notification
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserWarnedEvent {

    private String username;

    /** WARNING_1 (first offence) or WARNING_2 (final warning before ban). */
    private WarningLevel level;

    /** Human-readable reason for logging and the admin notification message. */
    private String reason;

    /** Strike count at the moment this warning was issued. */
    private int strikeCount;

    /** How many more strikes until an automatic ban is triggered. */
    private int strikesUntilBan;

    private LocalDateTime detectedAt;
}
