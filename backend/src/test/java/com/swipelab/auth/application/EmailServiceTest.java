package com.swipelab.auth.application;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @InjectMocks
    private EmailService emailService;

    @Test
    void sendVerificationEmail_ShouldNotThrowException() {
        // As the current implementation just logs and sleeps, we just verify it doesn't throw anything
        assertDoesNotThrow(() -> emailService.sendVerificationEmail("test@swipelab.com", "token-123"));
    }

    @Test
    void sendPasswordResetEmail_ShouldNotThrowException() {
        assertDoesNotThrow(() -> emailService.sendPasswordResetEmail("test@swipelab.com", "token-123"));
    }
}
