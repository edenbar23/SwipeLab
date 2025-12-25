package com.swipelab.service.user;

import com.swipelab.dto.request.UpdateProfileRequest;
import com.swipelab.dto.response.UserProfileResponse;
import com.swipelab.exception.ResourceNotFoundException;
import com.swipelab.mapper.AuthMapper;
import com.swipelab.model.entity.User;
import com.swipelab.model.enums.UserRole;
import com.swipelab.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuthMapper authMapper;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private MockedStatic<SecurityContextHolder> mockedSecurityContextHolder;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .username("testuser")
                .email("test@example.com")
                .role(UserRole.USER)
                .displayName("Test User")
                .build();

        // Initialize static mock for SecurityContextHolder
        mockedSecurityContextHolder = mockStatic(SecurityContextHolder.class);
    }

    @AfterEach
    void tearDown() {
        // Close static mock after each test to avoid memory leaks or interference
        mockedSecurityContextHolder.close();
    }

    @Test
    void getUserProfile_Success() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(authMapper.toUserProfileResponse(testUser))
                .thenReturn(UserProfileResponse.builder().username("testuser").build());

        UserProfileResponse response = userService.getUserProfile("testuser");

        assertNotNull(response);
        assertEquals("testuser", response.getUsername());
        verify(userRepository).findByUsername("testuser");
    }

    @Test
    void getUserProfile_NotFound() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.getUserProfile("unknown"));
    }

    @Test
    void getCurrentUserProfile_Success() {
        // Mock Security Context behavior
        mockedSecurityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(testUser); // Principal is the User entity directly

        when(authMapper.toUserProfileResponse(testUser))
                .thenReturn(UserProfileResponse.builder().username("testuser").build());

        UserProfileResponse response = userService.getCurrentUserProfile();

        assertNotNull(response);
        assertEquals("testuser", response.getUsername());
    }

    @Test
    void getCurrentUserProfile_AuthenticatedUserNotFoundInRepo_WhenPrincipalIsString() {
        mockedSecurityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn("testuser"); // Principal is String
        when(authentication.getName()).thenReturn("testuser");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> userService.getCurrentUserProfile());
    }

    @Test
    void updateUserProfile_Success() {
        // Mock Security Context
        mockedSecurityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(testUser);

        // Mock Update
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setDisplayName("New Name");
        request.setProfileImageUrl("http://image.com/new.png");

        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(authMapper.toUserProfileResponse(any(User.class)))
                .thenReturn(UserProfileResponse.builder().displayName("New Name").build());

        UserProfileResponse response = userService.updateUserProfile(request);

        assertNotNull(response);
        assertEquals("New Name", response.getDisplayName());

        // Verify user object was updated before save
        assertEquals("New Name", testUser.getDisplayName());
        assertEquals("http://image.com/new.png", testUser.getProfileImageUrl());

        verify(userRepository).save(testUser);
    }
}
