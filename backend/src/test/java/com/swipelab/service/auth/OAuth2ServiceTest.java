package com.swipelab.service.auth;

import com.swipelab.model.entity.User;
import com.swipelab.model.enums.AuthProvider;
import com.swipelab.model.enums.UserRole;
import com.swipelab.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

    @InjectMocks
    private OAuth2Service oAuth2Service;

    @Mock
    private OAuth2User oAuth2User;

    @Test
    void processUserLogin_ShouldCreateNewUser_WhenUserDoesNotExist() {
        // Arrange
        String email = "new@example.com";
        String name = "New User";
        String picture = "pic.url";
        String sub = "google_123";

        when(oAuth2User.getAttribute("email")).thenReturn(email);
        when(oAuth2User.getAttribute("name")).thenReturn(name);
        when(oAuth2User.getAttribute("picture")).thenReturn(picture);
        when(oAuth2User.getAttribute("sub")).thenReturn(sub);

        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        User result = oAuth2Service.processUserLogin(oAuth2User);

        // Assert
        assertNotNull(result);
        assertEquals(email, result.getEmail());
        assertEquals(name, result.getDisplayName());
        assertEquals(picture, result.getProfileImageUrl());
        assertEquals(AuthProvider.GOOGLE, result.getProvider());
        assertEquals(UserRole.USER, result.getRole());
        assertTrue(result.getEmailVerified());

        verify(userRepository).save(any(User.class));
    }

    @Test
    void processUserLogin_ShouldUpdateExistingUser_WhenUserExists() {
        // Arrange
        String email = "existing@example.com";
        String name = "New Google Name";
        String picture = "new_pic.url";
        String sub = "google_123";

        // Logic only updates IF null. So let's test that it preserves existing if
        // present
        User existingUser = User.builder()
                .username(email)
                .email(email)
                .displayName("Old Name")
                .profileImageUrl("old_pic.url")
                .provider(AuthProvider.GOOGLE)
                .role(UserRole.USER)
                .build();

        when(oAuth2User.getAttribute("email")).thenReturn(email);
        when(oAuth2User.getAttribute("name")).thenReturn(name);
        when(oAuth2User.getAttribute("picture")).thenReturn(picture);
        when(oAuth2User.getAttribute("sub")).thenReturn(sub);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        User result = oAuth2Service.processUserLogin(oAuth2User);

        // Assert
        assertEquals("Old Name", result.getDisplayName()); // Code only updates if null
        assertEquals("old_pic.url", result.getProfileImageUrl()); // Code only updates if null
        assertNotNull(result.getLastLogin());

        verify(userRepository).save(existingUser);
    }

    @Test
    void processUserLogin_ShouldLinkProviderId_WhenLocalUserExists() {
        // Arrange
        String email = "local@example.com";
        String sub = "google_123";

        User localUser = User.builder()
                .username(email)
                .email(email)
                .provider(AuthProvider.LOCAL)
                .providerId(null)
                .build();

        when(oAuth2User.getAttribute("email")).thenReturn(email);
        when(oAuth2User.getAttribute("name")).thenReturn("Name"); // Stub required calls
        when(oAuth2User.getAttribute("picture")).thenReturn("pic"); // Stub required calls
        when(oAuth2User.getAttribute("sub")).thenReturn(sub);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(localUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        User result = oAuth2Service.processUserLogin(oAuth2User);

        // Assert
        assertEquals("google_123", result.getProviderId()); // Should link provider ID
        verify(userRepository).save(localUser);
    }
}
