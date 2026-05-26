package com.swipelab.users.application;

import com.swipelab.classification.domain.WarningLevel;
import com.swipelab.model.enums.UserRole;
import com.swipelab.model.enums.UserStatus;
import com.swipelab.users.domain.User;
import com.swipelab.users.events.UserBannedBySystemEvent;
import com.swipelab.users.events.UserStatusChangedEvent;
import com.swipelab.users.events.UserWarnedEvent;
import com.swipelab.users.infrastructure.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserEventListener")
class UserEventListenerTest {

    @Mock private UserRepository userRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private UserEventListener userEventListener;

    private User activeUser;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(userEventListener, "penaltyWarning1", 5.0);
        ReflectionTestUtils.setField(userEventListener, "penaltyWarning2", 15.0);

        activeUser = User.builder()
                .username("testuser")
                .email("testuser@test.com")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .credibilityScore(50.0)
                .warningCount(0)
                .strikeCount(3)
                .consecutiveCorrectGolds(5)
                .build();

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(activeUser));
        when(userRepository.save(any(User.class))).thenReturn(activeUser);
    }

    // ── onUserWarned ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("WARNING_1 → status=WARNED, credibility -5, warningCount+1, recovery counter reset")
    void onUserWarned_warning1_updatesUser() {
        UserWarnedEvent event = UserWarnedEvent.builder()
                .username("testuser")
                .level(WarningLevel.WARNING_1)
                .reason("Fast response pattern")
                .strikeCount(5)
                .strikesUntilBan(10)
                .detectedAt(LocalDateTime.now())
                .build();

        userEventListener.onUserWarned(event);

        assertThat(activeUser.getStatus()).isEqualTo(UserStatus.WARNED);
        assertThat(activeUser.getWarningCount()).isEqualTo(1);
        assertThat(activeUser.getCredibilityScore()).isEqualTo(45.0); // 50 - 5
        assertThat(activeUser.getLastWarningAt()).isNotNull();
        assertThat(activeUser.getConsecutiveCorrectGolds()).isEqualTo(0); // reset
        verify(userRepository).save(activeUser);
    }

    @Test
    @DisplayName("WARNING_2 → credibility reduced by 15 (larger penalty)")
    void onUserWarned_warning2_largerPenalty() {
        activeUser.setWarningCount(1);

        UserWarnedEvent event = UserWarnedEvent.builder()
                .username("testuser")
                .level(WarningLevel.WARNING_2)
                .reason("Pattern escalation")
                .strikeCount(10)
                .strikesUntilBan(5)
                .detectedAt(LocalDateTime.now())
                .build();

        userEventListener.onUserWarned(event);

        assertThat(activeUser.getCredibilityScore()).isEqualTo(35.0); // 50 - 15
        assertThat(activeUser.getWarningCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("Credibility cannot go below 0 after penalty")
    void onUserWarned_credibilityFloorAtZero() {
        activeUser.setCredibilityScore(3.0); // less than penalty of 5

        UserWarnedEvent event = UserWarnedEvent.builder()
                .username("testuser")
                .level(WarningLevel.WARNING_1)
                .reason("Fast response")
                .strikeCount(5)
                .strikesUntilBan(10)
                .detectedAt(LocalDateTime.now())
                .build();

        userEventListener.onUserWarned(event);

        assertThat(activeUser.getCredibilityScore()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Warning on already-warned user still increments warningCount")
    void onUserWarned_alreadyWarned_incrementsCount() {
        activeUser.setStatus(UserStatus.WARNED);
        activeUser.setWarningCount(1);

        UserWarnedEvent event = UserWarnedEvent.builder()
                .username("testuser")
                .level(WarningLevel.WARNING_2)
                .reason("Continued pattern")
                .strikeCount(10)
                .strikesUntilBan(5)
                .detectedAt(LocalDateTime.now())
                .build();

        userEventListener.onUserWarned(event);

        assertThat(activeUser.getWarningCount()).isEqualTo(2);
    }

    // ── onUserBannedBySystem ──────────────────────────────────────────────────

    @Test
    @DisplayName("System ban → status=BANNED, active=false, accountLocked=true")
    void onUserBannedBySystem_setsCorrectState() {
        UserBannedBySystemEvent event = UserBannedBySystemEvent.builder()
                .username("testuser")
                .reason("Exceeded 15 strikes")
                .totalStrikes(15)
                .bannedAt(LocalDateTime.now())
                .build();

        userEventListener.onUserBannedBySystem(event);

        assertThat(activeUser.getStatus()).isEqualTo(UserStatus.BANNED);
        assertThat(activeUser.getActive()).isFalse();
        assertThat(activeUser.getAccountLocked()).isTrue();
        verify(userRepository).save(activeUser);
    }

    @Test
    @DisplayName("System ban → downstream UserStatusChangedEvent published for recipients module")
    void onUserBannedBySystem_publishesDownstreamEvent() {
        UserBannedBySystemEvent event = UserBannedBySystemEvent.builder()
                .username("testuser")
                .reason("15 strikes")
                .totalStrikes(15)
                .bannedAt(LocalDateTime.now())
                .build();

        userEventListener.onUserBannedBySystem(event);

        ArgumentCaptor<UserStatusChangedEvent> captor =
                ArgumentCaptor.forClass(UserStatusChangedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());

        UserStatusChangedEvent downstream = captor.getValue();
        assertThat(downstream.getUsername()).isEqualTo("testuser");
        assertThat(downstream.isActive()).isFalse();
    }
}
