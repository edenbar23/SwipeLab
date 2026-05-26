package com.swipelab.users.application;

import com.swipelab.auth.application.SecurityAuthorizationService;
import com.swipelab.auth.domain.AuthMapper;
import com.swipelab.dto.response.UserProfileResponse;
import com.swipelab.exception.ResourceNotFoundException;
import com.swipelab.model.enums.UserRole;
import com.swipelab.model.enums.UserStatus;
import com.swipelab.users.domain.User;
import com.swipelab.users.events.UserStatusChangedEvent;
import com.swipelab.users.infrastructure.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("UserService.banUser")
class UserServiceBanTest {

    @Mock private UserRepository userRepository;
    @Mock private AuthMapper authMapper;
    @Mock private SecurityAuthorizationService securityAuthorizationService;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private UserService userService;

    private User adminUser;
    private User targetUser;

    @BeforeEach
    void setUp() {
        adminUser = User.builder()
                .username("admin")
                .email("admin@test.com")
                .role(UserRole.RESEARCHER)
                .status(UserStatus.ACTIVE)
                .build();

        targetUser = User.builder()
                .username("target")
                .email("target@test.com")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .strikeCount(0)
                .warningCount(0)
                .consecutiveCorrectGolds(0)
                .build();

        // Stub SecurityContext so getCurrentUser() works
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn(adminUser);
        SecurityContext ctx = mock(SecurityContext.class);
        when(ctx.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(ctx);
    }

    // ── Happy flow ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Banning a regular user sets status=BANNED, active=false, accountLocked=true and publishes UserStatusChangedEvent")
    void banRegularUser_success() {
        when(securityAuthorizationService.isSuperAdmin("target")).thenReturn(false);
        when(userRepository.findByUsername("target")).thenReturn(Optional.of(targetUser));
        when(userRepository.save(any(User.class))).thenReturn(targetUser);
        when(authMapper.toUserProfileResponse(any())).thenReturn(UserProfileResponse.builder().build());

        userService.banUser("target");

        // Core state assertions
        assertThat(targetUser.getStatus()).isEqualTo(UserStatus.BANNED);
        assertThat(targetUser.getActive()).isFalse();
        // accountLocked must be set — mirrors auto-ban path and enables BannedUserFilter
        assertThat(targetUser.getAccountLocked()).isTrue();
        verify(userRepository).save(targetUser);

        // UserStatusChangedEvent must be published so recipients module removes the user
        ArgumentCaptor<UserStatusChangedEvent> eventCaptor =
                ArgumentCaptor.forClass(UserStatusChangedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        UserStatusChangedEvent published = eventCaptor.getValue();
        assertThat(published.getUsername()).isEqualTo("target");
        assertThat(published.isActive()).isFalse();
    }

    // ── Edge cases ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Banning Super Admin throws IllegalArgumentException")
    void banSuperAdmin_throws() {
        when(securityAuthorizationService.isSuperAdmin("superadmin")).thenReturn(true);

        assertThatThrownBy(() -> userService.banUser("superadmin"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Super Admin cannot be banned");

        verifyNoMoreInteractions(userRepository);
    }

    @Test
    @DisplayName("Banning yourself throws IllegalArgumentException")
    void banSelf_throws() {
        when(securityAuthorizationService.isSuperAdmin("admin")).thenReturn(false);

        assertThatThrownBy(() -> userService.banUser("admin"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot ban yourself");
    }

    @Test
    @DisplayName("Banning unknown user throws ResourceNotFoundException")
    void banUnknownUser_throws() {
        when(securityAuthorizationService.isSuperAdmin("ghost")).thenReturn(false);
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.banUser("ghost"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
