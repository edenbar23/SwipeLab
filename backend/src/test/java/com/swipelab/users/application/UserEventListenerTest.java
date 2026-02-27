package com.swipelab.users.application;

import com.swipelab.eventing.kafka.KafkaConfig;
import com.swipelab.eventing.kafka.KafkaEventPublisher;
import com.swipelab.users.domain.User;
import com.swipelab.users.events.FraudDetectedEvent;
import com.swipelab.users.events.UserStatusChangedEvent;
import com.swipelab.users.infrastructure.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserEventListenerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private KafkaEventPublisher eventPublisher;

    @InjectMocks
    private UserEventListener userEventListener;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setUsername("testuser");
        user.setCredibilityScore(50.0);
        user.setAccountLocked(false);
    }

    @Test
    void onFraudDetected_ShouldLockAccountAndPenalizeScore() {
        FraudDetectedEvent event = new FraudDetectedEvent("testuser", "reason", java.time.LocalDateTime.now());

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        userEventListener.onFraudDetected(event);

        verify(userRepository, times(1)).save(argThat(u -> 
                u.getAccountLocked() && u.getCredibilityScore() == 40.0
        ));

        verify(eventPublisher, times(1)).publish(eq(KafkaConfig.USER_EVENTS_TOPIC), any(UserStatusChangedEvent.class));
    }
    
    @Test
    void onFraudDetected_ScoreShouldNotGoBelowZero() {
        user.setCredibilityScore(5.0);
        FraudDetectedEvent event = new FraudDetectedEvent("testuser", "reason", java.time.LocalDateTime.now());

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        userEventListener.onFraudDetected(event);

        verify(userRepository, times(1)).save(argThat(u -> 
                u.getAccountLocked() && u.getCredibilityScore() == 0.0
        ));
    }

    @Test
    void onFraudDetected_ShouldThrowException_WhenUserNotFound() {
        FraudDetectedEvent event = new FraudDetectedEvent("notfound", "reason", java.time.LocalDateTime.now());

        when(userRepository.findByUsername("notfound")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> userEventListener.onFraudDetected(event));
    }
}
