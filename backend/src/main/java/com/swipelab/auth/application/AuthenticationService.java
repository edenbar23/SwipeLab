package com.swipelab.auth.application;

import com.swipelab.auth.dto.RegisterRequest;
import com.swipelab.auth.dto.ResetPasswordRequest;
import com.swipelab.auth.dto.AuthResponse;
import com.swipelab.exception.EmailVerificationException;
import com.swipelab.exception.PasswordResetException;
import com.swipelab.users.domain.User;
import com.swipelab.users.infrastructure.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.swipelab.auth.dto.LoginRequest;
import com.swipelab.exception.UnauthorizedException;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final com.swipelab.auth.domain.AuthMapper authMapper;
    private final JwtService jwtService;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

    @Value("${app.auto-verify-emails:false}")
    private boolean autoVerifyEmails;

    @Transactional
    public java.util.Map<String, Object> register(RegisterRequest request) {
        request.setUsername(request.getUsername().toLowerCase().trim());

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

        // Create new user using Mapper
        User user = authMapper.toUser(request);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setEmailVerificationToken(verificationToken);
        user.setVerificationTokenExpiry(LocalDateTime.now().plusHours(24)); // 24-hour expiry



        // Auto-verify emails in development environment
        if (autoVerifyEmails) {
            user.setEmailVerified(true);
            user.setStatus(com.swipelab.model.enums.UserStatus.ACTIVE);
            log.info("✅ Auto-verified email for user: {} (development mode)", user.getEmail());
        } else {
            user.setEmailVerified(false);
            user.setStatus(com.swipelab.model.enums.UserStatus.PENDING_VERIFICATION);
        }

        // Save user
        User savedUser = userRepository.save(user);

        // Publish UserCreatedEvent
        eventPublisher.publishEvent(
                com.swipelab.users.events.UserCreatedEvent.builder()
                        .username(savedUser.getUsername())
                        .email(savedUser.getEmail())
                        .displayName(savedUser.getDisplayName())
                        .build());

        // Send verification email asynchronously (only if not auto-verified)
        if (!autoVerifyEmails) {
            emailService.sendVerificationEmail(savedUser.getEmail(), verificationToken);
        }

        // Generate tokens only if auto-verified
        if (autoVerifyEmails) {
            String accessToken = jwtService.generateAccessToken(savedUser);
            String refreshToken = jwtService.generateRefreshToken(savedUser);
            
            java.util.Map<String, Object> response = new java.util.HashMap<>();
            response.put("message", "Registration successful. Auto-verified.");
            response.put("accessToken", accessToken);
            response.put("refreshToken", refreshToken);
            // using authMapper to just get the DTO for the user if needed, or we can just return the raw object 
            // because spring will serialize it, but it's safer to use the DTO
            response.put("user", authMapper.toAuthResponse(accessToken, refreshToken, savedUser).getUser());
            return response;
        } else {
            return java.util.Map.of("message", "Registration successful! A verification link has been sent to your email.");
        }
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
        user.setStatus(com.swipelab.model.enums.UserStatus.ACTIVE);

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
        String normalizedUsername = request.getUsername().toLowerCase().trim();
        User user = userRepository.findByUsername(normalizedUsername)
                .orElseThrow(() -> new UnauthorizedException("Invalid username or password"));

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
            throw new UnauthorizedException("Invalid username or password");
        }

        // Generate tokens — set lastLogin first so generateRefreshToken saves both in one UPDATE
        String accessToken = jwtService.generateAccessToken(user);
        user.setLastLogin(LocalDateTime.now());
        String refreshToken = jwtService.generateRefreshToken(user); // saves user with refreshTokenHash + lastLogin

        return authMapper.toAuthResponse(accessToken, refreshToken, user);
    }

    @Transactional
    public AuthResponse refresh(String refreshToken) {
        return jwtService.refreshTokens(refreshToken);
    }

    @Transactional
    public void logout(String refreshToken) {

        if (!jwtService.isRefreshToken(refreshToken)) {
            log.warn("logout: token is invalid or expired. Assuming already logged out.");
            return;
        }

        // Username is already validated inside isRefreshToken — no extra SELECT needed.
        String username = jwtService.extractUsername(refreshToken);

        // Single UPDATE — skips the redundant SELECT that caused the double-query in logs.
        int updated = userRepository.revokeRefreshToken(username);
        if (updated == 0) {
            log.warn("logout: no refresh token found to revoke for user '{}'", username);
        }
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

    /**
     * Resets user password using a valid reset token
     *
     * @param request Contains reset token and new password
     * @throws PasswordResetException if token is invalid, expired, or already used
     */
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        // Find user by reset token
        User user = userRepository.findByResetPasswordToken(request.getToken())
                .orElseThrow(() -> new PasswordResetException("Invalid or expired reset token"));

        // Validate token expiry
        if (user.getResetTokenExpiry() == null ||
                LocalDateTime.now().isAfter(user.getResetTokenExpiry())) {
            // Invalidate expired token
            user.setResetPasswordToken(null);
            user.setResetTokenExpiry(null);
            userRepository.save(user);
            throw new PasswordResetException("Reset token has expired");
        }

        // Hash the new password
        String hashedPassword = passwordEncoder.encode(request.getNewPassword());
        user.setPasswordHash(hashedPassword);

        // Invalidate the reset token (one-time use)
        user.setResetPasswordToken(null);
        user.setResetTokenExpiry(null);

        // Optionally: Invalidate all refresh tokens for security
        user.setRefreshTokenHash(null);

        // Save updated user
        // Save updated user
        userRepository.save(user);
    }
    
        /**
     * Invite a new admin user
     * generates an invitation token and sends an email.
     */
    @Transactional
    public void inviteAdmin(com.swipelab.auth.dto.InviteAdminRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("User already exists with this email");
        }

        String invitationToken = UUID.randomUUID().toString();

        User user = new User();
        user.setUsername(request.getEmail()); // Using email as username for invited admins initially
        user.setEmail(request.getEmail());
        user.setRole(request.getRole());
        user.setEmailVerified(false);
        user.setStatus(com.swipelab.model.enums.UserStatus.PENDING_INVITE);
        user.setEmailVerificationToken(invitationToken); 
        user.setVerificationTokenExpiry(LocalDateTime.now().plusDays(7));
        user.setProvider(com.swipelab.auth.infrastructure.AuthProvider.LOCAL);
        user.setActive(true);

        userRepository.save(user);

        emailService.sendInvitationEmail(user.getEmail(), request.getRole().name(), invitationToken);
    }

}













