package com.swipelab.classification.application;

import com.swipelab.classification.domain.Classification;
import com.swipelab.classification.domain.FraudDetectionService;
import com.swipelab.classification.domain.GoldImage;
import com.swipelab.classification.domain.Image;
import com.swipelab.classification.domain.ImageService;
import com.swipelab.classification.dto.UserClassification;
import com.swipelab.classification.dto.api.NextBatchResponse;
import com.swipelab.classification.dto.api.SubmitClassificationRequest;
import com.swipelab.classification.events.ClassificationSubmittedEvent;
import com.swipelab.classification.infrastructure.ClassificationRepository;
import com.swipelab.classification.infrastructure.CredibilityRepository;
import com.swipelab.classification.infrastructure.GoldImageRepository;
import com.swipelab.classification.infrastructure.ImageRepository;
import com.swipelab.exception.ResourceNotFoundException;
import com.swipelab.tasks.domain.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClassificationServiceTest {

    @Mock
    private ClassificationRepository classificationRepository;

    @Mock
    private ImageRepository imageRepository;

    @Mock
    private GoldImageRepository goldImageRepository;

    @Mock
    private CredibilityRepository credibilityRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private FraudDetectionService fraudDetectionService;

    @Mock
    private ImageService imageService;

    @InjectMocks
    private ClassificationService classificationService;

    private SubmitClassificationRequest request;
    private Image image;
    private Task task;

    @BeforeEach
    void setUp() {
        request = new SubmitClassificationRequest();
        request.setImageId(1L);
        request.setTaskId(1L);
        request.setDecision(Classification.UserResponse.YES);
        request.setResponseTimeMs(1500L);

        task = new Task();
        task.setId(1L);
        task.setQuerySpecies("Lion");

        image = new Image();
        image.setId(1L);
        image.setTask(task);
    }

    @Test
    void submitClassification_ShouldThrowException_WhenImageNotFound() {
        when(imageRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                classificationService.submitClassification("testuser", "USER", 0.8, request));
    }

    @Test
    void submitClassification_ShouldSaveCredibility_WhenGoldImageExists() {
        GoldImage goldImage = new GoldImage();
        goldImage.setCorrectAnswer(GoldImage.UserResponse.YES);
        goldImage.setSpecies("Lion");

        when(imageRepository.findById(1L)).thenReturn(Optional.of(image));
        when(goldImageRepository.findByImageId(1L)).thenReturn(Optional.of(goldImage));
        when(imageService.getNextBatchForApi(anyLong(), anyString(), anyInt()))
                .thenReturn(NextBatchResponse.builder().build());

        classificationService.submitClassification("testuser", "USER", 0.8, request);

        verify(fraudDetectionService, times(1)).analyzeClassification("testuser", 1500L);
        verify(credibilityRepository, times(1)).save(any());
        verify(classificationRepository, never()).save(any());

        ArgumentCaptor<ClassificationSubmittedEvent> eventCaptor = ArgumentCaptor.forClass(ClassificationSubmittedEvent.class);
        verify(kafkaTemplate, times(1)).send(eq("classification-events"), eventCaptor.capture());

        ClassificationSubmittedEvent event = eventCaptor.getValue();
        assertTrue(event.isCorrect());
        assertTrue(event.isGoldStandard());
    }

    @Test
    void submitClassification_ShouldSaveClassification_WhenRegularImage() {
        when(imageRepository.findById(1L)).thenReturn(Optional.of(image));
        when(goldImageRepository.findByImageId(1L)).thenReturn(Optional.empty());
        when(imageService.getNextBatchForApi(anyLong(), anyString(), anyInt()))
                .thenReturn(NextBatchResponse.builder().build());

        Classification savedClassification = new Classification();
        savedClassification.setId(10L);
        when(classificationRepository.save(any(Classification.class))).thenReturn(savedClassification);

        classificationService.submitClassification("testuser", "USER", 0.8, request);

        verify(fraudDetectionService, times(1)).analyzeClassification("testuser", 1500L);
        verify(credibilityRepository, never()).save(any());
        verify(classificationRepository, times(1)).save(any());

        ArgumentCaptor<ClassificationSubmittedEvent> eventCaptor = ArgumentCaptor.forClass(ClassificationSubmittedEvent.class);
        verify(kafkaTemplate, times(1)).send(eq("classification-events"), eventCaptor.capture());

        ClassificationSubmittedEvent event = eventCaptor.getValue();
        assertFalse(event.isCorrect());
        assertFalse(event.isGoldStandard());
        assertEquals(10L, event.getClassificationId());
    }

    @Test
    void submitBatchResponses_ShouldProcessCorrectly() {
        UserClassification userClassification = new UserClassification(1L, Classification.UserResponse.YES);
        List<UserClassification> responses = Collections.singletonList(userClassification);

        Classification savedClassification = new Classification();
        savedClassification.setId(10L);

        when(imageRepository.findById(1L)).thenReturn(Optional.of(image));
        when(goldImageRepository.findByImageId(1L)).thenReturn(Optional.empty());
        when(classificationRepository.save(any(Classification.class))).thenReturn(savedClassification);

        classificationService.submitBatchResponses("testuser", "USER", 1L, responses);

        verify(classificationRepository, times(1)).save(any(Classification.class));
        verify(kafkaTemplate, times(1)).send(eq("classification-events"), any(ClassificationSubmittedEvent.class));
    }
}
