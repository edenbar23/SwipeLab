package com.swipelab.security;

import com.swipelab.config.JwtConfig;
import com.swipelab.security.enums.TokenType;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final JwtConfig jwtConfig;

    /**
     * Generates a signed JWT token.
     */
    public String generateToken(
            String username,
            String role,
            TokenType tokenType,
            Duration expiration
    ) {
        Instant now = Instant.now();

        return Jwts.builder()
                .subject(username)
                .claim("role", role)
                .claim("type", tokenType.name())
                .issuer(jwtConfig.getIssuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expiration)))
                .signWith(jwtConfig.getSigningKey())
                .compact();
    }

    /**
     * Parses and validates JWT claims.
     * Throws JwtException if invalid or expired.
     */
    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(jwtConfig.getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Validates token signature and expiration.
     */
    public boolean isTokenValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    /**
     * Extracts username (JWT subject).
     */
    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    /**
     * Extracts user role.
     */
    public String extractRole(String token) {
        return parseClaims(token).get("role", String.class);
    }

    /**
     * Extracts token type (ACCESS / REFRESH).
     */
    public TokenType extractTokenType(String token) {
        String type = parseClaims(token).get("type", String.class);
        return TokenType.valueOf(type);
    }
}
