package com.swipelab.service.auth;

import com.swipelab.dto.request.LoginRequest;
import com.swipelab.dto.request.RegisterRequest;
import com.swipelab.dto.request.ResetPasswordRequest;
import com.swipelab.dto.response.AuthResponse;
import com.swipelab.exception.EmailVerificationException;
import com.swipelab.exception.UnauthorizedException;
import com.swipelab.mapper.AuthMapper;
import com.swipelab.model.entity.User;
import com.swipelab.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

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

    @InjectMocks
    private AuthenticationService authService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .username("testuser")
                .email("test@example.com")
                .passwordHash("encoded_password")
                .active(true)
                .emailVerified(true)
                .accountLocked(false)
                .build();
    }

    @Test
    void register_Success() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setEmail("new@example.com");
        request.setPassword("password");

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        when(authMapper.toUser(request)).thenReturn(testUser);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded_pass");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtService.generateAccessToken(any(User.class))).thenReturn("access_token");
        when(jwtService.generateRefreshToken(any(User.class))).thenReturn("refresh_token");
        when(authMapper.toAuthResponse(anyString(), anyString(), any(User.class)))
                .thenReturn(AuthResponse.builder().accessToken("access_token").build());

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("access_token", response.getAccessToken());
        verify(emailService).sendVerificationEmail(anyString(), anyString()); // Verify email sent
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_Fail_EmailExists() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@example.com");

        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        assertThrows(RuntimeException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void login_Success() {
        LoginRequest request = new LoginRequest();
        request.setUsername("testuser");
        request.setPassword("password");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password", "encoded_password")).thenReturn(true);
        when(jwtService.generateAccessToken(testUser)).thenReturn("access");
        when(jwtService.generateRefreshToken(testUser)).thenReturn("refresh");
        when(authMapper.toAuthResponse(eq("access"), eq("refresh"), eq(testUser)))
                .thenReturn(AuthResponse.builder().accessToken("access").build());

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        verify(userRepository).save(testUser); // Last login updated
    }

    @Test
    void login_Fail_WrongPassword() {
        LoginRequest request = new LoginRequest();
        request.setUsername("testuser");
        request.setPassword("wrong");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrong", "encoded_password")).thenReturn(false);

        assertThrows(UnauthorizedException.class, () -> authService.login(request));
    }

    @Test
    void login_Fail_EmailNotVerified() {
        testUser.setEmailVerified(false);
        LoginRequest request = new LoginRequest();
        request.setUsername("testuser");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        assertThrows(UnauthorizedException.class, () -> authService.login(request));
    }

    @Test
    void verifyEmail_Success() {
        String token = "valid_token";
        testUser.setEmailVerified(false);
        testUser.setVerificationTokenExpiry(LocalDateTime.now().plusHours(1));

        when(userRepository.findByEmailVerificationToken(token)).thenReturn(Optional.of(testUser));

        authService.verifyEmail(token);

        assertTrue(testUser.getEmailVerified());
        assertNull(testUser.getEmailVerificationToken());
        verify(userRepository).save(testUser);
    }

    @Test
    void verifyEmail_Fail_Expired() {
        String token = "expired_token";
        testUser.setVerificationTokenExpiry(LocalDateTime.now().minusHours(1));

        when(userRepository.findByEmailVerificationToken(token)).thenReturn(Optional.of(testUser));

        assertThrows(EmailVerificationException.class, () -> authService.verifyEmail(token));
    }

    @Test
    void forgotPassword_Success_UserExists() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        authService.forgotPassword("test@example.com");

        assertNotNull(testUser.getResetPasswordToken());
        verify(emailService).sendPasswordResetEmail(eq("test@example.com"), anyString());
        verify(userRepository).save(testUser);
    }

    @Test
    void resetPassword_Success() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("reset_token");
        request.setNewPassword("new_pass");

        testUser.setResetPasswordToken("reset_token");
        testUser.setResetTokenExpiry(LocalDateTime.now().plusHours(1));

        when(userRepository.findByResetPasswordToken("reset_token")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode("new_pass")).thenReturn("new_encoded_pass");

        authService.resetPassword(request);

        assertNull(testUser.getResetPasswordToken());
        assertEquals("new_encoded_pass", testUser.getPasswordHash());
        verify(userRepository).save(testUser);
    }
}
