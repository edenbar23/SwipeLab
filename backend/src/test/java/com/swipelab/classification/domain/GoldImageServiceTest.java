package com.swipelab.classification.domain;

import com.swipelab.classification.infrastructure.GoldImageRepository;
import com.swipelab.classification.infrastructure.ImageRepository;
import com.swipelab.dto.request.GoldImageRequest;
import com.swipelab.dto.response.GoldImageResponse;
import com.swipelab.exception.ResourceNotFoundException;
import com.swipelab.classification.application.port.out.TaskProvider;
import com.swipelab.infrastructure.FileStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

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

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private TaskProvider taskProvider;

    @InjectMocks
    private GoldImageService goldImageService;

    private Image image;
    private GoldImage goldImage;
    private GoldImageRequest request;
    private TaskProvider.TaskInfo taskInfo;

    private static final String APP_BASE_URL = "http://localhost:8080";

    @BeforeEach
    void setUp() {
        // Inject @Value field that Mockito cannot set automatically
        ReflectionTestUtils.setField(goldImageService, "appBaseUrl", APP_BASE_URL);

        taskInfo = new TaskProvider.TaskInfo(1L, "Question", "Lion", Collections.emptyList());

        image = new Image();
        image.setId(1L);
        image.setTaskId(1L);
        image.setSrcPath("/uploads/test-uuid.jpg");

        goldImage = new GoldImage();
        goldImage.setId(1L);
        goldImage.setImage(image);
        goldImage.setSpecies("Lion");
        goldImage.setCorrectAnswer(GoldImage.UserResponse.YES);
        goldImage.setActive(true);

        request = new GoldImageRequest();
        request.setImageId(1L);
        request.setSpecies("Lion");
        request.setCorrectAnswer(GoldImage.UserResponse.YES);
    }

    // ── createGoldImage ──────────────────────────────────────────────────────

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

    // ── uploadGoldImage ──────────────────────────────────────────────────────

    @Test
    void uploadGoldImage_WithFile_ShouldStoreFileAndReturnResponse() {
        MultipartFile mockFile = mock(MultipartFile.class);
        when(mockFile.isEmpty()).thenReturn(false);
        when(fileStorageService.storeFile(mockFile)).thenReturn("/uploads/stored-uuid.png");
        when(taskProvider.getTaskInfo(1L)).thenReturn(taskInfo);
        when(imageRepository.save(any(Image.class))).thenReturn(image);
        when(goldImageRepository.save(any(GoldImage.class))).thenReturn(goldImage);

        GoldImageResponse response = goldImageService.uploadGoldImage(mockFile, null, 1L, "Lion", "YES");

        assertNotNull(response);
        assertEquals(1L, response.getId());
        // imageUrl must resolve to the opaque API endpoint, not the raw file path
        assertTrue(response.getImageUrl().contains("/api/admin/gold-images/1/image"));
        verify(fileStorageService).storeFile(mockFile);
        verify(fileStorageService, never()).storeFileFromUrl(any());
    }

    @Test
    void uploadGoldImage_WithUrl_ShouldDownloadAndStoreLocally() {
        String remoteUrl = "https://example.com/bee.jpg";
        when(fileStorageService.storeFileFromUrl(remoteUrl)).thenReturn("/uploads/downloaded-uuid.jpg");
        when(taskProvider.getTaskInfo(1L)).thenReturn(taskInfo);
        when(imageRepository.save(any(Image.class))).thenReturn(image);
        when(goldImageRepository.save(any(GoldImage.class))).thenReturn(goldImage);

        GoldImageResponse response = goldImageService.uploadGoldImage(null, remoteUrl, 1L, "Lion", "YES");

        assertNotNull(response);
        // External URL must never be returned as imageUrl — it must be the local API endpoint
        assertFalse(response.getImageUrl().startsWith("https://example.com"),
                "Raw external URL must not leak into the response");
        assertTrue(response.getImageUrl().contains("/api/admin/gold-images/"),
                "Response imageUrl should point to the internal image endpoint");
        verify(fileStorageService).storeFileFromUrl(remoteUrl);
        verify(fileStorageService, never()).storeFile(any());
    }

    @Test
    void uploadGoldImage_WithNeitherFileNorUrl_ShouldThrow() {
        assertThrows(IllegalArgumentException.class,
                () -> goldImageService.uploadGoldImage(null, null, 1L, "Lion", "YES"));
        verify(fileStorageService, never()).storeFile(any());
        verify(fileStorageService, never()).storeFileFromUrl(any());
    }

    // ── mapToResponse / imageUrl resolution ──────────────────────────────────

    @Test
    void getGoldImageById_ShouldReturnAbsoluteApiUrl_ForLocalUpload() {
        when(goldImageRepository.findById(1L)).thenReturn(Optional.of(goldImage));

        GoldImageResponse response = goldImageService.getGoldImageById(1L);

        assertNotNull(response);
        assertEquals(
                APP_BASE_URL + "/api/admin/gold-images/1/image",
                response.getImageUrl(),
                "Locally uploaded images should be served via the API endpoint"
        );
    }

    @Test
    void mapToResponse_ShouldPassThroughExternalUrl_WhenSrcPathIsHttp() {
        image.setSrcPath("https://external.example.com/bee.jpg");
        when(goldImageRepository.findById(1L)).thenReturn(Optional.of(goldImage));

        GoldImageResponse response = goldImageService.getGoldImageById(1L);

        assertEquals("https://external.example.com/bee.jpg", response.getImageUrl(),
                "External URLs stored as srcPath should be returned unchanged");
    }

    // ── getImageResource ─────────────────────────────────────────────────────

    @Test
    void getImageResource_ShouldReturnResource_ForLocalUpload() {
        Resource mockResource = mock(Resource.class);
        when(goldImageRepository.findById(1L)).thenReturn(Optional.of(goldImage));
        when(fileStorageService.loadFile("/uploads/test-uuid.jpg")).thenReturn(mockResource);

        Resource result = goldImageService.getImageResource(1L);

        assertSame(mockResource, result);
        verify(fileStorageService).loadFile("/uploads/test-uuid.jpg");
    }

    @Test
    void getImageResource_ShouldThrow_WhenImageNotFound() {
        when(goldImageRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> goldImageService.getImageResource(99L));
        verify(fileStorageService, never()).loadFile(any());
    }

    @Test
    void getImageResource_ShouldThrow_WhenSrcPathIsExternalUrl() {
        image.setSrcPath("https://external.example.com/bee.jpg");
        when(goldImageRepository.findById(1L)).thenReturn(Optional.of(goldImage));

        assertThrows(IllegalArgumentException.class, () -> goldImageService.getImageResource(1L),
                "Requesting image bytes for an external-URL image must throw");
    }

    // ── other CRUD ───────────────────────────────────────────────────────────

    @Test
    void getGoldImagesByTask_ShouldReturnValidList() {
        when(goldImageRepository.findAllByActiveTrue()).thenReturn(Collections.singletonList(goldImage));

        List<GoldImageResponse> responses = goldImageService.getGoldImagesByTask(1L);

        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals(1L, responses.get(0).getId());
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
    void deleteGoldImage_ShouldSoftDelete_WhenExists() {
        when(goldImageRepository.findById(1L)).thenReturn(Optional.of(goldImage));
        when(goldImageRepository.save(any(GoldImage.class))).thenReturn(goldImage);

        goldImageService.deleteGoldImage(1L);

        assertFalse(goldImage.getActive(), "active flag must be set to false after soft-delete");
        verify(goldImageRepository).save(goldImage);
        // Physical DELETE must never be called
        verify(goldImageRepository, never()).deleteById(any());
    }

    @Test
    void deleteGoldImage_ShouldThrow_WhenGoldImageNotFound() {
        when(goldImageRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> goldImageService.deleteGoldImage(99L));
        verify(goldImageRepository, never()).save(any());
    }

    @Test
    void getAllGoldImages_ShouldReturnList() {
        when(goldImageRepository.findAllByActiveTrue()).thenReturn(Arrays.asList(goldImage, goldImage));

        List<GoldImageResponse> responses = goldImageService.getAllGoldImages();

        assertEquals(2, responses.size());
        verify(goldImageRepository, times(1)).findAllByActiveTrue();
    }
}
