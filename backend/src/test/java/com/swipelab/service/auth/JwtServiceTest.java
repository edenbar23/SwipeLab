package com.swipelab.service.auth;

import com.swipelab.config.JwtConfig;
import com.swipelab.dto.response.AuthResponse;
import com.swipelab.exception.UnauthorizedException;
import com.swipelab.model.entity.User;
import com.swipelab.model.enums.UserRole;
import com.swipelab.repository.UserRepository;
import com.swipelab.security.JwtTokenProvider;
import com.swipelab.security.enums.TokenType;
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

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * JWT Service Tests
 * 
 * Tests for JWT token generation, validation, and refresh functionality.
 * 
 * What this test should cover:
 * - Generate access tokens successfully
 * - Generate refresh tokens and save hash to user
 * - Validate refresh tokens (valid, invalid, expired, wrong type)
 * - Refresh tokens with rotation
 * - Revoke refresh tokens
 * - Extract username from tokens
 * - Handle edge cases (missing user, token hash mismatch, revoked tokens)
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("JWT Service Tests")
class JwtServiceTest {

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private JwtConfig jwtConfig;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private JwtService jwtService;

    private User testUser;
    private String testAccessToken;
    private String testRefreshToken;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .username("testuser")
                .email("test@example.com")
                .role(UserRole.USER)
                .active(true)
                .accountLocked(false)
                .emailVerified(true)
                .build();

        testAccessToken = "test-access-token";
        testRefreshToken = "test-refresh-token";

        when(jwtConfig.getAccessTokenExpirationMinutes()).thenReturn(15L);
        when(jwtConfig.getRefreshTokenExpirationDays()).thenReturn(7L);
    }

    @Test
    @DisplayName("Should generate access token successfully")
    void testGenerateAccessToken_Success() {
        // Given
        when(tokenProvider.generateToken(
                eq(testUser.getUsername()),
                eq(testUser.getRole().name()),
                eq(TokenType.ACCESS),
                any()
        )).thenReturn(testAccessToken);

        // When
        String result = jwtService.generateAccessToken(testUser);

        // Then
        assertNotNull(result);
        assertEquals(testAccessToken, result);
        verify(tokenProvider).generateToken(
                eq(testUser.getUsername()),
                eq(testUser.getRole().name()),
                eq(TokenType.ACCESS),
                any()
        );
    }

    @Test
    @DisplayName("Should generate refresh token and save hash to user")
    void testGenerateRefreshToken() {
        // Given
        String hashedToken = "hashed-refresh-token";
        when(tokenProvider.generateToken(
                eq(testUser.getUsername()),
                eq(testUser.getRole().name()),
                eq(TokenType.REFRESH),
                any()
        )).thenReturn(testRefreshToken);
        when(passwordEncoder.encode(testRefreshToken)).thenReturn(hashedToken);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        String result = jwtService.generateRefreshToken(testUser);

        // Then
        assertNotNull(result);
        assertEquals(testRefreshToken, result);
        verify(passwordEncoder).encode(testRefreshToken);
        verify(userRepository).save(argThat(user -> 
            user.getRefreshTokenHash() != null
        ));
    }

    @Test
    @DisplayName("Should validate refresh token successfully")
    void testValidateRefreshToken_Success() {
        // Given
        when(tokenProvider.isTokenValid(testRefreshToken)).thenReturn(true);
        when(tokenProvider.extractTokenType(testRefreshToken)).thenReturn(TokenType.REFRESH);
        when(tokenProvider.extractUsername(testRefreshToken)).thenReturn(testUser.getUsername());
        when(userRepository.findByUsername(testUser.getUsername())).thenReturn(Optional.of(testUser));
        testUser.setRefreshTokenHash("hashed-token");
        when(passwordEncoder.matches(testRefreshToken, "hashed-token"))
                .thenReturn(true);

        // When
        User result = jwtService.validateRefreshToken(testRefreshToken);

        // Then
        assertNotNull(result);
        assertEquals(testUser.getUsername(), result.getUsername());
        verify(tokenProvider).isTokenValid(testRefreshToken);
        verify(tokenProvider).extractTokenType(testRefreshToken);
        verify(passwordEncoder).matches(testRefreshToken, testUser.getRefreshTokenHash());
    }

    @Test
    @DisplayName("Should throw exception when refresh token is invalid")
    void testValidateRefreshToken_InvalidToken() {
        // Given
        when(tokenProvider.isTokenValid(testRefreshToken)).thenReturn(false);

        // When & Then
        assertThrows(UnauthorizedException.class, () -> 
            jwtService.validateRefreshToken(testRefreshToken)
        );
        verify(tokenProvider).isTokenValid(testRefreshToken);
        verify(userRepository, never()).findByUsername(anyString());
    }

    @Test
    @DisplayName("Should throw exception when refresh token type is wrong")
    void testValidateRefreshToken_WrongTokenType() {
        // Given
        when(tokenProvider.isTokenValid(testRefreshToken)).thenReturn(true);
        when(tokenProvider.extractTokenType(testRefreshToken)).thenReturn(TokenType.ACCESS);

        // When & Then
        assertThrows(UnauthorizedException.class, () -> 
            jwtService.validateRefreshToken(testRefreshToken)
        );
    }

    @Test
    @DisplayName("Should throw exception when user not found")
    void testValidateRefreshToken_UserNotFound() {
        // Given
        when(tokenProvider.isTokenValid(testRefreshToken)).thenReturn(true);
        when(tokenProvider.extractTokenType(testRefreshToken)).thenReturn(TokenType.REFRESH);
        when(tokenProvider.extractUsername(testRefreshToken)).thenReturn("nonexistent");
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(UnauthorizedException.class, () -> 
            jwtService.validateRefreshToken(testRefreshToken)
        );
    }

    @Test
    @DisplayName("Should throw exception when refresh token hash doesn't match")
    void testValidateRefreshToken_TokenHashMismatch() {
        // Given
        testUser.setRefreshTokenHash("different-hash");
        when(tokenProvider.isTokenValid(testRefreshToken)).thenReturn(true);
        when(tokenProvider.extractTokenType(testRefreshToken)).thenReturn(TokenType.REFRESH);
        when(tokenProvider.extractUsername(testRefreshToken)).thenReturn(testUser.getUsername());
        when(userRepository.findByUsername(testUser.getUsername())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(testRefreshToken, testUser.getRefreshTokenHash()))
                .thenReturn(false);

        // When & Then
        assertThrows(UnauthorizedException.class, () -> 
            jwtService.validateRefreshToken(testRefreshToken)
        );
    }

    @Test
    @DisplayName("Should refresh tokens successfully")
    void testRefreshTokens_Success() {
        // Given
        String newAccessToken = "new-access-token";
        String newRefreshToken = "new-refresh-token";
        String hashedNewRefreshToken = "hashed-new-refresh-token";
        
        testUser.setRefreshTokenHash("hashed-old-token");
        
        when(tokenProvider.isTokenValid(testRefreshToken)).thenReturn(true);
        when(tokenProvider.extractTokenType(testRefreshToken)).thenReturn(TokenType.REFRESH);
        when(tokenProvider.extractUsername(testRefreshToken)).thenReturn(testUser.getUsername());
        when(userRepository.findByUsername(testUser.getUsername())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(testRefreshToken, testUser.getRefreshTokenHash()))
                .thenReturn(true);
        
        when(tokenProvider.generateToken(
                eq(testUser.getUsername()),
                eq(testUser.getRole().name()),
                eq(TokenType.ACCESS),
                any()
        )).thenReturn(newAccessToken);
        
        when(tokenProvider.generateToken(
                eq(testUser.getUsername()),
                eq(testUser.getRole().name()),
                eq(TokenType.REFRESH),
                any()
        )).thenReturn(newRefreshToken);
        
        when(passwordEncoder.encode(newRefreshToken)).thenReturn(hashedNewRefreshToken);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        AuthResponse result = jwtService.refreshTokens(testRefreshToken);

        // Then
        assertNotNull(result);
        assertEquals(newAccessToken, result.getAccessToken());
        assertEquals(newRefreshToken, result.getRefreshToken());
        assertTrue(result.getExpiresIn() > 0);
        // refreshTokens calls generateRefreshToken which saves once
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw exception when refresh token is revoked")
    void testRefreshTokens_TokenRevoked() {
        // Given
        testUser.setRefreshTokenHash(null);
        when(tokenProvider.isTokenValid(testRefreshToken)).thenReturn(true);
        when(tokenProvider.extractTokenType(testRefreshToken)).thenReturn(TokenType.REFRESH);
        when(tokenProvider.extractUsername(testRefreshToken)).thenReturn(testUser.getUsername());
        when(userRepository.findByUsername(testUser.getUsername())).thenReturn(Optional.of(testUser));

        // When & Then
        assertThrows(UnauthorizedException.class, () -> 
            jwtService.refreshTokens(testRefreshToken)
        );
    }

    @Test
    @DisplayName("Should revoke refresh token")
    void testRevokeRefreshToken() {
        // Given
        testUser.setRefreshTokenHash("some-hash");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        jwtService.revokeRefreshToken(testUser);

        // Then
        verify(userRepository).save(argThat(user -> 
            user.getRefreshTokenHash() == null
        ));
    }

    @Test
    @DisplayName("Should check if token is refresh token")
    void testIsRefreshToken() {
        // Given
        when(tokenProvider.isTokenValid(testRefreshToken)).thenReturn(true);
        when(tokenProvider.extractTokenType(testRefreshToken)).thenReturn(TokenType.REFRESH);

        // When
        boolean result = jwtService.isRefreshToken(testRefreshToken);

        // Then
        assertTrue(result);
    }

    @Test
    @DisplayName("Should return false when token is not refresh token")
    void testIsRefreshToken_NotRefreshToken() {
        // Given
        when(tokenProvider.isTokenValid(testAccessToken)).thenReturn(true);
        when(tokenProvider.extractTokenType(testAccessToken)).thenReturn(TokenType.ACCESS);

        // When
        boolean result = jwtService.isRefreshToken(testAccessToken);

        // Then
        assertFalse(result);
    }

    @Test
    @DisplayName("Should extract username from token")
    void testExtractUsername() {
        // Given
        when(tokenProvider.extractUsername(testRefreshToken)).thenReturn(testUser.getUsername());

        // When
        String result = jwtService.extractUsername(testRefreshToken);

        // Then
        assertEquals(testUser.getUsername(), result);
        verify(tokenProvider).extractUsername(testRefreshToken);
    }

    @Test
    @DisplayName("Should return correct access token expiry seconds")
    void testGetAccessTokenExpirySeconds() {
        // Given
        when(jwtConfig.getAccessTokenExpirationMinutes()).thenReturn(30L);

        // When
        long result = jwtService.getAccessTokenExpirySeconds();

        // Then
        assertEquals(1800L, result); // 30 minutes * 60 seconds
    }
}

