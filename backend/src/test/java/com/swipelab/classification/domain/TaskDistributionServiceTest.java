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

    @Mock
    private com.swipelab.classification.infrastructure.GoldImageRepository goldImageRepository;



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
    void getNextRegularImagePair_ShouldReturnRegularImage() {
        when(imageRepository.findRegularImageCandidatesForUser(eq("testuser"), eq(1L), eq(1), any(PageRequest.class)))
                .thenReturn(List.of(regularImage2));
        when(classificationRepository.findQueriedSpeciesByUsernameAndImageId("testuser", 2L))
                .thenReturn(Collections.emptyList());

        Optional<TaskDistributionService.ImageSpeciesPair> result =
                taskDistributionService.getNextRegularImagePair("testuser", 1L, SPECIES);

        assertTrue(result.isPresent());
        assertEquals(2L, result.get().image().getId());
        assertEquals("Bat", result.get().species());
    }

    @Test
    void getNextGoldImagePair_ShouldReturnGoldImage() {
        when(imageRepository.findUnclassifiedGoldImages("testuser", SPECIES))
                .thenReturn(List.of(goldImage));
        
        com.swipelab.classification.domain.GoldImage mockGoldImage = new com.swipelab.classification.domain.GoldImage();
        mockGoldImage.setSpecies("Bat");
        when(goldImageRepository.findByImageIdAndActiveTrue(3L)).thenReturn(Optional.of(mockGoldImage));

        Optional<TaskDistributionService.ImageSpeciesPair> result =
                taskDistributionService.getNextGoldImagePair("testuser", 1L, SPECIES);

        assertTrue(result.isPresent());
        assertEquals(3L, result.get().image().getId());
        assertEquals("Bat", result.get().species());
    }

    @Test
    void getNextGoldImagePair_ShouldReturnEmpty_WhenNoGoldAvailable() {
        when(imageRepository.findUnclassifiedGoldImages("testuser", SPECIES))
                .thenReturn(Collections.emptyList());

        Optional<TaskDistributionService.ImageSpeciesPair> result =
                taskDistributionService.getNextGoldImagePair("testuser", 1L, SPECIES);

        assertFalse(result.isPresent());
    }

    @Test
    void resetUserSession_ShouldBeNoOp() {
        taskDistributionService.resetUserSession("testuser", 1L);
        verifyNoInteractions(classificationRepository);
        verifyNoInteractions(imageRepository);
    }
}

