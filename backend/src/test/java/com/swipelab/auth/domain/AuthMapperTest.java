package com.swipelab.auth.domain;

import com.swipelab.auth.infrastructure.AuthProvider;
import com.swipelab.dto.request.RegisterRequest;
import com.swipelab.dto.response.AuthResponse;
import com.swipelab.dto.response.UserProfileResponse;
import com.swipelab.model.enums.UserRole;
import com.swipelab.users.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AuthMapperTest {

    private AuthMapper authMapper;

    @BeforeEach
    void setUp() {
        authMapper = new AuthMapper();
    }

    @Test
    void toUser_ShouldMapCorrectly() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("testuser");
        request.setEmail("test@swipelab.com");
        request.setDisplayName("Test User");

        User user = authMapper.toUser(request);

        assertNotNull(user);
        assertEquals("testuser", user.getUsername());
        assertEquals("test@swipelab.com", user.getEmail());
        assertEquals("Test User", user.getDisplayName());
        assertEquals(UserRole.USER, user.getRole());
        assertEquals(AuthProvider.LOCAL, user.getProvider());
        assertTrue(user.getActive());
        assertFalse(user.getAccountLocked());
        assertFalse(user.getEmailVerified());
        assertEquals(0.0, user.getCredibilityScore());
        assertEquals(0L, user.getScore());
        assertEquals("", user.getBadges());
        assertEquals("UNRANKED", user.getRank());
    }

    @Test
    void toUser_ShouldReturnNull_WhenRequestIsNull() {
        assertNull(authMapper.toUser(null));
    }

    @Test
    void toUserProfileResponse_ShouldMapCorrectly() {
        User user = new User();
        user.setUsername("testuser");
        user.setEmail("test@swipelab.com");
        user.setDisplayName("Test User");
        user.setProfileImageUrl("pic.jpg");
        user.setRole(UserRole.USER);
        user.setScore(100L);
        user.setBadges("badge1,badge2");
        user.setRank("EXPERT");

        UserProfileResponse response = authMapper.toUserProfileResponse(user);

        assertNotNull(response);
        assertEquals("testuser", response.getUsername());
        assertEquals("test@swipelab.com", response.getEmail());
        assertEquals("Test User", response.getDisplayName());
        assertEquals("pic.jpg", response.getProfileImageUrl());
        assertEquals(UserRole.USER, response.getRole());
        assertEquals(100L, response.getScore());
        
        List<String> badges = response.getBadges();
        assertNotNull(badges);
        assertEquals(2, badges.size());
        assertTrue(badges.containsAll(Arrays.asList("badge1", "badge2")));
        
        assertEquals("EXPERT", response.getRank());
    }

    @Test
    void toUserProfileResponse_ShouldHandleNullsCorrectly() {
        User user = new User();
        user.setUsername("testuser");

        UserProfileResponse response = authMapper.toUserProfileResponse(user);

        assertNotNull(response);
        assertEquals(0L, response.getScore());
        assertTrue(response.getBadges().isEmpty());
        assertEquals("UNRANKED", response.getRank());
    }

    @Test
    void toUserProfileResponse_ShouldReturnNull_WhenUserIsNull() {
        assertNull(authMapper.toUserProfileResponse(null));
    }

    @Test
    void toAuthResponse_ShouldMapCorrectly() {
        User user = new User();
        user.setUsername("testuser");

        AuthResponse response = authMapper.toAuthResponse("access", "refresh", user);

        assertNotNull(response);
        assertEquals("access", response.getAccessToken());
        assertEquals("refresh", response.getRefreshToken());
        assertEquals(86400, response.getExpiresIn());
        assertEquals("Authentication successful", response.getMessage());
        assertNotNull(response.getUser());
        assertEquals("testuser", response.getUser().getUsername());
    }
}
