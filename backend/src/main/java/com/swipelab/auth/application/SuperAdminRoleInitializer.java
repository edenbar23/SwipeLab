package com.swipelab.auth.application;

import com.swipelab.model.enums.UserRole;
import com.swipelab.users.domain.User;
import com.swipelab.auth.infrastructure.AuthProvider;
import com.swipelab.users.infrastructure.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

/**
 * Ensures that the designated Super Admin (defined in .env) has the RESEARCHER role in the database.
 * This runs automatically on application startup, which is useful for Docker deployments.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SuperAdminRoleInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.security.super-admin.username}")
    private String superAdminUsername;

    @Value("${app.security.super-admin.password}")
    private String superAdminPassword;

    @Override
    @Transactional
    public void run(String... args) {
        if (superAdminUsername == null || superAdminUsername.isEmpty()) {
            return;
        }

        String normalizedUsername = superAdminUsername.toLowerCase().trim();
        log.info("Checking super-admin role initialization for: {}", normalizedUsername);

        userRepository.findByUsername(normalizedUsername).ifPresentOrElse(user -> {
            boolean updated = false;
            if (user.getRole() != UserRole.RESEARCHER) {
                user.setRole(UserRole.RESEARCHER);
                updated = true;
            }
            if (superAdminPassword != null && !superAdminPassword.isEmpty()) {
                user.setPasswordHash(passwordEncoder.encode(superAdminPassword));
                updated = true;
            }

            if (updated) {
                userRepository.save(user);
                log.info("✅ Successfully updated role to RESEARCHER and/or reset password for super-admin: {}", normalizedUsername);
            } else {
                log.info("✅ Super-admin '{}' already has the RESEARCHER role.", normalizedUsername);
            }
        }, () -> {
            log.info("ℹ️ Super-admin '{}' not found in database. Creating now with credentials from .env...", normalizedUsername);
            
            User admin = new User();
            admin.setUsername(normalizedUsername);
            admin.setEmail(normalizedUsername); // Assuming email is the username
            admin.setDisplayName("Super Admin");
            admin.setRole(UserRole.RESEARCHER);
            admin.setProvider(AuthProvider.LOCAL);
            admin.setActive(true);
            admin.setAccountLocked(false);
            admin.setEmailVerified(true);
            admin.setCredibilityScore(0.0);
            admin.setScore(0L);
            admin.setRank("UNRANKED");
            admin.setCreatedAt(LocalDateTime.now());
            
            if (superAdminPassword != null && !superAdminPassword.isEmpty()) {
                admin.setPasswordHash(passwordEncoder.encode(superAdminPassword));
            } else {
                admin.setPasswordHash(passwordEncoder.encode("superpassword123")); // fallback
            }

            userRepository.save(admin);
            log.info("✅ Super-admin '{}' created successfully.", normalizedUsername);
        });
    }
}
