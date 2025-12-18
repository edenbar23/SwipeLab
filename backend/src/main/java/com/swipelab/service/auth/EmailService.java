package com.swipelab.service.auth;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailService {

    @Async
    public void sendVerificationEmail(String email, String token) {
        // TODO: Implement actual email sending (e.g., using SendGrid, AWS SES, JavaMailSender)
        // For now, just log the verification link
        String verificationLink = "http://localhost:8080/auth/verify-email?token=" + token;

        log.info("==============================================");
        log.info("VERIFICATION EMAIL");
        log.info("To: {}", email);
        log.info("Verification Link: {}", verificationLink);
        log.info("==============================================");

        // Simulate email sending delay
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}