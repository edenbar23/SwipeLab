package com.swipelab.auth.application;

import com.swipelab.auth.domain.AuthMapper;
import com.swipelab.dto.request.LoginRequest;
import com.swipelab.dto.request.RegisterRequest;
import com.swipelab.dto.request.ResetPasswordRequest;
import com.swipelab.dto.response.AuthResponse;
import com.swipelab.eventing.kafka.KafkaEventPublisher;
import com.swipelab.exception.EmailVerificationException;
import com.swipelab.exception.PasswordResetException;
import com.swipelab.exception.UnauthorizedException;
import com.swipelab.users.domain.User;
import com.swipelab.users.infrastructure.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailService emailService;

    @Mock
    private AuthMapper authMapper;

    @Mock
    private JwtService jwtService;

    @Mock
    private KafkaEventPublisher eventPublisher;

    @InjectMocks
    private AuthenticationService authenticationService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authenticationService, "autoVerifyEmails", false);
    }

    @Test
    void register_ShouldThrowException_WhenEmailExists() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@swipelab.com");

        when(userRepository.existsByEmail("test@swipelab.com")).thenReturn(true);

        assertThrows(RuntimeException.class, () -> authenticationService.register(request));
    }

    @Test
    void register_ShouldThrowException_WhenUsernameExists() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@swipelab.com");
        request.setUsername("testuser");

        when(userRepository.existsByEmail("test@swipelab.com")).thenReturn(false);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(new User()));

        assertThrows(RuntimeException.class, () -> authenticationService.register(request));
    }

    @Test
    void register_ShouldSaveUserAndSendEmail_WhenValidRequest() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@swipelab.com");
        request.setUsername("testuser");
        request.setPassword("password123");

        User user = new User();
        user.setUsername("testuser");
        user.setEmail("test@swipelab.com");

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        when(authMapper.toUser(request)).thenReturn(user);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed-password");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArguments()[0]);
        when(jwtService.generateAccessToken(any())).thenReturn("access");
        when(jwtService.generateRefreshToken(any())).thenReturn("refresh");
        
        AuthResponse response = AuthResponse.builder()
                .accessToken("access")
                .build();
        when(authMapper.toAuthResponse(anyString(), anyString(), any())).thenReturn(response);

        AuthResponse result = authenticationService.register(request);

        assertNotNull(result);
        assertEquals("access", result.getAccessToken());

        verify(emailService, times(1)).sendVerificationEmail(eq("test@swipelab.com"), anyString());
        verify(eventPublisher, times(1)).publish(anyString(), any());
    }

    @Test
    void verifyEmail_ShouldThrowException_WhenTokenInvalid() {
        when(userRepository.findByEmailVerificationToken("invalid-token")).thenReturn(Optional.empty());

        assertThrows(EmailVerificationException.class, () -> authenticationService.verifyEmail("invalid-token"));
    }

    @Test
    void verifyEmail_ShouldThrowException_WhenTokenExpired() {
        User user = new User();
        user.setVerificationTokenExpiry(LocalDateTime.now().minusHours(1));
        when(userRepository.findByEmailVerificationToken("expired-token")).thenReturn(Optional.of(user));

        assertThrows(EmailVerificationException.class, () -> authenticationService.verifyEmail("expired-token"));
    }

    @Test
    void verifyEmail_ShouldVerifyUser_WhenTokenValid() {
        User user = new User();
        user.setEmailVerified(false);
        user.setVerificationTokenExpiry(LocalDateTime.now().plusHours(1));

        when(userRepository.findByEmailVerificationToken("valid-token")).thenReturn(Optional.of(user));

        authenticationService.verifyEmail("valid-token");

        assertTrue(user.getEmailVerified());
        assertNull(user.getEmailVerificationToken());
        verify(userRepository, times(1)).save(user);
    }

    @Test
    void login_ShouldThrowException_WhenUserNotFound() {
        LoginRequest request = new LoginRequest();
        request.setUsername("nonexistent");

        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        assertThrows(UnauthorizedException.class, () -> authenticationService.login(request));
    }

    @Test
    void login_ShouldThrowException_WhenAccountDisabled() {
        LoginRequest request = new LoginRequest();
        request.setUsername("disableduser");

        User user = new User();
        user.setActive(false);

        when(userRepository.findByUsername("disableduser")).thenReturn(Optional.of(user));

        assertThrows(UnauthorizedException.class, () -> authenticationService.login(request));
    }

    @Test
    void login_ShouldThrowException_WhenEmailNotVerified() {
        LoginRequest request = new LoginRequest();
        request.setUsername("unverifieduser");

        User user = new User();
        user.setActive(true);
        user.setAccountLocked(false);
        user.setEmailVerified(false);

        when(userRepository.findByUsername("unverifieduser")).thenReturn(Optional.of(user));

        assertThrows(UnauthorizedException.class, () -> authenticationService.login(request));
    }

    @Test
    void login_ShouldReturnTokens_WhenCredentialsAreValid() {
        LoginRequest request = new LoginRequest();
        request.setUsername("validuser");
        request.setPassword("password");

        User user = new User();
        user.setActive(true);
        user.setAccountLocked(false);
        user.setEmailVerified(true);
        user.setPasswordHash("hashed-password");

        when(userRepository.findByUsername("validuser")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", "hashed-password")).thenReturn(true);
        when(jwtService.generateAccessToken(user)).thenReturn("access");
        when(jwtService.generateRefreshToken(user)).thenReturn("refresh");
        
        AuthResponse response = AuthResponse.builder()
                .accessToken("access")
                .build();
        when(authMapper.toAuthResponse("access", "refresh", user)).thenReturn(response);

        AuthResponse result = authenticationService.login(request);

        assertNotNull(result);
        assertEquals("access", result.getAccessToken());
        verify(userRepository, times(1)).save(user);
    }

    @Test
    void forgotPassword_ShouldSendEmail_WhenUserExists() {
        User user = new User();
        user.setEmail("test@swipelab.com");

        when(userRepository.findByEmail("test@swipelab.com")).thenReturn(Optional.of(user));

        authenticationService.forgotPassword("test@swipelab.com");

        verify(userRepository, times(1)).save(user);
        assertNotNull(user.getResetPasswordToken());
        verify(emailService, times(1)).sendPasswordResetEmail(eq("test@swipelab.com"), anyString());
    }

    @Test
    void forgotPassword_ShouldNotSendEmail_WhenUserDoesNotExist() {
        when(userRepository.findByEmail("nonexistent@swipelab.com")).thenReturn(Optional.empty());

        authenticationService.forgotPassword("nonexistent@swipelab.com");

        verify(userRepository, never()).save(any(User.class));
        verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString());
    }

    @Test
    void resetPassword_ShouldThrowException_WhenTokenInvalid() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("invalid-token");

        when(userRepository.findByResetPasswordToken("invalid-token")).thenReturn(Optional.empty());

        assertThrows(PasswordResetException.class, () -> authenticationService.resetPassword(request));
    }

    @Test
    void resetPassword_ShouldResetPassword_WhenTokenValid() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("valid-token");
        request.setNewPassword("newpassword");

        User user = new User();
        user.setResetTokenExpiry(LocalDateTime.now().plusHours(1));

        when(userRepository.findByResetPasswordToken("valid-token")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("newpassword")).thenReturn("new-hash");

        authenticationService.resetPassword(request);

        assertEquals("new-hash", user.getPasswordHash());
        assertNull(user.getResetPasswordToken());
        verify(userRepository, times(1)).save(user);
    }
}
