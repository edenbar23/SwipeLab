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

import com.swipelab.classification.infrastructure.ClassificationRepository;
import com.swipelab.users.application.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CredibilityWeightedThresholdPolicyTest {

    @Mock
    private ClassificationRepository classificationRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private CredibilityWeightedThresholdPolicy policy;

    private final Long IMAGE_ID = 100L;
    private final Long TASK_ID = 200L;
    private final String QUERY_SPECIES = "Corn";

    @BeforeEach
    void setUp() {
    }

    @Test
    void testEvaluateThreshold_NoClassifications_ReturnsEmpty() {
        when(classificationRepository.findByImageIdAndQuerySpecies(IMAGE_ID, QUERY_SPECIES))
                .thenReturn(Collections.emptyList());

        Optional<ThresholdPolicy.ThresholdResult> result = policy.evaluateThreshold(IMAGE_ID, TASK_ID, QUERY_SPECIES, 3.0);

        assertTrue(result.isEmpty());
    }

    @Test
    void testEvaluateThreshold_InvalidThreshold_ReturnsEmpty() {
        Optional<ThresholdPolicy.ThresholdResult> result = policy.evaluateThreshold(IMAGE_ID, TASK_ID, QUERY_SPECIES, 2.0);
        assertTrue(result.isEmpty());

        Optional<ThresholdPolicy.ThresholdResult> result2 = policy.evaluateThreshold(IMAGE_ID, TASK_ID, QUERY_SPECIES, 21.0);
        assertTrue(result2.isEmpty());
    }

    @Test
    void testEvaluateThreshold_ThreeExperts_ReachesThresholdOfThree() {
        // Arrange
        Classification c1 = Classification.builder().username("expert1").userRole("RESEARCHER").userResponse(Classification.UserResponse.YES).build();
        Classification c2 = Classification.builder().username("expert2").userRole("RESEARCHER").userResponse(Classification.UserResponse.YES).build();
        Classification c3 = Classification.builder().username("expert3").userRole("RESEARCHER").userResponse(Classification.UserResponse.YES).build();

        when(classificationRepository.findByImageIdAndQuerySpecies(IMAGE_ID, QUERY_SPECIES))
                .thenReturn(Arrays.asList(c1, c2, c3));

        // Act
        Optional<ThresholdPolicy.ThresholdResult> result = policy.evaluateThreshold(IMAGE_ID, TASK_ID, QUERY_SPECIES, 3.0);

        // Assert
        assertTrue(result.isPresent());
        assertEquals("YES", result.get().winningDecision());
        assertEquals(3.0, result.get().finalScore());
    }

    @Test
    void testEvaluateThreshold_MixOfCredibilities_ReachesThreshold() {
        // Arrange
        // target: 2.0
        // expert (weight 1.0) votes YES
        // user1 (score 75 -> weight 0.75) votes YES
        // user2 (score 25 -> weight 0.25) votes YES
        // total YES = 3.0
        
        Classification expert1 = Classification.builder().username("expert1").userRole("RESEARCHER").userResponse(Classification.UserResponse.YES).build();
        Classification expert2 = Classification.builder().username("expert2").userRole("RESEARCHER").userResponse(Classification.UserResponse.YES).build();
        Classification user1 = Classification.builder().username("user1").userRole("USER").userResponse(Classification.UserResponse.YES).build();
        Classification user2 = Classification.builder().username("user2").userRole("USER").userResponse(Classification.UserResponse.YES).build();

        when(classificationRepository.findByImageIdAndQuerySpecies(IMAGE_ID, QUERY_SPECIES))
                .thenReturn(Arrays.asList(expert1, expert2, user1, user2));
                
        when(userService.getUserCredibility("user1")).thenReturn(75.0);
        when(userService.getUserCredibility("user2")).thenReturn(25.0);

        // Act
        Optional<ThresholdPolicy.ThresholdResult> result = policy.evaluateThreshold(IMAGE_ID, TASK_ID, QUERY_SPECIES, 3.0);

        // Assert
        assertTrue(result.isPresent());
        assertEquals("YES", result.get().winningDecision());
        assertEquals(3.0, result.get().finalScore());
    }

    @Test
    void testEvaluateThreshold_SplitVotes_DoesNotReachThreshold() {
        // Arrange
        // target: 3.0
        // expert (1.0) votes YES
        // expert (1.0) votes NO
        // expert (1.0) votes YES
        // user1 (100 -> 1.0) votes YES
        // total YES = 3.0, NO = 1.0. YES should win
        
        Classification expert1 = Classification.builder().username("expert1").userRole("RESEARCHER").userResponse(Classification.UserResponse.YES).build();
        Classification expert2 = Classification.builder().username("expert2").userRole("RESEARCHER").userResponse(Classification.UserResponse.NO).build();
        Classification expert3 = Classification.builder().username("expert3").userRole("RESEARCHER").userResponse(Classification.UserResponse.YES).build();
        Classification user1 = Classification.builder().username("user1").userRole("USER").userResponse(Classification.UserResponse.YES).build();

        when(classificationRepository.findByImageIdAndQuerySpecies(IMAGE_ID, QUERY_SPECIES))
                .thenReturn(Arrays.asList(expert1, expert2, expert3, user1));
                
        when(userService.getUserCredibility("user1")).thenReturn(100.0);

        // Act
        Optional<ThresholdPolicy.ThresholdResult> result = policy.evaluateThreshold(IMAGE_ID, TASK_ID, QUERY_SPECIES, 3.0);

        // Assert
        assertTrue(result.isPresent());
        assertEquals("YES", result.get().winningDecision());
        assertEquals(3.0, result.get().finalScore());
    }
    
    @Test
    void testEvaluateThreshold_DoesNotReachThreshold() {
        // Arrange
        // target: 3.0
        // 2 experts vote YES = 2.0
        
        Classification expert1 = Classification.builder().username("expert1").userRole("RESEARCHER").userResponse(Classification.UserResponse.YES).build();
        Classification expert2 = Classification.builder().username("expert2").userRole("RESEARCHER").userResponse(Classification.UserResponse.YES).build();

        when(classificationRepository.findByImageIdAndQuerySpecies(IMAGE_ID, QUERY_SPECIES))
                .thenReturn(Arrays.asList(expert1, expert2));

        // Act
        Optional<ThresholdPolicy.ThresholdResult> result = policy.evaluateThreshold(IMAGE_ID, TASK_ID, QUERY_SPECIES, 3.0);

        // Assert
        assertTrue(result.isEmpty());
    }
}
