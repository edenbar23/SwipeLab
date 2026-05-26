package com.swipelab.users.application;

import com.swipelab.classification.domain.WarningLevel;
import com.swipelab.users.domain.AdminNotification;
import com.swipelab.users.domain.NotificationSeverity;
import com.swipelab.users.domain.NotificationType;
import com.swipelab.users.events.UserBannedBySystemEvent;
import com.swipelab.users.events.UserWarnedEvent;
import com.swipelab.users.infrastructure.AdminNotificationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminNotificationService")
class AdminNotificationServiceTest {

    @Mock private AdminNotificationRepository adminNotificationRepository;

    @InjectMocks
    private AdminNotificationService adminNotificationService;

    // ── onUserWarned ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("WARNING_1 event → WARNING severity notification persisted")
    void onUserWarned_warning1_createsWarningSeverityNotification() {
        when(adminNotificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UserWarnedEvent event = UserWarnedEvent.builder()
                .username("badactor")
                .level(WarningLevel.WARNING_1)
                .reason("Fast response pattern")
                .strikeCount(5)
                .strikesUntilBan(10)
                .detectedAt(LocalDateTime.now())
                .build();

        adminNotificationService.onUserWarned(event);

        ArgumentCaptor<AdminNotification> captor = ArgumentCaptor.forClass(AdminNotification.class);
        verify(adminNotificationRepository).save(captor.capture());

        AdminNotification saved = captor.getValue();
        assertThat(saved.getType()).isEqualTo(NotificationType.USER_WARNED);
        assertThat(saved.getSeverity()).isEqualTo(NotificationSeverity.WARNING);
        assertThat(saved.getTargetUsername()).isEqualTo("badactor");
        assertThat(saved.getIsRead()).isFalse();
        assertThat(saved.getMessage()).contains("5");      // strikeCount in message
        assertThat(saved.getMessage()).contains("10");     // strikesUntilBan in message
    }

    @Test
    @DisplayName("WARNING_2 event → CRITICAL severity notification persisted")
    void onUserWarned_warning2_createsCriticalSeverityNotification() {
        when(adminNotificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UserWarnedEvent event = UserWarnedEvent.builder()
                .username("badactor")
                .level(WarningLevel.WARNING_2)
                .reason("Repeated pattern")
                .strikeCount(10)
                .strikesUntilBan(5)
                .detectedAt(LocalDateTime.now())
                .build();

        adminNotificationService.onUserWarned(event);

        ArgumentCaptor<AdminNotification> captor = ArgumentCaptor.forClass(AdminNotification.class);
        verify(adminNotificationRepository).save(captor.capture());

        assertThat(captor.getValue().getSeverity()).isEqualTo(NotificationSeverity.CRITICAL);
        assertThat(captor.getValue().getTitle()).contains("Final warning");
    }

    // ── onUserBannedBySystem ──────────────────────────────────────────────────

    @Test
    @DisplayName("Ban event → CRITICAL USER_BANNED notification persisted")
    void onUserBannedBySystem_createsCriticalBanNotification() {
        when(adminNotificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UserBannedBySystemEvent event = UserBannedBySystemEvent.builder()
                .username("badactor")
                .reason("Exceeded strike limit")
                .totalStrikes(15)
                .bannedAt(LocalDateTime.now())
                .build();

        adminNotificationService.onUserBannedBySystem(event);

        ArgumentCaptor<AdminNotification> captor = ArgumentCaptor.forClass(AdminNotification.class);
        verify(adminNotificationRepository).save(captor.capture());

        AdminNotification saved = captor.getValue();
        assertThat(saved.getType()).isEqualTo(NotificationType.USER_BANNED);
        assertThat(saved.getSeverity()).isEqualTo(NotificationSeverity.CRITICAL);
        assertThat(saved.getTargetUsername()).isEqualTo("badactor");
        assertThat(saved.getMessage()).contains("15"); // totalStrikes in message
        assertThat(saved.getIsRead()).isFalse();
    }

    // ── notifyUserRecovered ───────────────────────────────────────────────────

    @Test
    @DisplayName("Recovery → INFO USER_RECOVERED notification persisted")
    void notifyUserRecovered_createsInfoNotification() {
        when(adminNotificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        adminNotificationService.notifyUserRecovered("goodactor", 3);

        ArgumentCaptor<AdminNotification> captor = ArgumentCaptor.forClass(AdminNotification.class);
        verify(adminNotificationRepository).save(captor.capture());

        AdminNotification saved = captor.getValue();
        assertThat(saved.getType()).isEqualTo(NotificationType.USER_RECOVERED);
        assertThat(saved.getSeverity()).isEqualTo(NotificationSeverity.INFO);
        assertThat(saved.getTargetUsername()).isEqualTo("goodactor");
        assertThat(saved.getMessage()).contains("3"); // remaining strikes
        assertThat(saved.getIsRead()).isFalse();
    }
}
