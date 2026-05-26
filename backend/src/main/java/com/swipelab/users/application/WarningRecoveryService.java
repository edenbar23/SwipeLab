package com.swipelab.users.application;

import com.swipelab.model.enums.UserStatus;
import com.swipelab.users.domain.User;
import com.swipelab.users.infrastructure.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles organic recovery from WARNED status through sustained correct gold-image answers.
 *
 * Recovery ladder:
 *   Every {correctGoldsToRecoverStrike} consecutive correct gold answers → -1 strike
 *   If strikeCount drops below strikesForWarning1 → status restored to ACTIVE
 *
 * A single wrong answer resets the consecutive counter (must be genuinely improving).
 *
 * Called synchronously from GoldImageEvaluatorService after each gold evaluation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WarningRecoveryService {

    private final UserRepository userRepository;
    private final AdminNotificationService adminNotificationService;

    @Value("${app.fraud-detection.correct-golds-to-recover-strike:10}")
    private int correctGoldsToRecoverStrike;

    @Value("${app.fraud-detection.strikes-for-warning-1:5}")
    private int strikesForWarning1;

    /**
     * Processes a gold-image result for a warned user.
     * Only acts when user status is WARNED — all other statuses are a no-op.
     *
     * @param username  the user who submitted the classification
     * @param isCorrect whether their answer matched the gold standard
     */
    @Transactional
    public void processGoldResult(String username, boolean isCorrect) {
        User user = userRepository.findByUsername(username)
                .orElse(null);

        if (user == null || user.getStatus() != UserStatus.WARNED) {
            return; // Only process recovery for WARNED users
        }

        if (!isCorrect) {
            // Wrong answer — reset the consecutive counter.
            if (user.getConsecutiveCorrectGolds() != null && user.getConsecutiveCorrectGolds() > 0) {
                log.debug("Wrong gold answer from warned user {} — resetting consecutive counter ({} → 0)",
                        username, user.getConsecutiveCorrectGolds());
                user.setConsecutiveCorrectGolds(0);
                userRepository.save(user);
            }
            return;
        }

        // Correct answer — advance the counter.
        int newConsecutive = (user.getConsecutiveCorrectGolds() == null ? 0 : user.getConsecutiveCorrectGolds()) + 1;
        user.setConsecutiveCorrectGolds(newConsecutive);

        log.debug("Correct gold answer from warned user {} ({}/{} toward strike reduction)",
                username, newConsecutive, correctGoldsToRecoverStrike);

        if (newConsecutive >= correctGoldsToRecoverStrike) {
            // Reached the threshold — remove one strike and reset counter.
            int currentStrikes = user.getStrikeCount() == null ? 0 : user.getStrikeCount();
            int newStrikes = Math.max(0, currentStrikes - 1);
            user.setStrikeCount(newStrikes);
            user.setConsecutiveCorrectGolds(0);

            log.info("Strike removed via recovery for user {} ({} → {} strikes)",
                    username, currentStrikes, newStrikes);

            // Check if the user has dropped below the WARNING_1 threshold — restore ACTIVE.
            if (newStrikes < strikesForWarning1) {
                user.setStatus(UserStatus.ACTIVE);
                user.setWarningCount(0);
                user.setLastWarningAt(null);

                log.info("User {} has recovered from WARNED status (strikes={}) — status restored to ACTIVE",
                        username, newStrikes);

                // Notify admin of the positive outcome.
                adminNotificationService.notifyUserRecovered(username, newStrikes);
            }
        }

        userRepository.save(user);
    }
}
