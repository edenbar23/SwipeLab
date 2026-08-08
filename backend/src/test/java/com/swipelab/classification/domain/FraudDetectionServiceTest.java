package com.swipelab.classification.domain;

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

import com.swipelab.auth.application.SecurityAuthorizationService;
import com.swipelab.classification.infrastructure.SuspiciousActivityRepository;
import com.swipelab.config.application.MaliciousLabelingConfigService;
import com.swipelab.config.application.dto.MaliciousLabelingConfigResponse;
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
    @Mock private MaliciousLabelingConfigService maliciousLabelingConfigService;

    @InjectMocks
    private FraudDetectionService fraudDetectionService;

    private User regularUser;
    private MaliciousLabelingConfigResponse defaultCfg;

    @BeforeEach
    void setUp() {
        // Build the config DTO that the mock configService will return.
        // This replaces the old ReflectionTestUtils @Value injection.
        defaultCfg = MaliciousLabelingConfigResponse.builder()
                .minResponseTimeMs(300L)
                .researcherMinResponseTimeMs(150L)
                .suspiciousCountForStrike(3)
                .slidingWindowMinutes(10)
                .strikesForWarning1(5)
                .strikesForWarning2(10)
                .strikesForBan(15)
                .warningCooldownMinutes(30)
                .autoBanEnabled(true)
                .maliciousThreshold(15.0)
                .maliciousMinSamples(20)
                .build();

        lenient().when(maliciousLabelingConfigService.getMaliciousLabelingConfig()).thenReturn(defaultCfg);

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

    // ── Auto-ban toggle ───────────────────────────────────────────────────────

    @Test
    @DisplayName("Auto-ban ENABLED: strikes reach ban threshold → UserBannedBySystemEvent published")
    void autoBanEnabled_strikesAtThreshold_banPublished() {
        when(suspiciousActivityRepository
                .countByUsernameAndSeverityAndCreatedAtAfter(any(), any(), any()))
                .thenReturn(3L);
        regularUser.setStrikeCount(14); // new strike = 15 = strikesForBan, auto-ban enabled
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(regularUser));
        // defaultCfg has autoBanEnabled=true — already stubbed in setUp

        fraudDetectionService.analyzeClassification("testuser", "USER", 100L, 1L);

        ArgumentCaptor<UserBannedBySystemEvent> captor =
                ArgumentCaptor.forClass(UserBannedBySystemEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().getUsername()).isEqualTo("testuser");
    }

    @Test
    @DisplayName("Auto-ban DISABLED: strikes reach ban threshold → NO ban event, strike still saved")
    void autoBanDisabled_strikesAtThreshold_noBanPublished() {
        // Override config: auto-ban off
        MaliciousLabelingConfigResponse noBanCfg = MaliciousLabelingConfigResponse.builder()
                .minResponseTimeMs(300L).researcherMinResponseTimeMs(150L)
                .suspiciousCountForStrike(3).slidingWindowMinutes(10)
                .strikesForWarning1(5).strikesForWarning2(10)
                .strikesForBan(15).warningCooldownMinutes(30)
                .autoBanEnabled(false) // <── key difference
                .maliciousThreshold(15.0).maliciousMinSamples(20)
                .build();
        when(maliciousLabelingConfigService.getMaliciousLabelingConfig()).thenReturn(noBanCfg);

        when(suspiciousActivityRepository
                .countByUsernameAndSeverityAndCreatedAtAfter(any(), any(), any()))
                .thenReturn(3L);
        regularUser.setStrikeCount(14); // would normally trigger ban
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(regularUser));

        fraudDetectionService.analyzeClassification("testuser", "USER", 100L, 1L);

        // Strike count incremented to 15
        assertThat(regularUser.getStrikeCount()).isEqualTo(15);
        // But NO ban event published
        verifyNoInteractions(eventPublisher);
    }
}
