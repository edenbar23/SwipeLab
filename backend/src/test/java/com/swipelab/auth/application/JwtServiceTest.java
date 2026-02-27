package com.swipelab.auth.application;

import com.swipelab.auth.infrastructure.JwtConfig;
import com.swipelab.auth.infrastructure.JwtTokenProvider;
import com.swipelab.auth.infrastructure.enums.TokenType;
import com.swipelab.dto.response.AuthResponse;
import com.swipelab.exception.UnauthorizedException;
import com.swipelab.users.domain.User;
import com.swipelab.users.infrastructure.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
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

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setRole(com.swipelab.model.enums.UserRole.USER);
    }

    @Test
    void generateAccessToken_ShouldReturnToken() {
        when(jwtConfig.getAccessTokenExpirationMinutes()).thenReturn(15L);
        when(tokenProvider.generateToken(anyString(), anyString(), eq(TokenType.ACCESS), any(Duration.class)))
                .thenReturn("access-token");

        String token = jwtService.generateAccessToken(testUser);

        assertEquals("access-token", token);
        verify(tokenProvider).generateToken("testuser", "USER", TokenType.ACCESS, Duration.ofMinutes(15));
    }

    @Test
    void generateRefreshToken_ShouldReturnTokenAndSaveHash() {
        when(jwtConfig.getRefreshTokenExpirationDays()).thenReturn(7L);
        when(tokenProvider.generateToken(anyString(), anyString(), eq(TokenType.REFRESH), any(Duration.class)))
                .thenReturn("refresh-token");
        when(passwordEncoder.encode("refresh-token")).thenReturn("hashed-token");

        String token = jwtService.generateRefreshToken(testUser);

        assertEquals("refresh-token", token);
        assertEquals("hashed-token", testUser.getRefreshTokenHash());
        verify(userRepository).save(testUser);
    }

    @Test
    void validateRefreshToken_ShouldThrowException_WhenTokenInvalid() {
        when(tokenProvider.isTokenValid("invalid-token")).thenReturn(false);

        assertThrows(UnauthorizedException.class, () -> jwtService.validateRefreshToken("invalid-token"));
    }

    @Test
    void validateRefreshToken_ShouldThrowException_WhenTokenTypeMismatch() {
        when(tokenProvider.isTokenValid("valid-token")).thenReturn(true);
        when(tokenProvider.extractTokenType("valid-token")).thenReturn(TokenType.ACCESS);

        assertThrows(UnauthorizedException.class, () -> jwtService.validateRefreshToken("valid-token"));
    }

    @Test
    void validateRefreshToken_ShouldThrowException_WhenUserNotFound() {
        when(tokenProvider.isTokenValid("valid-token")).thenReturn(true);
        when(tokenProvider.extractTokenType("valid-token")).thenReturn(TokenType.REFRESH);
        when(tokenProvider.extractUsername("valid-token")).thenReturn("unknown");
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThrows(UnauthorizedException.class, () -> jwtService.validateRefreshToken("valid-token"));
    }

    @Test
    void validateRefreshToken_ShouldThrowException_WhenTokenRevoked() {
        when(tokenProvider.isTokenValid("valid-token")).thenReturn(true);
        when(tokenProvider.extractTokenType("valid-token")).thenReturn(TokenType.REFRESH);
        when(tokenProvider.extractUsername("valid-token")).thenReturn("testuser");
        
        testUser.setRefreshTokenHash("different-hash");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("valid-token", "different-hash")).thenReturn(false);

        assertThrows(UnauthorizedException.class, () -> jwtService.validateRefreshToken("valid-token"));
    }

    @Test
    void validateRefreshToken_ShouldReturnUser_WhenValid() {
        when(tokenProvider.isTokenValid("valid-token")).thenReturn(true);
        when(tokenProvider.extractTokenType("valid-token")).thenReturn(TokenType.REFRESH);
        when(tokenProvider.extractUsername("valid-token")).thenReturn("testuser");
        
        testUser.setRefreshTokenHash("hash");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("valid-token", "hash")).thenReturn(true);

        User result = jwtService.validateRefreshToken("valid-token");

        assertEquals(testUser, result);
    }

    @Test
    void rotateTokens_ShouldReturnNewTokens() {
        // Mock validation
        when(tokenProvider.isTokenValid("old-refresh")).thenReturn(true);
        when(tokenProvider.extractTokenType("old-refresh")).thenReturn(TokenType.REFRESH);
        when(tokenProvider.extractUsername("old-refresh")).thenReturn("testuser");
        
        testUser.setRefreshTokenHash("hash");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("old-refresh", "hash")).thenReturn(true);

        // Mock generation
        when(jwtConfig.getAccessTokenExpirationMinutes()).thenReturn(15L);
        when(jwtConfig.getRefreshTokenExpirationDays()).thenReturn(7L);
        when(tokenProvider.generateToken(anyString(), anyString(), eq(TokenType.ACCESS), any(Duration.class)))
                .thenReturn("new-access");
        when(tokenProvider.generateToken(anyString(), anyString(), eq(TokenType.REFRESH), any(Duration.class)))
                .thenReturn("new-refresh");
        when(passwordEncoder.encode("new-refresh")).thenReturn("new-hash");

        AuthResponse response = jwtService.rotateTokens("old-refresh");

        assertNotNull(response);
        assertEquals("new-access", response.getAccessToken());
        assertEquals("new-refresh", response.getRefreshToken());
        assertEquals(900L, response.getExpiresIn());
    }

    @Test
    void revokeRefreshToken_ShouldClearHash() {
        testUser.setRefreshTokenHash("hash");

        jwtService.revokeRefreshToken(testUser);

        assertNull(testUser.getRefreshTokenHash());
        verify(userRepository).save(testUser);
    }
    
    @Test
    void refreshTokens_ShouldThrowException_WhenTokenInvalid() {
        when(tokenProvider.isTokenValid("invalid")).thenReturn(false);
        assertThrows(UnauthorizedException.class, () -> jwtService.refreshTokens("invalid"));
    }
    
    @Test
    void isRefreshToken_ShouldReturnTrue_WhenValidAndTypeMatch() {
        when(tokenProvider.isTokenValid("token")).thenReturn(true);
        when(tokenProvider.extractTokenType("token")).thenReturn(TokenType.REFRESH);
        
        assertTrue(jwtService.isRefreshToken("token"));
    }
}
