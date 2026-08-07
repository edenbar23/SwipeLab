package com.swipelab.classification.application;

import com.swipelab.classification.domain.core.Classification;
import com.swipelab.classification.domain.core.Label;
import com.swipelab.classification.domain.core.LabelService;
import com.swipelab.classification.domain.core.ValidationService;
import com.swipelab.classification.domain.image.Image;
import com.swipelab.classification.domain.image.SpeciesReferenceImage;
import com.swipelab.classification.domain.image.ImageService;
import com.swipelab.classification.domain.image.ImageFetchService;
import com.swipelab.classification.domain.goldimage.GoldImage;
import com.swipelab.classification.domain.goldimage.GoldImageService;
import com.swipelab.classification.domain.goldimage.GoldImagePolicy;
import com.swipelab.classification.domain.goldimage.FrequencyBasedGoldImagePolicy;
import com.swipelab.classification.domain.fraud.CredibilityRecord;
import com.swipelab.classification.domain.fraud.SuspiciousActivityRecord;
import com.swipelab.classification.domain.fraud.FraudAnalysisResult;
import com.swipelab.classification.domain.fraud.WarningLevel;
import com.swipelab.classification.domain.fraud.FraudDetectionService;
import com.swipelab.classification.domain.threshold.ThresholdPolicy;
import com.swipelab.classification.domain.threshold.CredibilityWeightedThresholdPolicy;
import com.swipelab.classification.domain.distribution.TaskDistributionService;

import com.swipelab.classification.domain.fraud.FraudDetectionService;
import com.swipelab.classification.domain.fraud.WarningLevel;
import com.swipelab.auth.application.SecurityAuthorizationService;
import com.swipelab.classification.infrastructure.SuspiciousActivityRepository;
import com.swipelab.config.application.MaliciousLabelingConfigService;
import com.swipelab.config.application.dto.MaliciousLabelingConfigResponse;
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
    @Mock private MaliciousLabelingConfigService maliciousLabelingConfigService;

    @InjectMocks
    private FraudDetectionService fraudDetectionService;

    private User activeUser;

    @BeforeEach
    void setUp() {
        MaliciousLabelingConfigResponse mockConfig = MaliciousLabelingConfigResponse.builder()
                .minResponseTimeMs(300L)
                .researcherMinResponseTimeMs(150L)
                .suspiciousCountForStrike(3)
                .slidingWindowMinutes(10)
                .strikesForWarning1(5)
                .strikesForWarning2(10)
                .strikesForBan(15)
                .warningCooldownMinutes(30)
                .autoBanEnabled(true)
                .build();
        when(maliciousLabelingConfigService.getMaliciousLabelingConfig()).thenReturn(mockConfig);


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
