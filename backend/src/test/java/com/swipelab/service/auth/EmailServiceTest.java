package com.swipelab.service.auth;

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
    void sendVerificationEmail_ShouldExecuteWithoutException() {
        assertDoesNotThrow(() -> emailService.sendVerificationEmail("test@example.com", "token123"));
    }

    @Test
    void sendPasswordResetEmail_ShouldExecuteWithoutException() {
        assertDoesNotThrow(() -> emailService.sendPasswordResetEmail("test@example.com", "resetToken"));
    }
}
