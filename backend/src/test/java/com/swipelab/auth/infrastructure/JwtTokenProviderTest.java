package com.swipelab.auth.infrastructure;

import com.swipelab.auth.infrastructure.enums.TokenType;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtTokenProviderTest {

    @Mock
    private JwtConfig jwtConfig;

    @InjectMocks
    private JwtTokenProvider jwtTokenProvider;

    private SecretKey secretKey;

    @BeforeEach
    void setUp() {
        // Create a 256-bit key for HMAC-SHA256
        String secret = "my-super-secret-key-that-is-at-least-256-bits-long!";
        secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void generateToken_ShouldReturnValidJwt() {
        when(jwtConfig.getIssuer()).thenReturn("swipelab");
        when(jwtConfig.getSigningKey()).thenReturn(secretKey);

        String token = jwtTokenProvider.generateToken("testuser", "USER", TokenType.ACCESS, Duration.ofMinutes(15));

        assertNotNull(token);
        assertTrue(token.split("\\.").length == 3);
        
        Claims claims = Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload();
        assertEquals("testuser", claims.getSubject());
        assertEquals("swipelab", claims.getIssuer());
        assertEquals("USER", claims.get("role"));
        assertEquals("ACCESS", claims.get("type"));
    }

    @Test
    void isTokenValid_ShouldReturnTrue_WhenTokenIsValid() {
        when(jwtConfig.getIssuer()).thenReturn("swipelab");
        when(jwtConfig.getSigningKey()).thenReturn(secretKey);

        String token = jwtTokenProvider.generateToken("testuser", "USER", TokenType.ACCESS, Duration.ofMinutes(15));

        assertTrue(jwtTokenProvider.isTokenValid(token));
    }

    @Test
    void isTokenValid_ShouldReturnFalse_WhenTokenIsInvalid() {
        when(jwtConfig.getSigningKey()).thenReturn(secretKey);
        
        // Invalid token format
        assertFalse(jwtTokenProvider.isTokenValid("invalid.token.format"));
    }

    @Test
    void extractUsername_ShouldReturnSubject() {
        when(jwtConfig.getIssuer()).thenReturn("swipelab");
        when(jwtConfig.getSigningKey()).thenReturn(secretKey);

        String token = jwtTokenProvider.generateToken("testuser", "USER", TokenType.ACCESS, Duration.ofMinutes(15));

        assertEquals("testuser", jwtTokenProvider.extractUsername(token));
    }

    @Test
    void extractRole_ShouldReturnRoleClaim() {
        when(jwtConfig.getIssuer()).thenReturn("swipelab");
        when(jwtConfig.getSigningKey()).thenReturn(secretKey);

        String token = jwtTokenProvider.generateToken("testuser", "USER", TokenType.ACCESS, Duration.ofMinutes(15));

        assertEquals("USER", jwtTokenProvider.extractRole(token));
    }

    @Test
    void extractTokenType_ShouldReturnTokenTypeEnum() {
        when(jwtConfig.getIssuer()).thenReturn("swipelab");
        when(jwtConfig.getSigningKey()).thenReturn(secretKey);

        String token = jwtTokenProvider.generateToken("testuser", "USER", TokenType.ACCESS, Duration.ofMinutes(15));

        assertEquals(TokenType.ACCESS, jwtTokenProvider.extractTokenType(token));
    }
}
