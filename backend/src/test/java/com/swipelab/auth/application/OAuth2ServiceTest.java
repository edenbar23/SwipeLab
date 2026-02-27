package com.swipelab.auth.application;

import com.swipelab.auth.infrastructure.AuthProvider;
import com.swipelab.model.enums.UserRole;
import com.swipelab.users.domain.User;
import com.swipelab.users.infrastructure.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OAuth2ServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private OAuth2User oAuth2User;

    @InjectMocks
    private OAuth2Service oAuth2Service;

    @Test
    void processUserLogin_ShouldUpdateExistingUser() {
        String email = "test@example.com";
        String name = "Test User";
        String picture = "pic.jpg";
        String providerId = "12345";

        when(oAuth2User.getAttribute("email")).thenReturn(email);
        when(oAuth2User.getAttribute("name")).thenReturn(name);
        when(oAuth2User.getAttribute("picture")).thenReturn(picture);
        when(oAuth2User.getAttribute("sub")).thenReturn(providerId);

        User existingUser = new User();
        existingUser.setEmail(email);
        existingUser.setProvider(AuthProvider.LOCAL); // Test provider upgrade/link
        
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArguments()[0]);

        User result = oAuth2Service.processUserLogin(oAuth2User);

        assertNotNull(result);
        assertEquals(email, result.getEmail());
        assertEquals(name, result.getDisplayName());
        assertEquals(picture, result.getProfileImageUrl());
        assertEquals(providerId, result.getProviderId());
        assertNotNull(result.getLastLogin());
        
        verify(userRepository).save(existingUser);
    }

    @Test
    void processUserFromIdToken_ShouldCreateNewUser_WhenUserDoesNotExist() {
        String email = "new@example.com";
        String name = "New User";
        String picture = "newpic.jpg";
        String providerId = "67890";

        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArguments()[0]);

        User result = oAuth2Service.processUserFromIdToken(email, name, picture, providerId);

        assertNotNull(result);
        assertEquals(email, result.getUsername());
        assertEquals(email, result.getEmail());
        assertEquals(name, result.getDisplayName());
        assertEquals(picture, result.getProfileImageUrl());
        assertEquals(AuthProvider.GOOGLE, result.getProvider());
        assertEquals(providerId, result.getProviderId());
        assertEquals(UserRole.USER, result.getRole());
        assertTrue(result.getEmailVerified());
        assertTrue(result.getActive());
        assertFalse(result.getAccountLocked());
        assertNotNull(result.getCreatedAt());

        verify(userRepository).save(any(User.class));
    }
}
