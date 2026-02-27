package com.swipelab.users.application;

import com.swipelab.auth.domain.AuthMapper;
import com.swipelab.dto.request.UpdateProfileRequest;
import com.swipelab.dto.response.UserProfileResponse;
import com.swipelab.exception.ResourceNotFoundException;
import com.swipelab.users.domain.User;
import com.swipelab.users.infrastructure.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuthMapper authMapper;

    @InjectMocks
    private UserService userService;

    private User user;
    private UserProfileResponse profileResponse;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setUsername("testuser");
        user.setCredibilityScore(0.85);
        user.setActive(true);

        profileResponse = UserProfileResponse.builder()
                .username("testuser")
                .build();
    }

    @Test
    void getUserProfile_ShouldReturnProfile_WhenUserExists() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(authMapper.toUserProfileResponse(user)).thenReturn(profileResponse);

        UserProfileResponse response = userService.getUserProfile("testuser");

        assertNotNull(response);
        assertEquals("testuser", response.getUsername());
    }

    @Test
    void getUserProfile_ShouldThrowException_WhenUserNotExists() {
        when(userRepository.findByUsername("nonexist")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.getUserProfile("nonexist"));
    }

    @Test
    void getCurrentUserProfile_ShouldReturnProfile() {
        // Mock Security Context
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);

        when(authMapper.toUserProfileResponse(user)).thenReturn(profileResponse);

        UserProfileResponse response = userService.getCurrentUserProfile();

        assertNotNull(response);
        assertEquals("testuser", response.getUsername());
    }

    @Test
    void updateUserProfile_ShouldUpdateAndReturnProfile() {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setDisplayName("New Name");
        request.setProfileImageUrl("http://image.com/new.jpg");

        when(userRepository.save(any(User.class))).thenReturn(user);
        when(authMapper.toUserProfileResponse(user)).thenReturn(profileResponse);

        UserProfileResponse response = userService.updateUserProfile(request);

        assertNotNull(response);
        verify(userRepository, times(1)).save(argThat(u -> 
                "New Name".equals(u.getDisplayName()) && 
                "http://image.com/new.jpg".equals(u.getProfileImageUrl())
        ));
    }

    @Test
    void getUserCredibility_ShouldReturnScore() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        Double score = userService.getUserCredibility("testuser");

        assertEquals(0.85, score);
    }

    @Test
    void getAllUsers_ShouldReturnList() {
        when(userRepository.findAll()).thenReturn(Collections.singletonList(user));
        when(authMapper.toUserProfileResponse(user)).thenReturn(profileResponse);

        List<UserProfileResponse> responses = userService.getAllUsers();

        assertEquals(1, responses.size());
    }

    @Test
    void banUser_ShouldSetActiveFalse() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(authMapper.toUserProfileResponse(user)).thenReturn(profileResponse);

        userService.banUser("testuser");

        verify(userRepository, times(1)).save(argThat(u -> !u.getActive()));
    }

    @Test
    void unbanUser_ShouldSetActiveTrue() {
        user.setActive(false);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(authMapper.toUserProfileResponse(user)).thenReturn(profileResponse);

        userService.unbanUser("testuser");

        verify(userRepository, times(1)).save(argThat(User::getActive));
    }
}
