package com.swipelab.users.application;

import com.swipelab.classification.domain.Classification;
import com.swipelab.classification.domain.Image;
import com.swipelab.classification.domain.util.CredibilityCalculator;
import com.swipelab.classification.infrastructure.ClassificationRepository;
import com.swipelab.model.enums.UserRole;
import com.swipelab.users.domain.User;
import com.swipelab.users.infrastructure.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CredibilityServiceTest {

    @Mock
    private ClassificationRepository classificationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CredibilityCalculator credibilityCalculator;

    @InjectMocks
    private CredibilityService credibilityService;

    private User regularUser;
    private User researcherUser;
    private Classification userClassification;
    private Classification expertClassification;
    private Image image;

    @BeforeEach
    void setUp() {
        regularUser = User.builder()
                .username("regular")
                .role(UserRole.USER)
                .agreementWithExperts(0.0)
                .majorityAgreementScore(0.0)
                .build();

        researcherUser = User.builder()
                .username("expert")
                .role(UserRole.RESEARCHER)
                .build();

        image = new Image();
        image.setId(1L);

        userClassification = new Classification();
        userClassification.setUsername("regular");
        userClassification.setImage(image);
        userClassification.setUserResponse(Classification.UserResponse.YES);

        expertClassification = new Classification();
        expertClassification.setUsername("expert");
        expertClassification.setImage(image);
        expertClassification.setUserResponse(Classification.UserResponse.YES);
    }

    @Test
    void updateUserCredibility_ShouldSkip_IfResearcher() {
        when(userRepository.findByUsername("expert")).thenReturn(Optional.of(researcherUser));

        credibilityService.updateUserCredibility("expert", 1L);

        verify(classificationRepository, never()).findByUsername(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateUserCredibility_ShouldCalculateAndSave_IfRegularUser() {
        when(userRepository.findByUsername("regular")).thenReturn(Optional.of(regularUser));
        
        // Mock expert agreement
        when(classificationRepository.findByUsername("regular")).thenReturn(Collections.singletonList(userClassification));
        when(classificationRepository.findExpertClassifications()).thenReturn(Collections.singletonList(expertClassification));
        when(credibilityCalculator.calculateCohenKappa(anyList(), anyList())).thenReturn(0.85);

        // Mock majority agreement
        when(classificationRepository.findByImageId(1L)).thenReturn(Arrays.asList(userClassification, expertClassification));
        when(credibilityCalculator.calculateMajorityVote(anyList())).thenReturn(Classification.UserResponse.YES);
        when(credibilityCalculator.calculateMajorityAgreementScore(userClassification, Classification.UserResponse.YES)).thenReturn(1.0);

        credibilityService.updateUserCredibility("regular", 1L);

        assertEquals(0.85, regularUser.getAgreementWithExperts());
        assertEquals(1.0, regularUser.getMajorityAgreementScore());
        verify(userRepository, times(1)).save(regularUser);
    }

    @Test
    void getCredibilityStats_ShouldReturnStats() {
        when(userRepository.findByUsername("regular")).thenReturn(Optional.of(regularUser));
        when(classificationRepository.findByUsername("regular")).thenReturn(Collections.singletonList(userClassification));
        when(classificationRepository.findExpertClassifications()).thenReturn(Collections.singletonList(expertClassification));

        regularUser.setAgreementWithExperts(0.8);
        regularUser.setMajorityAgreementScore(0.9);

        CredibilityService.CredibilityStats stats = credibilityService.getCredibilityStats("regular");

        assertNotNull(stats);
        assertEquals("regular", stats.getUsername());
        assertEquals(1, stats.getTotalClassifications());
        assertEquals(0.8, stats.getExpertAgreementScore());
        assertEquals(0.9, stats.getMajorityAgreementScore());
        assertEquals(1, stats.getImagesInCommonWithExperts());
    }
}
