package com.swipelab.classification.application;

import com.swipelab.classification.domain.FraudDetectionService;
import com.swipelab.classification.domain.WarningLevel;
import com.swipelab.auth.application.SecurityAuthorizationService;
import com.swipelab.classification.infrastructure.SuspiciousActivityRepository;
import com.swipelab.model.enums.UserRole;
import com.swipelab.model.enums.UserStatus;
import com.swipelab.users.domain.User;
import com.swipelab.users.events.UserWarnedEvent;
import com.swipelab.users.events.UserBannedBySystemEvent;
import com.swipelab.users.infrastructure.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the rewritten FraudDetectionService (sliding-window, graduated escalation).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("FraudDetectionService")
class FraudDetectionServiceTest {

    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private UserRepository userRepository;
    @Mock private SecurityAuthorizationService securityAuthorizationService;
    @Mock private SuspiciousActivityRepository suspiciousActivityRepository;

    @InjectMocks
    private FraudDetectionService fraudDetectionService;

    private User activeUser;

    @BeforeEach
    void setUp() {
        // Mirror application.yml defaults using actual field names from FraudDetectionService
        ReflectionTestUtils.setField(fraudDetectionService, "minResponseTimeMs",           300L);
        ReflectionTestUtils.setField(fraudDetectionService, "researcherMinResponseTimeMs",  150L);
        ReflectionTestUtils.setField(fraudDetectionService, "suspiciousCountForStrike",       3);
        ReflectionTestUtils.setField(fraudDetectionService, "slidingWindowMinutes",           10);
        ReflectionTestUtils.setField(fraudDetectionService, "strikesForWarning1",             5);
        ReflectionTestUtils.setField(fraudDetectionService, "strikesForWarning2",            10);
        ReflectionTestUtils.setField(fraudDetectionService, "strikesForBan",                15);
        ReflectionTestUtils.setField(fraudDetectionService, "warningCooldownMinutes",        30);


        activeUser = User.builder()
                .username("testuser")
                .email("testuser@test.com")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .strikeCount(0)
                .warningCount(0)
                .consecutiveCorrectGolds(0)
                .credibilityScore(60.0)
                .build();

        when(securityAuthorizationService.isSuperAdmin("testuser")).thenReturn(false);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(activeUser));
        when(userRepository.save(any(User.class))).thenReturn(activeUser);
    }

    // ── Single fast response — must NOT trigger a strike ─────────────────────

    @Test
    @DisplayName("Single fast response below threshold → no event published (not enough for strike)")
    void singleFastResponse_noEvent() {
        fraudDetectionService.analyzeClassification("testuser", "USER", 100L, 1L);

        verify(eventPublisher, never()).publishEvent(any(UserWarnedEvent.class));
        verify(eventPublisher, never()).publishEvent(any(UserBannedBySystemEvent.class));
    }

    // ── Normal speed → no action ──────────────────────────────────────────────

    @Test
    @DisplayName("Response above threshold → no strike, no event")
    void normalResponseTime_noStrike() {
        // 500ms is well above the 300ms threshold
        for (int i = 0; i < 10; i++) {
            fraudDetectionService.analyzeClassification("testuser", "USER", 500L, 1L);
        }

        verify(eventPublisher, never()).publishEvent(any());
    }

    // ── Enough fast responses in window → strike recorded ────────────────────

    @Test
    @DisplayName("3+ fast responses within window → strike counter incremented")
    void fastResponsesInWindow_incrementsStrike() {
        // The service counts events in the window via suspiciousActivityRepository.
        // Stub it to return exactly the threshold so escalate() fires after each call.
        when(suspiciousActivityRepository.countByUsernameAndSeverityAndCreatedAtAfter(
                eq("testuser"), eq(WarningLevel.STRIKE), any()))
                .thenReturn(3L); // equals suspiciousCountForStrike=3 → triggers escalation

        fraudDetectionService.analyzeClassification("testuser", "USER", 100L, 1L);

        verify(userRepository, atLeastOnce()).save(activeUser);
    }

    // ── Super Admin immunity ──────────────────────────────────────────────────

    @Test
    @DisplayName("Super Admin → analyzeClassification is a no-op regardless of speed")
    void superAdminImmunity_noop() {
        when(securityAuthorizationService.isSuperAdmin("superadmin")).thenReturn(true);
        when(userRepository.findByUsername("superadmin")).thenReturn(Optional.of(
                User.builder().username("superadmin").role(UserRole.RESEARCHER)
                        .status(UserStatus.ACTIVE).strikeCount(0).build()));

        for (int i = 0; i < 20; i++) {
            fraudDetectionService.analyzeClassification("superadmin", "RESEARCHER", 10L, 1L);
        }

        verify(eventPublisher, never()).publishEvent(any(UserBannedBySystemEvent.class));
        verify(eventPublisher, never()).publishEvent(any(UserWarnedEvent.class));
    }

    // ── Researcher lenient threshold ──────────────────────────────────────────

    @Test
    @DisplayName("Researcher response at 160ms > researcher threshold (150ms) → no strike")
    void researcherAboveThreshold_noStrike() {
        when(securityAuthorizationService.isSuperAdmin("researcher1")).thenReturn(false);
        User researcher = User.builder()
                .username("researcher1")
                .email("r@test.com")
                .role(UserRole.RESEARCHER)
                .status(UserStatus.ACTIVE)
                .strikeCount(0)
                .build();
        when(userRepository.findByUsername("researcher1")).thenReturn(Optional.of(researcher));
        when(userRepository.save(any(User.class))).thenReturn(researcher);

        // 160ms > 150ms researcher threshold — should NOT be flagged as fast
        for (int i = 0; i < 20; i++) {
            fraudDetectionService.analyzeClassification("researcher1", "RESEARCHER", 160L, 1L);
        }

        verify(eventPublisher, never()).publishEvent(any(UserWarnedEvent.class));
    }
}
