package com.swipelab.users.application;

import com.swipelab.classification.domain.WarningLevel;
import com.swipelab.model.enums.UserStatus;
import com.swipelab.users.domain.User;
import com.swipelab.users.events.UserBannedBySystemEvent;
import com.swipelab.users.events.UserStatusChangedEvent;
import com.swipelab.users.events.UserWarnedEvent;
import com.swipelab.users.infrastructure.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Reacts to fraud-detection events and mutates the User aggregate accordingly.
 *
 * onUserWarned      – applies credibility penalty + sets WARNED status
 * onUserBannedBySystem – sets BANNED + publishes downstream UserStatusChangedEvent
 *
 * Both handlers are @Async so they never block the labeling request thread.
 * @Transactional ensures the user row update is atomic.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserEventListener {

    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${app.fraud-detection.credibility-penalty-warning-1:5.0}")
    private double penaltyWarning1;

    @Value("${app.fraud-detection.credibility-penalty-warning-2:15.0}")
    private double penaltyWarning2;

    // ── Warning handler ───────────────────────────────────────────────────────

    @Async
    @EventListener
    @Transactional
    public void onUserWarned(UserWarnedEvent event) {
        log.info("Processing UserWarnedEvent: user={}, level={}, strikes={}",
                event.getUsername(), event.getLevel(), event.getStrikeCount());

        User user = findUser(event.getUsername());

        // Increment warning counter and record timestamp (used for cooldown).
        user.setWarningCount((user.getWarningCount() == null ? 0 : user.getWarningCount()) + 1);
        user.setLastWarningAt(LocalDateTime.now());
        user.setStatus(UserStatus.WARNED);

        // Reset the recovery counter — a warning resets any progress toward recovery.
        user.setConsecutiveCorrectGolds(0);

        // Apply credibility penalty: WARNING_2 is more severe.
        double penalty = event.getLevel() == WarningLevel.WARNING_1 ? penaltyWarning1 : penaltyWarning2;
        double currentScore = user.getCredibilityScore() != null ? user.getCredibilityScore() : 0.0;
        user.setCredibilityScore(Math.max(0.0, currentScore - penalty));

        userRepository.save(user);

        log.warn("User {} warned ({}): credibility reduced by {}, new score={}",
                event.getUsername(), event.getLevel(), penalty, user.getCredibilityScore());
    }

    // ── System-ban handler ────────────────────────────────────────────────────

    @Async
    @EventListener
    @Transactional
    public void onUserBannedBySystem(UserBannedBySystemEvent event) {
        log.warn("Processing UserBannedBySystemEvent: user={}, strikes={}",
                event.getUsername(), event.getTotalStrikes());

        User user = findUser(event.getUsername());

        user.setStatus(UserStatus.BANNED);
        user.setActive(false);
        user.setAccountLocked(true);

        userRepository.save(user);

        log.warn("User {} automatically banned after {} strikes", event.getUsername(), event.getTotalStrikes());

        // Notify the recipients module so the user is removed from active recipient lists.
        eventPublisher.publishEvent(UserStatusChangedEvent.builder()
                .username(user.getUsername())
                .active(false)
                .build());
    }

    // ─────────────────────────────────────────────────────────────────────────

    private User findUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
    }
}
