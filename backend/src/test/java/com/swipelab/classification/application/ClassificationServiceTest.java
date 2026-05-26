package com.swipelab.classification.application;

import com.swipelab.classification.domain.Classification;
import com.swipelab.classification.domain.FraudDetectionService;
import com.swipelab.classification.domain.Image;
import com.swipelab.classification.domain.ImageService;
import com.swipelab.classification.dto.UserClassification;
import com.swipelab.classification.dto.api.NextBatchResponse;
import com.swipelab.classification.dto.api.SubmitClassificationRequest;
import com.swipelab.classification.events.ClassificationSubmittedEvent;
import com.swipelab.classification.domain.FraudAnalysisResult;
import com.swipelab.classification.infrastructure.ClassificationRepository;
import com.swipelab.classification.infrastructure.ImageRepository;
import com.swipelab.exception.ResourceNotFoundException;
import com.swipelab.classification.application.port.out.TaskProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

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
    private TaskProvider taskProvider;

    @Mock
    private GoldImageEvaluatorService goldImageEvaluatorService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private FraudDetectionService fraudDetectionService;

    @Mock
    private ImageService imageService;

    @InjectMocks
    private ClassificationService classificationService;

    private SubmitClassificationRequest request;
    private Image image;
    private TaskProvider.TaskInfo taskInfo;

    @BeforeEach
    void setUp() {
        request = new SubmitClassificationRequest();
        request.setImageId(1L);
        request.setTaskId(1L);
        request.setDecision(Classification.UserResponse.YES);
        request.setResponseTimeMs(1500L);

        taskInfo = new TaskProvider.TaskInfo(1L, "Question", "Lion", Collections.emptyList());

        image = new Image();
        image.setId(1L);
        image.setTaskId(1L);
    }

    @Test
    void submitClassification_ShouldThrowException_WhenImageNotFound() {
        when(fraudDetectionService.analyzeClassification(anyString(), anyString(), anyLong(), anyLong()))
                .thenReturn(new FraudAnalysisResult(false, null));
        when(imageRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                classificationService.submitClassification("testuser", "USER", 0.8, request));
    }

    @Test
    void submitClassification_ShouldSaveCredibility_WhenGoldImageExists() {
        GoldImageEvaluationResult goldResult = new GoldImageEvaluationResult(true, "Lion");

        when(imageRepository.findById(1L)).thenReturn(Optional.of(image));
        when(goldImageEvaluatorService.evaluate(any(), any(), any(), any()))
                .thenReturn(Optional.of(goldResult));
        when(imageService.getNextBatchForApi(anyLong(), anyString(), anyInt()))
                .thenReturn(NextBatchResponse.builder().build());
        when(fraudDetectionService.analyzeClassification(anyString(), anyString(), anyLong(), anyLong()))
                .thenReturn(new FraudAnalysisResult(false, null));

        classificationService.submitClassification("testuser", "USER", 0.8, request);

        verify(fraudDetectionService, times(1)).analyzeClassification("testuser", "USER", 1500L, 1L);
        verify(goldImageEvaluatorService, times(1)).evaluate(any(), any(), any(), any());
        verify(classificationRepository, never()).save(any());

        ArgumentCaptor<ClassificationSubmittedEvent> eventCaptor = ArgumentCaptor.forClass(ClassificationSubmittedEvent.class);
        verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());

        ClassificationSubmittedEvent event = eventCaptor.getValue();
        assertTrue(event.isCorrect());
        assertTrue(event.isGoldStandard());
    }

    @Test
    void submitClassification_ShouldSaveClassification_WhenRegularImage() {
        when(imageRepository.findById(1L)).thenReturn(Optional.of(image));
        when(goldImageEvaluatorService.evaluate(any(), any(), any(), any())).thenReturn(Optional.empty());
        when(imageService.getNextBatchForApi(anyLong(), anyString(), anyInt()))
                .thenReturn(NextBatchResponse.builder().build());
        when(taskProvider.getTaskInfo(1L)).thenReturn(taskInfo);
        when(fraudDetectionService.analyzeClassification(anyString(), anyString(), anyLong(), anyLong()))
                .thenReturn(new FraudAnalysisResult(false, null));

        Classification savedClassification = new Classification();
        savedClassification.setId(10L);
        when(classificationRepository.save(any(Classification.class))).thenReturn(savedClassification);

        classificationService.submitClassification("testuser", "USER", 0.8, request);

        verify(fraudDetectionService, times(1)).analyzeClassification("testuser", "USER", 1500L, 1L);
        verify(goldImageEvaluatorService, times(1)).evaluate(any(), any(), any(), any());
        verify(classificationRepository, times(1)).save(any());

        ArgumentCaptor<ClassificationSubmittedEvent> eventCaptor = ArgumentCaptor.forClass(ClassificationSubmittedEvent.class);
        verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());

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
        when(goldImageEvaluatorService.evaluate(any(), any(), any(), any())).thenReturn(Optional.empty());
        when(taskProvider.getTaskInfo(1L)).thenReturn(taskInfo);
        when(classificationRepository.save(any(Classification.class))).thenReturn(savedClassification);

        classificationService.submitBatchResponses("testuser", "USER", 1L, responses);

        verify(classificationRepository, times(1)).save(any(Classification.class));
        verify(eventPublisher, times(1)).publishEvent(any(ClassificationSubmittedEvent.class));
    }

    @Test
    void submitClassification_ShouldFallbackToTargetSpeciesNames_WhenQuerySpeciesIsEmpty() {
        TaskProvider.TaskInfo emptyQueryTaskInfo = new TaskProvider.TaskInfo(1L, "Question", "", List.of("Tiger", "Bear"));
        
        when(imageRepository.findById(1L)).thenReturn(Optional.of(image));
        when(goldImageEvaluatorService.evaluate(any(), any(), any(), any())).thenReturn(Optional.empty());
        when(imageService.getNextBatchForApi(anyLong(), anyString(), anyInt()))
                .thenReturn(NextBatchResponse.builder().build());
        when(taskProvider.getTaskInfo(1L)).thenReturn(emptyQueryTaskInfo);
        when(fraudDetectionService.analyzeClassification(anyString(), anyString(), anyLong(), anyLong()))
                .thenReturn(new FraudAnalysisResult(false, null));

        Classification savedClassification = new Classification();
        savedClassification.setId(11L);
        when(classificationRepository.save(any(Classification.class))).thenReturn(savedClassification);

        SubmitClassificationRequest testReq = new SubmitClassificationRequest();
        testReq.setImageId(1L);
        testReq.setTaskId(1L);
        testReq.setQuestion("Is this a Bear?");
        testReq.setDecision(Classification.UserResponse.YES);
        testReq.setResponseTimeMs(1500L); // Need to set this for the mock

        classificationService.submitClassification("testuser", "USER", 0.8, testReq);

        ArgumentCaptor<Classification> classificationCaptor = ArgumentCaptor.forClass(Classification.class);
        verify(classificationRepository, times(1)).save(classificationCaptor.capture());
        assertEquals("Bear", classificationCaptor.getValue().getQuerySpecies());

        ArgumentCaptor<ClassificationSubmittedEvent> eventCaptor = ArgumentCaptor.forClass(ClassificationSubmittedEvent.class);
        verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());
        assertEquals("Bear", eventCaptor.getValue().getSpecies());
    }
}
