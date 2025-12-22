package com.swipelab.service.auth;

import com.swipelab.dto.request.*;
import com.swipelab.dto.response.AuthResponse;
import com.swipelab.exception.EmailVerificationException;
import com.swipelab.exception.PasswordResetException;
import com.swipelab.exception.UnauthorizedException;
import com.swipelab.mapper.AuthMapper;
import com.swipelab.model.entity.User;
import com.swipelab.model.enums.AuthProvider;
import com.swipelab.model.enums.UserRole;
import com.swipelab.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Authentication Service Tests
 * 
 * Tests for user registration, login, email verification, and password reset.
 * 
 * What this test should cover:
 * - User registration (success, duplicate email, duplicate username)
 * - Email verification (success, invalid token, expired token, already verified)
 * - Resend verification email
 * - User login (success, wrong password, inactive account, locked account, unverified email)
 * - Token refresh
 * - Logout
 * - Forgot password (existing user, non-existing user)
 * - Reset password (success, invalid token, expired token)
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Authentication Service Tests")
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
    private AuthenticationService authenticationService;

    private User testUser;
    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private AuthResponse authResponse;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .username("testuser")
                .email("test@example.com")
                .displayName("Test User")
                .passwordHash("hashed-password")
                .role(UserRole.USER)
                .active(true)
                .accountLocked(false)
                .emailVerified(true)
                .provider(AuthProvider.LOCAL)
                .build();

        registerRequest = new RegisterRequest();
        registerRequest.setUsername("newuser");
        registerRequest.setEmail("newuser@example.com");
        registerRequest.setPassword("password123");
        registerRequest.setDisplayName("New User");

        loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("password123");

        authResponse = AuthResponse.builder()
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .expiresIn(3600L)
                .message("Success")
                .build();
    }

    @Test
    @DisplayName("Should register new user successfully")
    void testRegister_Success() {
        // Given
        User newUser = User.builder()
                .username(registerRequest.getUsername())
                .email(registerRequest.getEmail())
                .displayName(registerRequest.getDisplayName())
                .build();

        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
        when(userRepository.findByUsername(registerRequest.getUsername())).thenReturn(Optional.empty());
        when(authMapper.toUser(registerRequest)).thenReturn(newUser);
        when(passwordEncoder.encode(registerRequest.getPassword())).thenReturn("hashed-password");
        when(userRepository.save(any(User.class))).thenReturn(newUser);
        when(jwtService.generateAccessToken(any(User.class))).thenReturn("access-token");
        when(jwtService.generateRefreshToken(any(User.class))).thenReturn("refresh-token");
        when(authMapper.toAuthResponse(anyString(), anyString(), any(User.class)))
                .thenReturn(authResponse);

        // When
        AuthResponse result = authenticationService.register(registerRequest);

        // Then
        assertNotNull(result);
        verify(userRepository).existsByEmail(registerRequest.getEmail());
        verify(userRepository).findByUsername(registerRequest.getUsername());
        verify(passwordEncoder).encode(registerRequest.getPassword());
        verify(userRepository).save(argThat(user -> 
            user.getEmailVerificationToken() != null &&
            user.getVerificationTokenExpiry() != null
        ));
        verify(emailService).sendVerificationEmail(eq(registerRequest.getEmail()), anyString());
        verify(jwtService).generateAccessToken(any(User.class));
        verify(jwtService).generateRefreshToken(any(User.class));
    }

    @Test
    @DisplayName("Should throw exception when email already exists")
    void testRegister_EmailAlreadyExists() {
        // Given
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(true);

        // When & Then
        assertThrows(RuntimeException.class, () -> 
            authenticationService.register(registerRequest)
        );
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw exception when username already exists")
    void testRegister_UsernameAlreadyExists() {
        // Given
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
        when(userRepository.findByUsername(registerRequest.getUsername()))
                .thenReturn(Optional.of(testUser));

        // When & Then
        assertThrows(RuntimeException.class, () -> 
            authenticationService.register(registerRequest)
        );
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should verify email successfully")
    void testVerifyEmail_Success() {
        // Given
        String token = UUID.randomUUID().toString();
        testUser.setEmailVerificationToken(token);
        testUser.setVerificationTokenExpiry(LocalDateTime.now().plusHours(1));
        testUser.setEmailVerified(false);

        when(userRepository.findByEmailVerificationToken(token))
                .thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        authenticationService.verifyEmail(token);

        // Then
        verify(userRepository).findByEmailVerificationToken(token);
        verify(userRepository).save(argThat(user -> 
            user.getEmailVerified() == true &&
            user.getEmailVerificationToken() == null &&
            user.getVerificationTokenExpiry() == null
        ));
    }

    @Test
    @DisplayName("Should throw exception when verification token is invalid")
    void testVerifyEmail_InvalidToken() {
        // Given
        String token = "invalid-token";
        when(userRepository.findByEmailVerificationToken(token))
                .thenReturn(Optional.empty());

        // When & Then
        assertThrows(EmailVerificationException.class, () -> 
            authenticationService.verifyEmail(token)
        );
    }

    @Test
    @DisplayName("Should throw exception when verification token is expired")
    void testVerifyEmail_ExpiredToken() {
        // Given
        String token = UUID.randomUUID().toString();
        testUser.setEmailVerificationToken(token);
        testUser.setVerificationTokenExpiry(LocalDateTime.now().minusHours(1));

        when(userRepository.findByEmailVerificationToken(token))
                .thenReturn(Optional.of(testUser));

        // When & Then
        assertThrows(EmailVerificationException.class, () -> 
            authenticationService.verifyEmail(token)
        );
    }

    @Test
    @DisplayName("Should throw exception when email already verified")
    void testVerifyEmail_AlreadyVerified() {
        // Given
        String token = UUID.randomUUID().toString();
        testUser.setEmailVerificationToken(token);
        testUser.setEmailVerified(true);

        when(userRepository.findByEmailVerificationToken(token))
                .thenReturn(Optional.of(testUser));

        // When & Then
        assertThrows(EmailVerificationException.class, () -> 
            authenticationService.verifyEmail(token)
        );
    }

    @Test
    @DisplayName("Should resend verification email successfully")
    void testResendVerificationEmail_Success() {
        // Given
        testUser.setEmailVerified(false);
        when(userRepository.findByEmail(testUser.getEmail()))
                .thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        authenticationService.resendVerificationEmail(testUser.getEmail());

        // Then
        verify(userRepository).findByEmail(testUser.getEmail());
        verify(userRepository).save(argThat(user -> 
            user.getEmailVerificationToken() != null &&
            user.getVerificationTokenExpiry() != null
        ));
        verify(emailService).sendVerificationEmail(eq(testUser.getEmail()), anyString());
    }

    @Test
    @DisplayName("Should throw exception when resending to already verified email")
    void testResendVerificationEmail_AlreadyVerified() {
        // Given
        testUser.setEmailVerified(true);
        when(userRepository.findByEmail(testUser.getEmail()))
                .thenReturn(Optional.of(testUser));

        // When & Then
        assertThrows(EmailVerificationException.class, () -> 
            authenticationService.resendVerificationEmail(testUser.getEmail())
        );
        verify(emailService, never()).sendVerificationEmail(anyString(), anyString());
    }

    @Test
    @DisplayName("Should login successfully with valid credentials")
    void testLogin_Success() {
        // Given
        when(userRepository.findByUsername(loginRequest.getUsername()))
                .thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(loginRequest.getPassword(), testUser.getPasswordHash()))
                .thenReturn(true);
        when(jwtService.generateAccessToken(testUser)).thenReturn("access-token");
        when(jwtService.generateRefreshToken(testUser)).thenReturn("refresh-token");
        when(authMapper.toAuthResponse(anyString(), anyString(), any(User.class)))
                .thenReturn(authResponse);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        AuthResponse result = authenticationService.login(loginRequest);

        // Then
        assertNotNull(result);
        verify(userRepository).findByUsername(loginRequest.getUsername());
        verify(passwordEncoder).matches(loginRequest.getPassword(), testUser.getPasswordHash());
        verify(jwtService).generateAccessToken(testUser);
        verify(jwtService).generateRefreshToken(testUser);
        verify(userRepository).save(argThat(user -> user.getLastLogin() != null));
    }

    @Test
    @DisplayName("Should throw exception when user not found")
    void testLogin_UserNotFound() {
        // Given
        when(userRepository.findByUsername(loginRequest.getUsername()))
                .thenReturn(Optional.empty());

        // When & Then
        assertThrows(UnauthorizedException.class, () -> 
            authenticationService.login(loginRequest)
        );
        verify(jwtService, never()).generateAccessToken(any());
    }

    @Test
    @DisplayName("Should throw exception when account is inactive")
    void testLogin_AccountInactive() {
        // Given
        testUser.setActive(false);
        when(userRepository.findByUsername(loginRequest.getUsername()))
                .thenReturn(Optional.of(testUser));

        // When & Then
        assertThrows(UnauthorizedException.class, () -> 
            authenticationService.login(loginRequest)
        );
    }

    @Test
    @DisplayName("Should throw exception when account is locked")
    void testLogin_AccountLocked() {
        // Given
        testUser.setAccountLocked(true);
        when(userRepository.findByUsername(loginRequest.getUsername()))
                .thenReturn(Optional.of(testUser));

        // When & Then
        assertThrows(UnauthorizedException.class, () -> 
            authenticationService.login(loginRequest)
        );
    }

    @Test
    @DisplayName("Should throw exception when email not verified")
    void testLogin_EmailNotVerified() {
        // Given
        testUser.setEmailVerified(false);
        when(userRepository.findByUsername(loginRequest.getUsername()))
                .thenReturn(Optional.of(testUser));

        // When & Then
        assertThrows(UnauthorizedException.class, () -> 
            authenticationService.login(loginRequest)
        );
    }

    @Test
    @DisplayName("Should throw exception when password is wrong")
    void testLogin_WrongPassword() {
        // Given
        when(userRepository.findByUsername(loginRequest.getUsername()))
                .thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(loginRequest.getPassword(), testUser.getPasswordHash()))
                .thenReturn(false);

        // When & Then
        assertThrows(UnauthorizedException.class, () -> 
            authenticationService.login(loginRequest)
        );
        verify(jwtService, never()).generateAccessToken(any());
    }

    @Test
    @DisplayName("Should refresh tokens successfully")
    void testRefresh_Success() {
        // Given
        String refreshToken = "refresh-token";
        when(jwtService.refreshTokens(refreshToken)).thenReturn(authResponse);

        // When
        AuthResponse result = authenticationService.refresh(refreshToken);

        // Then
        assertNotNull(result);
        verify(jwtService).refreshTokens(refreshToken);
    }

    @Test
    @DisplayName("Should logout successfully")
    void testLogout_Success() {
        // Given
        String refreshToken = "refresh-token";
        when(jwtService.isRefreshToken(refreshToken)).thenReturn(true);
        when(jwtService.extractUsername(refreshToken)).thenReturn(testUser.getUsername());
        when(userRepository.findByUsername(testUser.getUsername()))
                .thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        authenticationService.logout(refreshToken);

        // Then
        verify(jwtService).isRefreshToken(refreshToken);
        verify(userRepository).save(argThat(user -> 
            user.getRefreshTokenHash() == null
        ));
    }

    @Test
    @DisplayName("Should throw exception when logout with invalid refresh token")
    void testLogout_InvalidToken() {
        // Given
        String refreshToken = "invalid-token";
        when(jwtService.isRefreshToken(refreshToken)).thenReturn(false);

        // When & Then
        assertThrows(UnauthorizedException.class, () -> 
            authenticationService.logout(refreshToken)
        );
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should handle forgot password for existing user")
    void testForgotPassword_UserExists() {
        // Given
        when(userRepository.findByEmail(testUser.getEmail()))
                .thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        authenticationService.forgotPassword(testUser.getEmail());

        // Then
        verify(userRepository).findByEmail(testUser.getEmail());
        verify(userRepository).save(argThat(user -> 
            user.getResetPasswordToken() != null &&
            user.getResetTokenExpiry() != null
        ));
        verify(emailService).sendPasswordResetEmail(eq(testUser.getEmail()), anyString());
    }

    @Test
    @DisplayName("Should handle forgot password for non-existing user silently")
    void testForgotPassword_UserNotExists() {
        // Given
        when(userRepository.findByEmail("nonexistent@example.com"))
                .thenReturn(Optional.empty());

        // When
        authenticationService.forgotPassword("nonexistent@example.com");

        // Then
        verify(userRepository).findByEmail("nonexistent@example.com");
        verify(userRepository, never()).save(any(User.class));
        verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString());
    }

    @Test
    @DisplayName("Should reset password successfully")
    void testResetPassword_Success() {
        // Given
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("valid-token");
        request.setNewPassword("newPassword123");

        testUser.setResetPasswordToken("valid-token");
        testUser.setResetTokenExpiry(LocalDateTime.now().plusHours(1));

        when(userRepository.findByResetPasswordToken("valid-token"))
                .thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode("newPassword123")).thenReturn("hashed-new-password");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        authenticationService.resetPassword(request);

        // Then
        verify(userRepository).findByResetPasswordToken("valid-token");
        verify(passwordEncoder).encode("newPassword123");
        verify(userRepository).save(argThat(user -> 
            user.getPasswordHash().equals("hashed-new-password") &&
            user.getResetPasswordToken() == null &&
            user.getResetTokenExpiry() == null &&
            user.getRefreshTokenHash() == null
        ));
    }

    @Test
    @DisplayName("Should throw exception when reset token is invalid")
    void testResetPassword_InvalidToken() {
        // Given
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("invalid-token");
        request.setNewPassword("newPassword123");

        when(userRepository.findByResetPasswordToken("invalid-token"))
                .thenReturn(Optional.empty());

        // When & Then
        assertThrows(PasswordResetException.class, () -> 
            authenticationService.resetPassword(request)
        );
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    @DisplayName("Should throw exception when reset token is expired")
    void testResetPassword_ExpiredToken() {
        // Given
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("expired-token");
        request.setNewPassword("newPassword123");

        testUser.setResetPasswordToken("expired-token");
        testUser.setResetTokenExpiry(LocalDateTime.now().minusHours(1));

        when(userRepository.findByResetPasswordToken("expired-token"))
                .thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When & Then
        assertThrows(PasswordResetException.class, () -> 
            authenticationService.resetPassword(request)
        );
        verify(userRepository).save(argThat(user -> 
            user.getResetPasswordToken() == null &&
            user.getResetTokenExpiry() == null
        ));
    }
}

