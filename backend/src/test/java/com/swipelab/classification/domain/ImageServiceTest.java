package com.swipelab.classification.domain;

import com.swipelab.classification.dto.api.BatchImageDto;
import com.swipelab.classification.dto.api.NextBatchResponse;
import com.swipelab.classification.infrastructure.ClassificationRepository;
import com.swipelab.classification.infrastructure.GoldImageRepository;
import com.swipelab.classification.infrastructure.ImageRepository;
import com.swipelab.classification.infrastructure.LabelRepository;
import com.swipelab.dto.request.ImageUploadRequest;
import com.swipelab.dto.response.ImageBatchResponse;
import com.swipelab.dto.response.ImageResponse;
import com.swipelab.exception.ResourceNotFoundException;
import com.swipelab.classification.application.port.out.TaskProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ImageServiceTest {

    @Mock
    private ImageRepository imageRepository;

    @Mock
    private TaskProvider taskProvider;

    @Mock
    private LabelRepository labelRepository;

    @Mock
    private ClassificationRepository classificationRepository;

    @Mock
    private GoldImageRepository goldImageRepository;

    @Mock
    private TaskDistributionService taskDistributionService;

    @Mock
    private GoldImagePolicy goldImagePolicy;

    @InjectMocks
    private ImageService imageService;

    private TaskProvider.TaskInfo taskInfo;
    private Image image;

    @BeforeEach
    void setUp() {
        taskInfo = new TaskProvider.TaskInfo(1L, "Classify this image", "MAMMALS", Collections.emptyList(), Collections.emptyList());

        image = new Image();
        image.setId(1L);
        image.setSrcPath("http://example.com/img.jpg");
        image.setTaskId(1L);
        image.setPriority(1);
    }

    @Test
    void getNextBatchForApi_ShouldReturnImages() {
        when(taskProvider.getTaskInfo(1L)).thenReturn(taskInfo);
        when(goldImagePolicy.shouldIncludeGoldImageInBatch("testuser", 1L, 5)).thenReturn(false);
        when(taskDistributionService.getNextRegularImagePair(anyString(), anyLong(), anyList()))
                .thenReturn(Optional.of(new TaskDistributionService.ImageSpeciesPair(image, null)))
                .thenReturn(Optional.empty());

        NextBatchResponse response = imageService.getNextBatchForApi(1L, "testuser", 5);

        assertNotNull(response);
        assertEquals(1, response.getImages().size());
        BatchImageDto dto = response.getImages().get(0);
        assertEquals(1L, dto.getImageId());
        assertEquals("Classify this image", dto.getQuestion());
    }

    @Test
    void getNextBatchForApi_ShouldThrowException_WhenTaskNotFound() {
        when(taskProvider.getTaskInfo(1L)).thenThrow(new ResourceNotFoundException("Task not found: 1"));

        assertThrows(ResourceNotFoundException.class, () -> 
                imageService.getNextBatchForApi(1L, "testuser", 5));
    }

    @Test
    void uploadImage_ShouldSaveImageAndReturnResponse() {
        ImageUploadRequest request = new ImageUploadRequest();
        request.setTaskId(1L);
        request.setImageUrl("http://example.com/img.jpg");
        request.setCaption("caption");
        request.setPriority(1);
        request.setIsGoldStandard(false);

        when(taskProvider.getTaskInfo(1L)).thenReturn(taskInfo);
        when(imageRepository.save(any(Image.class))).thenReturn(image);
        when(goldImageRepository.existsByImageId(1L)).thenReturn(false);

        ImageResponse response = imageService.uploadImage(request);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("http://example.com/img.jpg", response.getImageUrl());
        verify(imageRepository, times(1)).save(any(Image.class));
    }

    @Test
    void uploadImage_ShouldCreateGoldImage_WhenIsGoldStandardIsTrue() {
        ImageUploadRequest request = new ImageUploadRequest();
        request.setTaskId(1L);
        request.setImageUrl("http://example.com/img.jpg");
        request.setIsGoldStandard(true);
        request.setCorrectLabelId(1L);

        Label label = new Label();
        label.setId(1L);
        label.setName("Lion");

        when(taskProvider.getTaskInfo(1L)).thenReturn(taskInfo);
        when(imageRepository.save(any(Image.class))).thenReturn(image);
        when(labelRepository.findById(1L)).thenReturn(Optional.of(label));

        imageService.uploadImage(request);

        verify(goldImageRepository, times(1)).save(any(GoldImage.class));
    }

    @Test
    void getImageBatch_ShouldReturnUnclassifiedImages() {
        Image image2 = new Image();
        image2.setId(2L);
        image2.setTaskId(1L);

        when(imageRepository.findByTaskId(1L)).thenReturn(Arrays.asList(image, image2));
        when(classificationRepository.existsByUsernameAndImageId("testuser", 1L)).thenReturn(true);
        when(classificationRepository.existsByUsernameAndImageId("testuser", 2L)).thenReturn(false);

        ImageBatchResponse response = imageService.getImageBatch(1L, "testuser");

        assertNotNull(response);
        assertEquals(1, response.getImages().size());
        assertEquals(2L, response.getImages().get(0).getId());  // Image 1 is excluded because it is classified
    }

    @Test
    void getImageById_ShouldReturnResponse_WhenExists() {
        when(imageRepository.findById(1L)).thenReturn(Optional.of(image));
        when(goldImageRepository.existsByImageId(1L)).thenReturn(false);

        ImageResponse response = imageService.getImageById(1L);

        assertNotNull(response);
        assertEquals(1L, response.getId());
    }

    @Test
    void getImageById_ShouldThrowException_WhenNotFound() {
        when(imageRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> imageService.getImageById(1L));
    }
}
