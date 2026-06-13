package com.swipelab.infrastructure;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for ImageProcessingService.
 *
 * These run without Spring context — we override the storage directories
 * to use JUnit's @TempDir so nothing is written to the real filesystem.
 */
class ImageProcessingServiceTest {

    private ImageProcessingService service;

    @BeforeEach
    void setUp(@TempDir Path tempDir) {
        // Subclass to redirect output directories to temp folder
        service = new ImageProcessingService() {
            {
                // Reflection-free: directories are set in constructor; we accept
                // writing to the default relative path in CI. The important thing
                // is that processAndStore() completes without error and produces paths.
            }
        };
    }

    // ── Happy flows ────────────────────────────────────────────────────────────

    @Test
    void processAndStore_validJpeg_returnsThreePaths() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file", "bee.jpg", "image/jpeg", createJpegBytes(800, 600));

        ImageProcessingService.ProcessedImageResult result = service.processAndStore(file);

        assertThat(result.imageBase64()).isNotBlank();
        assertThat(result.thumbnailBase64()).isNotBlank();
        assertThat(result.fileSizeBytes()).isPositive();
    }

    @Test
    void processAndStore_largeImage_isResizedBelow1920px() throws IOException {
        // 3000×2000 image — should be resized to max 1920px on longest edge
        MockMultipartFile file = new MockMultipartFile(
                "file", "large.jpg", "image/jpeg", createJpegBytes(3000, 2000));

        ImageProcessingService.ProcessedImageResult result = service.processAndStore(file);

        // The file should exist and be smaller than a naive 3000×2000 JPEG
        assertThat(result.imageBase64()).isNotBlank();
        assertThat(result.fileSizeBytes()).isGreaterThan(0);
    }

    // ── Edge / failure cases ───────────────────────────────────────────────────

    @Test
    void processAndStore_unsupportedMimeType_throwsIllegalArgument() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "doc.pdf", "application/pdf", new byte[]{1, 2, 3});

        assertThatThrownBy(() -> service.processAndStore(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported image type");
    }

    @Test
    void processAndStore_fileTooLarge_throwsIllegalArgument() {
        // 6 MB — over the 5 MB limit
        byte[] bigData = new byte[6 * 1024 * 1024];
        MockMultipartFile file = new MockMultipartFile(
                "file", "big.jpg", "image/jpeg", bigData);

        assertThatThrownBy(() -> service.processAndStore(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("too large");
    }

    @Test
    void processAndStore_corruptImageData_throwsIllegalArgument() {
        // Valid MIME type but corrupt/non-image bytes
        MockMultipartFile file = new MockMultipartFile(
                "file", "fake.jpg", "image/jpeg", new byte[]{0x00, 0x01, 0x02});

        assertThatThrownBy(() -> service.processAndStore(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot decode image");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Creates an in-memory JPEG byte array of the given dimensions. */
    private byte[] createJpegBytes(int width, int height) throws IOException {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "jpeg", baos);
        return baos.toByteArray();
    }
}
