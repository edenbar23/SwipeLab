package com.swipelab.users.application;

import com.swipelab.model.enums.UserRole;
import com.swipelab.model.enums.UserStatus;
import com.swipelab.users.domain.User;
import com.swipelab.users.infrastructure.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("WarningRecoveryService")
class WarningRecoveryServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private AdminNotificationService adminNotificationService;

    @InjectMocks
    private WarningRecoveryService warningRecoveryService;

    private User warnedUser;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(warningRecoveryService, "correctGoldsToRecoverStrike", 10);
        ReflectionTestUtils.setField(warningRecoveryService, "strikesForWarning1", 5);

        warnedUser = User.builder()
                .username("warneduser")
                .email("warned@test.com")
                .role(UserRole.USER)
                .status(UserStatus.WARNED)
                .strikeCount(6)
                .warningCount(1)
                .consecutiveCorrectGolds(0)
                .credibilityScore(45.0)
                .build();

        when(userRepository.findByUsername("warneduser")).thenReturn(Optional.of(warnedUser));
    }

    // ── Happy flows ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("Correct gold → consecutiveCorrectGolds incremented, no strike removed yet")
    void correctGold_incrementsCounter_noStrikeRemovedYet() {
        warnedUser.setConsecutiveCorrectGolds(4); // 4 correct, need 10
        when(userRepository.save(any(User.class))).thenReturn(warnedUser);

        warningRecoveryService.processGoldResult("warneduser", true);

        assertThat(warnedUser.getConsecutiveCorrectGolds()).isEqualTo(5);
        assertThat(warnedUser.getStrikeCount()).isEqualTo(6); // unchanged
        verify(userRepository).save(warnedUser);
    }

    @Test
    @DisplayName("10th consecutive correct gold → 1 strike removed, counter reset")
    void tenthCorrectGold_removesOneStrike() {
        warnedUser.setConsecutiveCorrectGolds(9); // this answer is the 10th
        when(userRepository.save(any(User.class))).thenReturn(warnedUser);

        warningRecoveryService.processGoldResult("warneduser", true);

        assertThat(warnedUser.getStrikeCount()).isEqualTo(5); // 6 → 5
        assertThat(warnedUser.getConsecutiveCorrectGolds()).isEqualTo(0); // reset
        // Still WARNED (5 strikes = threshold, not below it)
        assertThat(warnedUser.getStatus()).isEqualTo(UserStatus.WARNED);
        verifyNoInteractions(adminNotificationService);
    }

    @Test
    @DisplayName("Strike drops below threshold → user restored to ACTIVE, admin notified")
    void strikeDropsBelowThreshold_restoresActive() {
        warnedUser.setStrikeCount(5);             // threshold is strikesForWarning1=5
        warnedUser.setConsecutiveCorrectGolds(9);  // 10th correct answer
        when(userRepository.save(any(User.class))).thenReturn(warnedUser);

        warningRecoveryService.processGoldResult("warneduser", true);

        assertThat(warnedUser.getStrikeCount()).isEqualTo(4); // below 5
        assertThat(warnedUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(warnedUser.getWarningCount()).isEqualTo(0);
        assertThat(warnedUser.getLastWarningAt()).isNull();
        verify(adminNotificationService).notifyUserRecovered("warneduser", 4);
    }

    // ── Edge cases ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Wrong gold answer → consecutive counter reset to 0")
    void wrongGold_resetsConsecutiveCounter() {
        warnedUser.setConsecutiveCorrectGolds(7);
        when(userRepository.save(any(User.class))).thenReturn(warnedUser);

        warningRecoveryService.processGoldResult("warneduser", false);

        assertThat(warnedUser.getConsecutiveCorrectGolds()).isEqualTo(0);
        assertThat(warnedUser.getStrikeCount()).isEqualTo(6); // unchanged
        verify(userRepository).save(warnedUser);
    }

    @Test
    @DisplayName("Wrong gold when counter is already 0 → no unnecessary save")
    void wrongGold_counterAlreadyZero_noUnnecessarySave() {
        warnedUser.setConsecutiveCorrectGolds(0);

        warningRecoveryService.processGoldResult("warneduser", false);

        // Counter is already 0 — no point saving
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("ACTIVE user → processGoldResult is a no-op")
    void activeUser_noop() {
        User activeUser = User.builder()
                .username("activeuser")
                .email("active@test.com")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .strikeCount(2)
                .consecutiveCorrectGolds(0)
                .build();
        when(userRepository.findByUsername("activeuser")).thenReturn(Optional.of(activeUser));

        warningRecoveryService.processGoldResult("activeuser", true);

        verify(userRepository, never()).save(any());
        verifyNoInteractions(adminNotificationService);
    }

    @Test
    @DisplayName("BANNED user → processGoldResult is a no-op")
    void bannedUser_noop() {
        User bannedUser = User.builder()
                .username("banneduser")
                .email("banned@test.com")
                .role(UserRole.USER)
                .status(UserStatus.BANNED)
                .strikeCount(15)
                .consecutiveCorrectGolds(0)
                .build();
        when(userRepository.findByUsername("banneduser")).thenReturn(Optional.of(bannedUser));

        warningRecoveryService.processGoldResult("banneduser", true);

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Unknown user → graceful no-op (no exception)")
    void unknownUser_gracefulNoop() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        warningRecoveryService.processGoldResult("ghost", true);

        verifyNoInteractions(adminNotificationService);
    }

    @Test
    @DisplayName("Strike count cannot go below 0 on recovery")
    void strikeCountFloorAtZero() {
        warnedUser.setStrikeCount(0);
        warnedUser.setConsecutiveCorrectGolds(9);
        when(userRepository.save(any(User.class))).thenReturn(warnedUser);

        warningRecoveryService.processGoldResult("warneduser", true);

        assertThat(warnedUser.getStrikeCount()).isEqualTo(0); // floor at 0
    }
}
