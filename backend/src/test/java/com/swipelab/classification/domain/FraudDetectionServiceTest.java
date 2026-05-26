package com.swipelab.classification.domain;

import com.swipelab.auth.application.SecurityAuthorizationService;
import com.swipelab.classification.infrastructure.SuspiciousActivityRepository;
import com.swipelab.model.enums.UserRole;
import com.swipelab.model.enums.UserStatus;
import com.swipelab.users.domain.User;
import com.swipelab.users.events.UserBannedBySystemEvent;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FraudDetectionService")
class FraudDetectionServiceTest {

    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private SuspiciousActivityRepository suspiciousActivityRepository;
    @Mock private UserRepository userRepository;
    @Mock private SecurityAuthorizationService securityAuthorizationService;

    @InjectMocks
    private FraudDetectionService fraudDetectionService;

    private User regularUser;

    @BeforeEach
    void setUp() {
        // Bind @Value fields via ReflectionTestUtils (Spring not loaded in unit tests)
        ReflectionTestUtils.setField(fraudDetectionService, "minResponseTimeMs", 300L);
        ReflectionTestUtils.setField(fraudDetectionService, "researcherMinResponseTimeMs", 150L);
        ReflectionTestUtils.setField(fraudDetectionService, "suspiciousCountForStrike", 3);
        ReflectionTestUtils.setField(fraudDetectionService, "slidingWindowMinutes", 10);
        ReflectionTestUtils.setField(fraudDetectionService, "strikesForWarning1", 5);
        ReflectionTestUtils.setField(fraudDetectionService, "strikesForWarning2", 10);
        ReflectionTestUtils.setField(fraudDetectionService, "strikesForBan", 15);
        ReflectionTestUtils.setField(fraudDetectionService, "warningCooldownMinutes", 30);

        regularUser = User.builder()
                .username("testuser")
                .email("testuser@test.com")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .strikeCount(0)
                .warningCount(0)
                .consecutiveCorrectGolds(0)
                .build();

        when(securityAuthorizationService.isSuperAdmin(anyString())).thenReturn(false);
    }

    // ── Happy flows ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("Single fast response → suspicious event persisted but no strike (below window threshold)")
    void singleFastResponse_noStrike() {
        when(suspiciousActivityRepository
                .countByUsernameAndSeverityAndCreatedAtAfter(any(), any(), any()))
                .thenReturn(1L); // only 1 event in window, need 3

        fraudDetectionService.analyzeClassification("testuser", "USER", 100L, 1L);

        // Audit record saved
        verify(suspiciousActivityRepository).save(any(SuspiciousActivityRecord.class));
        // No user fetched → no strike, no event
        verifyNoInteractions(userRepository);
        verifyNoInteractions(eventPublisher);
    }

    @Test
    @DisplayName("N fast responses in window → strike recorded, still below WARNING_1 → no event")
    void nFastResponsesInWindow_strikeRecorded_belowWarning1() {
        when(suspiciousActivityRepository
                .countByUsernameAndSeverityAndCreatedAtAfter(any(), any(), any()))
                .thenReturn(3L); // exactly suspiciousCountForStrike
        regularUser.setStrikeCount(3); // new strike will be 4, below warning-1 threshold of 5
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(regularUser));

        fraudDetectionService.analyzeClassification("testuser", "USER", 100L, 1L);

        verify(userRepository).save(regularUser);
        assertThat(regularUser.getStrikeCount()).isEqualTo(4);
        verifyNoInteractions(eventPublisher);
    }

    @Test
    @DisplayName("Strike count reaches warning-1 threshold → UserWarnedEvent(WARNING_1) published")
    void strikeReachesWarning1_eventPublished() {
        when(suspiciousActivityRepository
                .countByUsernameAndSeverityAndCreatedAtAfter(any(), any(), any()))
                .thenReturn(3L);
        regularUser.setStrikeCount(4); // new strike will be 5 = strikesForWarning1
        regularUser.setWarningCount(0);
        regularUser.setLastWarningAt(null);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(regularUser));

        fraudDetectionService.analyzeClassification("testuser", "USER", 100L, 1L);

        ArgumentCaptor<UserWarnedEvent> captor = ArgumentCaptor.forClass(UserWarnedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());

        UserWarnedEvent event = captor.getValue();
        assertThat(event.getUsername()).isEqualTo("testuser");
        assertThat(event.getLevel()).isEqualTo(WarningLevel.WARNING_1);
        assertThat(event.getStrikeCount()).isEqualTo(5);
        assertThat(event.getStrikesUntilBan()).isEqualTo(10); // 15 - 5
    }

    @Test
    @DisplayName("Strike count reaches warning-2 threshold → UserWarnedEvent(WARNING_2) published")
    void strikeReachesWarning2_eventPublished() {
        when(suspiciousActivityRepository
                .countByUsernameAndSeverityAndCreatedAtAfter(any(), any(), any()))
                .thenReturn(3L);
        regularUser.setStrikeCount(9); // new strike = 10 = strikesForWarning2
        regularUser.setWarningCount(1); // already has WARNING_1
        regularUser.setLastWarningAt(LocalDateTime.now().minusHours(2)); // past cooldown
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(regularUser));

        fraudDetectionService.analyzeClassification("testuser", "USER", 100L, 1L);

        ArgumentCaptor<UserWarnedEvent> captor = ArgumentCaptor.forClass(UserWarnedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());

        assertThat(captor.getValue().getLevel()).isEqualTo(WarningLevel.WARNING_2);
    }

    @Test
    @DisplayName("Strike count reaches ban threshold → UserBannedBySystemEvent published")
    void strikeReachesBan_banEventPublished() {
        when(suspiciousActivityRepository
                .countByUsernameAndSeverityAndCreatedAtAfter(any(), any(), any()))
                .thenReturn(3L);
        regularUser.setStrikeCount(14); // new strike = 15 = strikesForBan
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(regularUser));

        fraudDetectionService.analyzeClassification("testuser", "USER", 100L, 1L);

        ArgumentCaptor<UserBannedBySystemEvent> captor =
                ArgumentCaptor.forClass(UserBannedBySystemEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());

        assertThat(captor.getValue().getUsername()).isEqualTo("testuser");
        assertThat(captor.getValue().getTotalStrikes()).isEqualTo(15);
    }

    // ── Edge cases ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Response time at or above threshold → no action taken")
    void responseTimeAboveThreshold_noAction() {
        fraudDetectionService.analyzeClassification("testuser", "USER", 300L, 1L);

        verifyNoInteractions(suspiciousActivityRepository);
        verifyNoInteractions(userRepository);
        verifyNoInteractions(eventPublisher);
    }

    @Test
    @DisplayName("Super Admin response → fully skipped regardless of speed")
    void superAdmin_immuneToFraudDetection() {
        when(securityAuthorizationService.isSuperAdmin("superadmin")).thenReturn(true);

        fraudDetectionService.analyzeClassification("superadmin", "USER", 1L, 1L);

        verifyNoInteractions(suspiciousActivityRepository);
        verifyNoInteractions(userRepository);
        verifyNoInteractions(eventPublisher);
    }

    @Test
    @DisplayName("Researcher gets lenient threshold (150ms) — 200ms response should not flag")
    void researcher_lenientThreshold_notFlagged() {
        // 200ms is above researcher threshold (150ms) but below user threshold (300ms)
        fraudDetectionService.analyzeClassification("researcher1", "RESEARCHER", 200L, 1L);

        verifyNoInteractions(suspiciousActivityRepository);
        verifyNoInteractions(eventPublisher);
    }

    @Test
    @DisplayName("Researcher below their threshold (100ms) → still flagged")
    void researcher_belowOwnThreshold_isFlagged() {
        when(suspiciousActivityRepository
                .countByUsernameAndSeverityAndCreatedAtAfter(any(), any(), any()))
                .thenReturn(1L); // below strike threshold — just an audit record

        // 100ms < researcher threshold 150ms → suspicious event persisted
        fraudDetectionService.analyzeClassification("researcher1", "RESEARCHER", 100L, 1L);

        verify(suspiciousActivityRepository).save(any(SuspiciousActivityRecord.class));
    }

    @Test
    @DisplayName("Warning suppressed when within cooldown window")
    void warning_suppressedWithinCooldown() {
        when(suspiciousActivityRepository
                .countByUsernameAndSeverityAndCreatedAtAfter(any(), any(), any()))
                .thenReturn(3L);
        regularUser.setStrikeCount(4); // new = 5 = WARNING_1 threshold
        regularUser.setWarningCount(0);
        // Last warning was 5 minutes ago — within 30-minute cooldown
        regularUser.setLastWarningAt(LocalDateTime.now().minusMinutes(5));
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(regularUser));

        fraudDetectionService.analyzeClassification("testuser", "USER", 100L, 1L);

        // Strike saved but no event published due to cooldown
        verify(userRepository).save(regularUser);
        verifyNoInteractions(eventPublisher);
    }

    @Test
    @DisplayName("Fast response exactly at window boundary → not counted in window")
    void fastResponse_outsideWindow_notCounted() {
        // 0 events in window → no strike
        when(suspiciousActivityRepository
                .countByUsernameAndSeverityAndCreatedAtAfter(any(), any(), any()))
                .thenReturn(0L);

        fraudDetectionService.analyzeClassification("testuser", "USER", 100L, 1L);

        verify(suspiciousActivityRepository).save(any()); // audit record still saved
        verifyNoInteractions(userRepository);             // but no escalation
        verifyNoInteractions(eventPublisher);
    }
}
