package com.swipelab.classification.application;

import com.swipelab.classification.domain.FraudDetectionService;
import com.swipelab.eventing.kafka.KafkaEventPublisher;
import com.swipelab.users.events.FraudDetectedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FraudDetectionServiceTest {

    @Mock
    private KafkaEventPublisher eventPublisher;

    @InjectMocks
    private FraudDetectionService fraudDetectionService;

    @Test
    void analyzeClassification_ShouldFlagUser_WhenResponseTimeIsTooLow() {
        fraudDetectionService.analyzeClassification("testuser", 200L); // threshold is 500L
        
        ArgumentCaptor<FraudDetectedEvent> captor = ArgumentCaptor.forClass(FraudDetectedEvent.class);
        verify(eventPublisher, times(1)).publish(eq("fraud-events"), captor.capture());
        
        assertEquals("testuser", captor.getValue().getUsername());
        assertEquals("Non-human response speed: 200ms", captor.getValue().getReason());
    }

    @Test
    void analyzeClassification_ShouldNotFlagUser_WhenResponseTimeIsNormal() {
        fraudDetectionService.analyzeClassification("normaluser", 1500L); // threshold is 500L
        verify(eventPublisher, never()).publish(anyString(), any(FraudDetectedEvent.class));
    }
}
