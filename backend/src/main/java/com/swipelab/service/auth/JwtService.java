package com.swipelab.service.auth;

import com.swipelab.config.JwtConfig;
import com.swipelab.dto.response.AuthResponse;
import com.swipelab.exception.UnauthorizedException;
import com.swipelab.model.entity.User;
import com.swipelab.repository.UserRepository;
import com.swipelab.security.JwtTokenProvider;
import com.swipelab.security.enums.TokenType;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtTokenProvider tokenProvider;
    private final JwtConfig jwtConfig;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public String generateAccessToken(User user) {
        return tokenProvider.generateToken(
                user.getUsername(),
                user.getRole().name(),
                TokenType.ACCESS,
                Duration.ofMinutes(jwtConfig.getAccessTokenExpirationMinutes())
        );
    }

    public String generateRefreshToken(User user) {
        String refreshToken = tokenProvider.generateToken(
                user.getUsername(),
                user.getRole().name(),
                TokenType.REFRESH,
                Duration.ofDays(jwtConfig.getRefreshTokenExpirationDays())
        );

        user.setRefreshTokenHash(passwordEncoder.encode(refreshToken));
        userRepository.save(user);

        return refreshToken;
    }

    public User validateRefreshToken(String token) {
        if (!tokenProvider.isTokenValid(token)
                || tokenProvider.extractTokenType(token) != TokenType.REFRESH) {
            throw new UnauthorizedException("Invalid refresh token");
        }

        String username = tokenProvider.extractUsername(token);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        if (!passwordEncoder.matches(token, user.getRefreshTokenHash())) {
            throw new UnauthorizedException("Refresh token revoked");
        }

        return user;
    }


    public AuthResponse rotateTokens(String refreshToken) {
        User user = validateRefreshToken(refreshToken);

        String newAccessToken = generateAccessToken(user);
        String newRefreshToken = generateRefreshToken(user); // rotation

        return new AuthResponse(newAccessToken, newRefreshToken);
    }

    public void revokeRefreshToken(User user) {
        user.setRefreshTokenHash(null);
        userRepository.save(user);
    }
}

