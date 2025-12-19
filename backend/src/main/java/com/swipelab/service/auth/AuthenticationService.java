package com.swipelab.service.auth;

import com.swipelab.dto.request.RegisterRequest;
import com.swipelab.dto.response.AuthResponse;
import com.swipelab.exception.EmailVerificationException;
import com.swipelab.model.entity.User;
import com.swipelab.model.enums.AuthProvider;
import com.swipelab.model.enums.UserRole;
import com.swipelab.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.swipelab.dto.request.LoginRequest;
import com.swipelab.exception.UnauthorizedException;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final JwtService jwtService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered");
        }

        // Check if username already exists
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new RuntimeException("Username already taken");
        }

        // Generate email verification token
        String verificationToken = UUID.randomUUID().toString();

        // Create new user
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setDisplayName(request.getDisplayName());
        user.setProvider(AuthProvider.LOCAL);
        user.setRole(UserRole.USER);
        user.setEmailVerified(false);
        user.setEmailVerificationToken(verificationToken);
        user.setVerificationTokenExpiry(LocalDateTime.now().plusHours(24)); // 24-hour expiry
        user.setActive(true);
        user.setAccountLocked(false);

        // Save user
        User savedUser = userRepository.save(user);

        // Send verification email asynchronously
        emailService.sendVerificationEmail(savedUser.getEmail(), verificationToken);

        // Generate tokens
        String accessToken = jwtService.generateAccessToken(savedUser);
        String refreshToken = jwtService.generateRefreshToken(savedUser);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(2592000) // 30 days in seconds
                .message("Registration successful. Please check your email to verify your account.")
                .build();
    }

    /**
     * Verifies user's email using the verification token
     *
     * @param token The verification token sent via email
     * @throws EmailVerificationException if token is invalid or expired
     */
    @Transactional
    public void verifyEmail(String token) {
        // Find user by verification token
        User user = userRepository.findByEmailVerificationToken(token)
                .orElseThrow(() -> new EmailVerificationException("Invalid verification token"));

        // Check if token has expired
        if (user.getVerificationTokenExpiry() == null ||
                LocalDateTime.now().isAfter(user.getVerificationTokenExpiry())) {
            throw new EmailVerificationException("Verification token has expired");
        }

        // Check if email is already verified
        if (user.getEmailVerified()) {
            throw new EmailVerificationException("Email is already verified");
        }

        // Mark email as verified
        user.setEmailVerified(true);

        // Invalidate the token
        user.setEmailVerificationToken(null);
        user.setVerificationTokenExpiry(null);

        // Save updated user
        userRepository.save(user);
    }

    /**
     * Resends verification email if the previous token expired
     *
     * @param email User's email address
     * @throws EmailVerificationException if email is already verified or not found
     */
    @Transactional
    public void resendVerificationEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new EmailVerificationException("User not found with email: " + email));

        if (user.getEmailVerified()) {
            throw new EmailVerificationException("Email is already verified");
        }

        // Generate new verification token
        String verificationToken = UUID.randomUUID().toString();
        user.setEmailVerificationToken(verificationToken);
        user.setVerificationTokenExpiry(LocalDateTime.now().plusHours(24));

        userRepository.save(user);

        // Send new verification email
        emailService.sendVerificationEmail(user.getEmail(), verificationToken);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new UnauthorizedException("User not exists"));

        // Account state checks
        if (!Boolean.TRUE.equals(user.getActive()) ||
                Boolean.TRUE.equals(user.getAccountLocked())) {
            throw new UnauthorizedException("Account disabled or locked");
        }

        // Email verification check
        if (!Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new UnauthorizedException("Email not verified");
        }

        // Password validation
        if (user.getPasswordHash() == null ||
                !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Wrong password");
        }

        // Generate tokens
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtService.getAccessTokenExpirySeconds())
                .message("Login successful")
                .build();
    }

    @Transactional
    public AuthResponse refresh(String refreshToken) {
        return jwtService.refreshTokens(refreshToken);
    }

    @Transactional
    public void logout(String refreshToken) {

        if (!jwtService.isRefreshToken(refreshToken)) {
            throw new UnauthorizedException("Invalid refresh token");
        }

        String username = jwtService.extractUsername(refreshToken);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        // Invalidate refresh token
        user.setRefreshTokenHash(null);
        userRepository.save(user);
    }

    /**
     * Handles forgot password request
     * Generates reset token and sends email for existing users
     * Returns success for non-existing emails (security best practice)
     *
     * @param email User's email address
     */
    @Transactional
    public void forgotPassword(String email) {
        // Look up user by email
        User user = userRepository.findByEmail(email).orElse(null);

        // If user exists, generate token and send email
        if (user != null) {
            // Generate password reset token
            String resetToken = UUID.randomUUID().toString();

            // Set token and expiry (1 hour)
            user.setResetPasswordToken(resetToken);
            user.setResetTokenExpiry(LocalDateTime.now().plusHours(1));

            // Save user with reset token
            userRepository.save(user);

            // Send password reset email asynchronously
            emailService.sendPasswordResetEmail(user.getEmail(), resetToken);
        }

        // Always return success, regardless of whether email exists
        // This prevents email enumeration attacks
    }


}