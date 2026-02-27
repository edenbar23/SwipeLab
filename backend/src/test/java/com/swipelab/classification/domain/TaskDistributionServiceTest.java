package com.swipelab.classification.domain;

import com.swipelab.classification.infrastructure.ClassificationRepository;
import com.swipelab.classification.infrastructure.ImageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskDistributionServiceTest {

    @Mock
    private ImageRepository imageRepository;

    @Mock
    private ClassificationRepository classificationRepository;

    @InjectMocks
    private TaskDistributionService taskDistributionService;

    private Image regularImage1;
    private Image regularImage2;
    private Image goldImage;

    @BeforeEach
    void setUp() {
        regularImage1 = new Image();
        regularImage1.setId(1L);
        regularImage1.setPriority(1);

        regularImage2 = new Image();
        regularImage2.setId(2L);
        regularImage2.setPriority(2);

        goldImage = new Image();
        goldImage.setId(3L);
        
        taskDistributionService.resetUserSession("testuser", 1L);
    }

    @Test
    void getNextImageForUser_ShouldReturnRegularImage() {
        when(imageRepository.findRegularImagesByTaskId(1L)).thenReturn(Arrays.asList(regularImage1, regularImage2));
        when(classificationRepository.existsByUsernameAndImageId("testuser", 1L)).thenReturn(false);
        when(classificationRepository.existsByUsernameAndImageId("testuser", 2L)).thenReturn(false);
        when(classificationRepository.countByImage_Id(anyLong())).thenReturn(0L);

        Optional<Image> result = taskDistributionService.getNextImageForUser("testuser", 1L);

        assertTrue(result.isPresent());
        assertEquals(2L, result.get().getId()); // priority 2 is higher than 1
    }

    @Test
    void getNextImageForUser_ShouldReturnGoldImage_WhenCountIs15() {
        Map<String, Integer> userClassificationCount = (Map<String, Integer>) ReflectionTestUtils.getField(taskDistributionService, "userClassificationCount");
        userClassificationCount.put("testuser_1", 15);

        when(imageRepository.findGoldStandardImagesByTaskId(1L)).thenReturn(Collections.singletonList(goldImage));
        when(classificationRepository.existsByUsernameAndImageId("testuser", 3L)).thenReturn(false);

        Optional<Image> result = taskDistributionService.getNextImageForUser("testuser", 1L);

        assertTrue(result.isPresent());
        assertEquals(3L, result.get().getId());
    }

    @Test
    void getNextImageForUser_ShouldFallbackToRegularImage_WhenNoGoldAvailable() {
        Map<String, Integer> userClassificationCount = (Map<String, Integer>) ReflectionTestUtils.getField(taskDistributionService, "userClassificationCount");
        userClassificationCount.put("testuser_1", 15);

        when(imageRepository.findGoldStandardImagesByTaskId(1L)).thenReturn(Collections.emptyList());
        when(imageRepository.findRegularImagesByTaskId(1L)).thenReturn(Collections.singletonList(regularImage1));
        when(classificationRepository.existsByUsernameAndImageId("testuser", 1L)).thenReturn(false);
        when(classificationRepository.countByImage_Id(1L)).thenReturn(0L);

        Optional<Image> result = taskDistributionService.getNextImageForUser("testuser", 1L);

        assertTrue(result.isPresent());
        assertEquals(1L, result.get().getId());
    }

    @Test
    void resetUserSession_ShouldRemoveSessionFromMap() {
        Map<String, Integer> userClassificationCount = (Map<String, Integer>) ReflectionTestUtils.getField(taskDistributionService, "userClassificationCount");
        userClassificationCount.put("testuser_1", 15);

        taskDistributionService.resetUserSession("testuser", 1L);

        assertEquals(0, userClassificationCount.size());
    }
}
