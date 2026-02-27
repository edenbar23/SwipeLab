package com.swipelab.classification.domain;

import com.swipelab.classification.infrastructure.GoldImageRepository;
import com.swipelab.classification.infrastructure.ImageRepository;
import com.swipelab.dto.request.GoldImageRequest;
import com.swipelab.dto.response.GoldImageResponse;
import com.swipelab.exception.ResourceNotFoundException;
import com.swipelab.tasks.domain.Task;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GoldImageServiceTest {

    @Mock
    private GoldImageRepository goldImageRepository;

    @Mock
    private ImageRepository imageRepository;

    @InjectMocks
    private GoldImageService goldImageService;

    private Image image;
    private GoldImage goldImage;
    private GoldImageRequest request;
    private Task task;

    @BeforeEach
    void setUp() {
        task = new Task();
        task.setId(1L);

        image = new Image();
        image.setId(1L);
        image.setTask(task);

        goldImage = new GoldImage();
        goldImage.setId(1L);
        goldImage.setImage(image);
        goldImage.setSpecies("Lion");
        goldImage.setCorrectAnswer(GoldImage.UserResponse.YES);

        request = new GoldImageRequest();
        request.setImageId(1L);
        request.setSpecies("Lion");
        request.setCorrectAnswer(GoldImage.UserResponse.YES);
    }

    @Test
    void createGoldImage_ShouldReturnResponse_WhenValidRequest() {
        when(imageRepository.findById(1L)).thenReturn(Optional.of(image));
        when(goldImageRepository.save(any(GoldImage.class))).thenReturn(goldImage);

        GoldImageResponse response = goldImageService.createGoldImage(request);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Lion", response.getSpecies());
        verify(goldImageRepository, times(1)).save(any(GoldImage.class));
    }

    @Test
    void createGoldImage_ShouldThrowException_WhenImageNotFound() {
        when(imageRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> goldImageService.createGoldImage(request));
        verify(goldImageRepository, never()).save(any(GoldImage.class));
    }

    @Test
    void getGoldImagesByTask_ShouldReturnValidList() {
        when(goldImageRepository.findAll()).thenReturn(Collections.singletonList(goldImage));

        List<GoldImageResponse> responses = goldImageService.getGoldImagesByTask(1L);

        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals(1L, responses.get(0).getId());
    }

    @Test
    void getGoldImageById_ShouldReturnResponse_WhenExists() {
        when(goldImageRepository.findById(1L)).thenReturn(Optional.of(goldImage));

        GoldImageResponse response = goldImageService.getGoldImageById(1L);

        assertNotNull(response);
        assertEquals(1L, response.getId());
    }

    @Test
    void getGoldImageById_ShouldThrowException_WhenNotFound() {
        when(goldImageRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> goldImageService.getGoldImageById(1L));
    }

    @Test
    void updateGoldImage_ShouldUpdateAndReturnResponse() {
        when(goldImageRepository.findById(1L)).thenReturn(Optional.of(goldImage));
        when(goldImageRepository.save(any(GoldImage.class))).thenReturn(goldImage);

        GoldImageResponse response = goldImageService.updateGoldImage(1L, request);

        assertNotNull(response);
        assertEquals("Lion", response.getSpecies());
        verify(goldImageRepository, times(1)).save(goldImage);
    }

    @Test
    void deleteGoldImage_ShouldCallDelete() {
        goldImageService.deleteGoldImage(1L);
        verify(goldImageRepository, times(1)).deleteById(1L);
    }

    @Test
    void getAllGoldImages_ShouldReturnList() {
        when(goldImageRepository.findAll()).thenReturn(Arrays.asList(goldImage, goldImage));

        List<GoldImageResponse> responses = goldImageService.getAllGoldImages();

        assertEquals(2, responses.size());
        verify(goldImageRepository, times(1)).findAll();
    }
}
