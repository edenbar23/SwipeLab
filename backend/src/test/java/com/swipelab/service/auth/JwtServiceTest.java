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
        testUser = User.builder()
                .username("testuser")
                .role(UserRole.USER)
                .build();
    }

    @Test
    void generateAccessToken_ShouldReturnToken() {
        when(jwtConfig.getAccessTokenExpirationMinutes()).thenReturn(15L);
        when(tokenProvider.generateToken(anyString(), anyString(), any(TokenType.class), any(Duration.class)))
                .thenReturn("access-token");

        String token = jwtService.generateAccessToken(testUser);

        assertEquals("access-token", token);
        verify(tokenProvider).generateToken(eq("testuser"), eq("USER"), eq(TokenType.ACCESS), any(Duration.class));
    }

    @Test
    void generateRefreshToken_ShouldReturnTokenAndSaveHash() {
        when(jwtConfig.getRefreshTokenExpirationDays()).thenReturn(7L);
        when(tokenProvider.generateToken(anyString(), anyString(), any(TokenType.class), any(Duration.class)))
                .thenReturn("refresh-token");
        when(passwordEncoder.encode("refresh-token")).thenReturn("encoded-hash");

        String token = jwtService.generateRefreshToken(testUser);

        assertEquals("refresh-token", token);
        assertEquals("encoded-hash", testUser.getRefreshTokenHash());
        verify(userRepository).save(testUser);
    }

    @Test
    void validateRefreshToken_ShouldReturnUser_WhenTokenIsValid() {
        String token = "valid-refresh-token";
        testUser.setRefreshTokenHash("encoded-hash");

        when(tokenProvider.isTokenValid(token)).thenReturn(true);
        when(tokenProvider.extractTokenType(token)).thenReturn(TokenType.REFRESH);
        when(tokenProvider.extractUsername(token)).thenReturn("testuser");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(token, "encoded-hash")).thenReturn(true);

        User result = jwtService.validateRefreshToken(token);

        assertEquals(testUser, result);
    }

    @Test
    void validateRefreshToken_ShouldThrow_WhenTokenInvalid() {
        String token = "invalid-token";
        when(tokenProvider.isTokenValid(token)).thenReturn(false);

        assertThrows(UnauthorizedException.class, () -> jwtService.validateRefreshToken(token));
    }

    @Test
    void validateRefreshToken_ShouldThrow_WhenTokenTypeNotRefresh() {
        String token = "access-token";
        when(tokenProvider.isTokenValid(token)).thenReturn(true);
        when(tokenProvider.extractTokenType(token)).thenReturn(TokenType.ACCESS);

        assertThrows(UnauthorizedException.class, () -> jwtService.validateRefreshToken(token));
    }

    @Test
    void refreshTokens_ShouldRotateTokens() {
        String oldToken = "old-refresh-token";
        testUser.setRefreshTokenHash("encoded-hash");

        // Mock validation chain
        when(tokenProvider.isTokenValid(oldToken)).thenReturn(true);
        when(tokenProvider.extractTokenType(oldToken)).thenReturn(TokenType.REFRESH);
        when(tokenProvider.extractUsername(oldToken)).thenReturn("testuser");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(oldToken, "encoded-hash")).thenReturn(true);

        // Mock generation
        when(jwtConfig.getAccessTokenExpirationMinutes()).thenReturn(15L);
        when(jwtConfig.getRefreshTokenExpirationDays()).thenReturn(7L);
        when(tokenProvider.generateToken(anyString(), anyString(), eq(TokenType.ACCESS), any(Duration.class)))
                .thenReturn("new-access-token");
        when(tokenProvider.generateToken(anyString(), anyString(), eq(TokenType.REFRESH), any(Duration.class)))
                .thenReturn("new-refresh-token");

        AuthResponse response = jwtService.refreshTokens(oldToken);

        assertEquals("new-access-token", response.getAccessToken());
        assertEquals("new-refresh-token", response.getRefreshToken());
        verify(userRepository, times(1)).save(testUser); // Saved once during generateRefreshToken call inside
                                                         // refreshTokens
    }

    @Test
    void revokeRefreshToken_ShouldClearHashAndSave() {
        testUser.setRefreshTokenHash("some-hash");

        jwtService.revokeRefreshToken(testUser);

        assertNull(testUser.getRefreshTokenHash());
        verify(userRepository).save(testUser);
    }
}
