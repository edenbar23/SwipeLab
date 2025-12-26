package com.swipelab.service.user;

import com.swipelab.model.entity.Classification;
import com.swipelab.model.entity.Image;
import com.swipelab.model.entity.Label;
import com.swipelab.model.entity.User;
import com.swipelab.model.enums.UserRole;
import com.swipelab.repository.ClassificationRepository;
import com.swipelab.repository.UserRepository;
import com.swipelab.util.CredibilityCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
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

    private User testUser;
    private Image testImage;
    private Label testLabel;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .username("testuser")
                .role(UserRole.USER)
                .agreementWithExperts(0.0)
                .majorityAgreementScore(0.0)
                .build();

        testImage = Image.builder().id(1L).build();
        testLabel = Label.builder().id(100L).build();
    }

    @Test
    void updateUserCredibility_ShouldUpdateScores_WhenUserIsRegular() {
        // Arrange
        Long imageId = 1L;
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // Mock Expert Agreement Logic
        Classification userClass = Classification.builder().user(testUser).image(testImage).label(testLabel).build();
        List<Classification> userClasses = Collections.singletonList(userClass);
        when(classificationRepository.findByUser_Username("testuser")).thenReturn(userClasses);

        List<Classification> expertClasses = Collections.singletonList(
                Classification.builder().user(User.builder().role(UserRole.RESEARCHER).build()).build());
        when(classificationRepository.findExpertClassifications()).thenReturn(expertClasses);

        when(credibilityCalculator.calculateCohenKappa(anyList(), anyList())).thenReturn(0.85);

        // Mock Majority Agreement Logic
        // For majority agreement loop
        when(classificationRepository.findByImageId(imageId)).thenReturn(List.of(userClass, userClass)); // Need >1 for
                                                                                                         // majority
        when(credibilityCalculator.calculateMajorityVote(anyList())).thenReturn(testLabel);
        when(credibilityCalculator.calculateMajorityAgreementScore(any(), any())).thenReturn(1.0);

        // Act
        credibilityService.updateUserCredibility("testuser", imageId);

        // Assert
        assertEquals(0.85, testUser.getAgreementWithExperts());
        assertEquals(1.0, testUser.getMajorityAgreementScore());
        verify(userRepository).save(testUser);
    }

    @Test
    void updateUserCredibility_ShouldSkip_WhenUserIsResearcher() {
        testUser.setRole(UserRole.RESEARCHER);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        credibilityService.updateUserCredibility("testuser", 1L);

        verify(classificationRepository, never()).findExpertClassifications();
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void recalculateCredibilityForImage_ShouldUpdateAllUsers() {
        Long imageId = 10L;
        User user1 = User.builder().username("u1").role(UserRole.USER).build();
        User user2 = User.builder().username("u2").role(UserRole.USER).build();

        Classification c1 = Classification.builder().user(user1).build();
        Classification c2 = Classification.builder().user(user2).build();

        when(classificationRepository.findNonExpertClassificationsByImageId(imageId))
                .thenReturn(List.of(c1, c2));

        // Stub find interactions for users
        when(classificationRepository.findByUser_Username("u1")).thenReturn(Collections.emptyList());
        when(classificationRepository.findByUser_Username("u2")).thenReturn(Collections.emptyList());

        credibilityService.recalculateCredibilityForImage(imageId);

        verify(userRepository, times(2)).save(any(User.class));
    }
}
