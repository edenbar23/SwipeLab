package com.swipelab.users.application;

import com.swipelab.classification.domain.core.Classification;
import com.swipelab.classification.domain.image.Image;
import com.swipelab.classification.events.ClassificationSubmittedEvent;
import com.swipelab.classification.infrastructure.ClassificationRepository;
import com.swipelab.model.enums.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CredibilityEventListenerTest {

    @Mock private CredibilityService credibilityService;
    @Mock private ClassificationRepository classificationRepository;

    @InjectMocks
    private CredibilityEventListener listener;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(listener, "minClassificationsForConsensus", 3);
    }

    private ClassificationSubmittedEvent buildEvent(String userRole, Long imageId,
                                                     String species, boolean isGold) {
        return ClassificationSubmittedEvent.builder()
                .username("user1")
                .imageId(imageId)
                .taskId(1L)
                .species(species)
                .userRole(userRole)
                .isGoldStandard(isGold)
                .userResponse(Classification.UserResponse.YES)
                .build();
    }

    // ── Rule 1: Consensus threshold crossed ───────────────────────────────────

    @Test
    void onClassificationSubmitted_ShouldTriggerBatchRecalculation_WhenThresholdJustCrossed() {
        ClassificationSubmittedEvent event = buildEvent(UserRole.USER.name(), 1L, "wasp", false);
        when(classificationRepository.countByImageIdAndQuerySpecies(1L, "wasp")).thenReturn(3L); // exactly at threshold

        listener.onClassificationSubmitted(event);

        verify(credibilityService, times(1))
                .recalculateCredibilityForImageQuery(1L, "wasp");
        verify(credibilityService, never()).updateUserCredibility(anyString());
    }

    @Test
    void onClassificationSubmitted_ShouldUpdateOnlySubmitter_WhenConsensusAlreadyExists() {
        ClassificationSubmittedEvent event = buildEvent(UserRole.USER.name(), 1L, "wasp", false);
        when(classificationRepository.countByImageIdAndQuerySpecies(1L, "wasp")).thenReturn(10L); // above threshold

        listener.onClassificationSubmitted(event);

        verify(credibilityService, never()).recalculateCredibilityForImageQuery(anyLong(), anyString());
        verify(credibilityService, times(1)).updateUserCredibility("user1");
    }

    @Test
    void onClassificationSubmitted_ShouldSkipUpdate_WhenBelowThreshold() {
        ClassificationSubmittedEvent event = buildEvent(UserRole.USER.name(), 1L, "wasp", false);
        when(classificationRepository.countByImageIdAndQuerySpecies(1L, "wasp")).thenReturn(2L); // below threshold

        listener.onClassificationSubmitted(event);

        verify(credibilityService, never()).recalculateCredibilityForImageQuery(anyLong(), anyString());
        verify(credibilityService, never()).updateUserCredibility(anyString());
    }

    // ── Rule 2: Expert classification ─────────────────────────────────────────

    @Test
    void onClassificationSubmitted_ShouldTriggerBatchRecalculation_WhenExpertClassifies() {
        ClassificationSubmittedEvent event = buildEvent(UserRole.RESEARCHER.name(), 1L, "wasp", false);

        listener.onClassificationSubmitted(event);

        verify(credibilityService, times(1))
                .recalculateCredibilityForImageQuery(1L, "wasp");
        verify(credibilityService, never()).updateUserCredibility(anyString());
        // Should NOT check count — expert bypass
        verify(classificationRepository, never()).countByImageIdAndQuerySpecies(anyLong(), anyString());
    }

    // ── Rule 3: Gold image classification ────────────────────────────────────

    @Test
    void onClassificationSubmitted_ShouldUpdateSubmitter_WhenGoldImage() {
        ClassificationSubmittedEvent event = buildEvent(UserRole.USER.name(), 1L, "wasp", true);

        listener.onClassificationSubmitted(event);

        // Just refresh the submitting user's composite score (gold record already saved by GoldImageEvaluatorService)
        verify(credibilityService, times(1)).updateUserCredibility("user1");
        verify(classificationRepository, never()).countByImageIdAndQuerySpecies(anyLong(), anyString());
    }

    // ── Edge cases ────────────────────────────────────────────────────────────

    @Test
    void onClassificationSubmitted_ShouldSkip_WhenUsernameIsNull() {
        ClassificationSubmittedEvent event = ClassificationSubmittedEvent.builder()
                .username(null)
                .imageId(1L)
                .species("wasp")
                .build();

        listener.onClassificationSubmitted(event);

        verify(credibilityService, never()).updateUserCredibility(anyString());
        verify(credibilityService, never()).recalculateCredibilityForImageQuery(anyLong(), anyString());
    }

    @Test
    void onClassificationSubmitted_ShouldSkip_WhenImageIdIsNull() {
        ClassificationSubmittedEvent event = ClassificationSubmittedEvent.builder()
                .username("user1")
                .imageId(null)
                .species("wasp")
                .build();

        listener.onClassificationSubmitted(event);

        verify(credibilityService, never()).updateUserCredibility(anyString());
        verify(credibilityService, never()).recalculateCredibilityForImageQuery(anyLong(), anyString());
    }
}
