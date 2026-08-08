package com.swipelab.users.application;

import com.swipelab.classification.domain.core.Classification;
import com.swipelab.classification.domain.image.Image;
import com.swipelab.classification.domain.util.CredibilityCalculator;
import com.swipelab.classification.infrastructure.ClassificationRepository;
import com.swipelab.classification.infrastructure.CredibilityRepository;
import com.swipelab.config.application.MaliciousLabelingConfigService;
import com.swipelab.config.application.dto.MaliciousLabelingConfigResponse;
import com.swipelab.model.enums.UserRole;
import com.swipelab.model.enums.UserStatus;
import com.swipelab.users.domain.User;
import com.swipelab.users.infrastructure.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CredibilityServiceTest {

    @Mock private ClassificationRepository classificationRepository;
    @Mock private CredibilityRepository credibilityRepository;
    @Mock private UserRepository userRepository;
    @Mock private CredibilityCalculator credibilityCalculator;
    @Mock private AdminNotificationService adminNotificationService;
    @Mock private MaliciousLabelingConfigService maliciousLabelingConfigService;

    @InjectMocks
    private CredibilityService credibilityService;

    private User regularUser;
    private User researcherUser;
    private Image image;
    private Classification userClassification;

    @BeforeEach
    void setUp() {
        // Inject remaining @Value fields (not moved to configService)
        ReflectionTestUtils.setField(credibilityService, "minClassificationsForConsensus", 3);
        ReflectionTestUtils.setField(credibilityService, "defaultScore", 50.0);

        // maliciousThreshold and maliciousMinSamples are now served by MaliciousLabelingConfigService
        lenient().when(maliciousLabelingConfigService.getMaliciousLabelingConfig())
                .thenReturn(MaliciousLabelingConfigResponse.builder()
                        .maliciousThreshold(15.0)
                        .maliciousMinSamples(20)
                        .autoBanEnabled(true)
                        .minResponseTimeMs(300L).researcherMinResponseTimeMs(150L)
                        .suspiciousCountForStrike(3).slidingWindowMinutes(10)
                        .strikesForWarning1(5).strikesForWarning2(10)
                        .strikesForBan(15).warningCooldownMinutes(30)
                        .build());

        image = new Image();
        image.setId(1L);

        regularUser = User.builder()
                .username("regular")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .credibilityScore(50.0)
                .agreementWithExperts(0.0)
                .majorityAgreementScore(0.0)
                .correctGoldClassifications(0)
                .totalGoldClassifications(0)
                .isFlagged(false)
                .build();

        researcherUser = User.builder()
                .username("expert")
                .role(UserRole.RESEARCHER)
                .credibilityScore(50.0)
                .build();

        userClassification = new Classification();
        userClassification.setUsername("regular");
        userClassification.setImage(image);
        userClassification.setQuerySpecies("wasp");
        userClassification.setUserResponse(Classification.UserResponse.YES);
    }

    // ── Happy path ────────────────────────────────────────────────────────────

    @Test
    void updateUserCredibility_ShouldSkipResearcher() {
        when(userRepository.findByUsername("expert")).thenReturn(Optional.of(researcherUser));

        credibilityService.updateUserCredibility("expert");

        verify(classificationRepository, never()).findByUsername(anyString());
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateUserCredibility_ShouldComputeCompositeScore_WhenMajoritySignalExists() {
        // Arrange
        Classification peerClassification = new Classification();
        peerClassification.setUsername("other");
        peerClassification.setImage(image);
        peerClassification.setQuerySpecies("wasp");
        peerClassification.setUserResponse(Classification.UserResponse.YES);

        Classification thirdClassification = new Classification();
        thirdClassification.setUsername("third");
        thirdClassification.setImage(image);
        thirdClassification.setQuerySpecies("wasp");
        thirdClassification.setUserResponse(Classification.UserResponse.YES);

        when(userRepository.findByUsername("regular")).thenReturn(Optional.of(regularUser));
        when(credibilityRepository.findByUsername("regular")).thenReturn(Collections.emptyList());
        when(classificationRepository.findByUsername("regular")).thenReturn(List.of(userClassification));
        when(classificationRepository.findExpertClassifications()).thenReturn(Collections.emptyList());
        when(classificationRepository.countByImageIdAndQuerySpecies(1L, "wasp")).thenReturn(3L);
        when(classificationRepository.findByImageIdAndQuerySpecies(1L, "wasp"))
                .thenReturn(List.of(userClassification, peerClassification, thirdClassification));
        when(credibilityCalculator.calculateMajorityVote(any())).thenReturn(Classification.UserResponse.YES);
        when(credibilityCalculator.calculateMajorityAgreementScore(any(), eq(Classification.UserResponse.YES)))
                .thenReturn(1.0);
        when(credibilityCalculator.calculateCompositeScore(isNull(), eq(1.0), isNull(), eq(0.0)))
                .thenReturn(85.0);
        when(classificationRepository.countByUsername("regular")).thenReturn(5L);

        // Act
        credibilityService.updateUserCredibility("regular");

        // Assert
        assertEquals(85.0, regularUser.getCredibilityScore());
        assertEquals(1.0, regularUser.getMajorityAgreementScore());
        verify(userRepository, times(1)).save(regularUser);
    }

    // ── Edge cases ────────────────────────────────────────────────────────────

    @Test
    void updateUserCredibility_ShouldSkipPair_WhenBelowConsensusThreshold() {
        // Only 2 classifications on the pair — below threshold of 3
        when(userRepository.findByUsername("regular")).thenReturn(Optional.of(regularUser));
        when(credibilityRepository.findByUsername("regular")).thenReturn(Collections.emptyList());
        when(classificationRepository.findByUsername("regular")).thenReturn(List.of(userClassification));
        when(classificationRepository.findExpertClassifications()).thenReturn(Collections.emptyList());
        when(classificationRepository.countByImageIdAndQuerySpecies(1L, "wasp")).thenReturn(2L);
        // Note: countByUsername is NOT stubbed — the service exits before checkForMaliciousLabeling

        credibilityService.updateUserCredibility("regular");

        // All signals are null — service exits early (hasSignal=false), never saves
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateUserCredibility_SameImageDifferentSpecies_TreatedIndependently() {
        // Two classifications on same image but different query species
        Classification waspClassification = new Classification();
        waspClassification.setUsername("regular");
        waspClassification.setImage(image);
        waspClassification.setQuerySpecies("wasp");
        waspClassification.setUserResponse(Classification.UserResponse.YES);

        Classification beeClassification = new Classification();
        beeClassification.setUsername("regular");
        beeClassification.setImage(image);
        beeClassification.setQuerySpecies("bee");
        beeClassification.setUserResponse(Classification.UserResponse.NO);

        when(userRepository.findByUsername("regular")).thenReturn(Optional.of(regularUser));
        when(credibilityRepository.findByUsername("regular")).thenReturn(Collections.emptyList());
        when(classificationRepository.findByUsername("regular"))
                .thenReturn(List.of(waspClassification, beeClassification));
        when(classificationRepository.findExpertClassifications()).thenReturn(Collections.emptyList());
        // Both pairs below threshold — use lenient to avoid UnnecessaryStubbingException
        // since the loop may not call the bee species if wasp is already below threshold
        lenient().when(classificationRepository.countByImageIdAndQuerySpecies(eq(1L), anyString()))
                .thenReturn(2L);
        // Note: countByUsername not stubbed — service exits before checkForMaliciousLabeling

        credibilityService.updateUserCredibility("regular");

        // Both pairs below threshold — no signal, no update
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateUserCredibility_ShouldFlagMaliciousLabeler_WhenScoreBelowThreshold() {
        regularUser.setCredibilityScore(10.0); // below maliciousThreshold=15.0
        regularUser.setIsFlagged(false);

        when(userRepository.findByUsername("regular")).thenReturn(Optional.of(regularUser));
        when(credibilityRepository.findByUsername("regular")).thenReturn(Collections.emptyList());
        when(classificationRepository.findByUsername("regular")).thenReturn(List.of(userClassification));
        when(classificationRepository.findExpertClassifications()).thenReturn(Collections.emptyList());
        when(classificationRepository.countByImageIdAndQuerySpecies(1L, "wasp")).thenReturn(5L);
        when(classificationRepository.findByImageIdAndQuerySpecies(1L, "wasp"))
                .thenReturn(List.of(userClassification));
        when(credibilityCalculator.calculateMajorityVote(any())).thenReturn(Classification.UserResponse.NO);
        when(credibilityCalculator.calculateMajorityAgreementScore(any(), any())).thenReturn(0.0);
        when(credibilityCalculator.calculateCompositeScore(any(), any(), any(), anyDouble())).thenReturn(10.0);
        // Over malicious-min-samples threshold
        when(classificationRepository.countByUsername("regular")).thenReturn(25L);

        credibilityService.updateUserCredibility("regular");

        assertTrue(regularUser.getIsFlagged());
        verify(adminNotificationService, times(1)).notifyMaliciousLabeler(eq("regular"), anyDouble());
    }

    @Test
    void updateUserCredibility_ShouldNotFlagMaliciousLabeler_WhenBelowMinSamples() {
        regularUser.setCredibilityScore(10.0);
        regularUser.setIsFlagged(false);

        when(userRepository.findByUsername("regular")).thenReturn(Optional.of(regularUser));
        when(credibilityRepository.findByUsername("regular")).thenReturn(Collections.emptyList());
        when(classificationRepository.findByUsername("regular")).thenReturn(List.of(userClassification));
        when(classificationRepository.findExpertClassifications()).thenReturn(Collections.emptyList());
        when(classificationRepository.countByImageIdAndQuerySpecies(1L, "wasp")).thenReturn(5L);
        when(classificationRepository.findByImageIdAndQuerySpecies(1L, "wasp"))
                .thenReturn(List.of(userClassification));
        when(credibilityCalculator.calculateMajorityVote(any())).thenReturn(Classification.UserResponse.NO);
        when(credibilityCalculator.calculateMajorityAgreementScore(any(), any())).thenReturn(0.0);
        when(credibilityCalculator.calculateCompositeScore(any(), any(), any(), anyDouble())).thenReturn(10.0);
        // Below malicious-min-samples
        when(classificationRepository.countByUsername("regular")).thenReturn(5L);

        credibilityService.updateUserCredibility("regular");

        assertFalse(regularUser.getIsFlagged());
        verify(adminNotificationService, never()).notifyMaliciousLabeler(anyString(), anyDouble());
    }

    @Test
    void getCredibilityStats_ShouldReturnAllFields() {
        regularUser.setAgreementWithExperts(0.8);
        regularUser.setMajorityAgreementScore(0.9);
        regularUser.setCorrectGoldClassifications(4);
        regularUser.setTotalGoldClassifications(5);
        regularUser.setCredibilityScore(72.0);

        when(userRepository.findByUsername("regular")).thenReturn(Optional.of(regularUser));
        when(classificationRepository.findByUsername("regular")).thenReturn(List.of(userClassification));
        when(classificationRepository.findExpertClassifications()).thenReturn(Collections.emptyList());

        CredibilityService.CredibilityStats stats = credibilityService.getCredibilityStats("regular");

        assertNotNull(stats);
        assertEquals("regular", stats.getUsername());
        assertEquals(1, stats.getTotalClassifications());
        assertEquals(72.0, stats.getCredibilityScore());
        assertEquals(0.8, stats.getExpertAgreementScore());
        assertEquals(0.9, stats.getMajorityAgreementScore());
        assertEquals(0.8, stats.getGoldAccuracy(), 1e-9);
    }
}
