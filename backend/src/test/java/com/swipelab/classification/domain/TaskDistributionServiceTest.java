package com.swipelab.classification.domain;

import com.swipelab.classification.infrastructure.ClassificationRepository;
import com.swipelab.classification.infrastructure.ImageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
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

    private static final List<String> SPECIES = List.of("Bat");

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
    }

    @Test
    void getNextImageForUser_ShouldReturnRegularImage() {
        when(classificationRepository.countByUsernameAndTaskId("testuser", 1L)).thenReturn(0L);
        when(imageRepository.findRegularImageCandidatesForUser(eq("testuser"), eq(1L), eq(1), any(PageRequest.class)))
                .thenReturn(List.of(regularImage2));
        when(classificationRepository.findQueriedSpeciesByUsernameAndImageId("testuser", 2L))
                .thenReturn(Collections.emptyList());

        Optional<TaskDistributionService.ImageSpeciesPair> result =
                taskDistributionService.getNextImageForUser("testuser", 1L, SPECIES);

        assertTrue(result.isPresent());
        assertEquals(2L, result.get().image().getId());
        assertEquals("Bat", result.get().species());
    }

    @Test
    void getNextImageForUser_ShouldReturnGoldImage_WhenCountIs15() {
        when(classificationRepository.countByUsernameAndTaskId("testuser", 1L)).thenReturn(15L);
        when(imageRepository.findUnclassifiedGoldImages("testuser", 1L))
                .thenReturn(List.of(goldImage));
        when(classificationRepository.findQueriedSpeciesByUsernameAndImageId("testuser", 3L))
                .thenReturn(Collections.emptyList());

        Optional<TaskDistributionService.ImageSpeciesPair> result =
                taskDistributionService.getNextImageForUser("testuser", 1L, SPECIES);

        assertTrue(result.isPresent());
        assertEquals(3L, result.get().image().getId());
        assertEquals("Bat", result.get().species());
    }

    @Test
    void getNextImageForUser_ShouldFallbackToRegularImage_WhenNoGoldAvailable() {
        when(classificationRepository.countByUsernameAndTaskId("testuser", 1L)).thenReturn(15L);
        when(imageRepository.findUnclassifiedGoldImages("testuser", 1L))
                .thenReturn(Collections.emptyList());
        when(imageRepository.findRegularImageCandidatesForUser(eq("testuser"), eq(1L), eq(1), any(PageRequest.class)))
                .thenReturn(List.of(regularImage1));
        when(classificationRepository.findQueriedSpeciesByUsernameAndImageId("testuser", 1L))
                .thenReturn(Collections.emptyList());

        Optional<TaskDistributionService.ImageSpeciesPair> result =
                taskDistributionService.getNextImageForUser("testuser", 1L, SPECIES);

        assertTrue(result.isPresent());
        assertEquals(1L, result.get().image().getId());
    }

    @Test
    void resetUserSession_ShouldBeNoOp() {
        taskDistributionService.resetUserSession("testuser", 1L);
        verifyNoInteractions(classificationRepository);
        verifyNoInteractions(imageRepository);
    }
}

