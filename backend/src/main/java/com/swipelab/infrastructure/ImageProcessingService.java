package com.swipelab.infrastructure;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.Iterator;
import java.util.Set;

/**
 * Handles all image processing for species reference images:
 * <ul>
 *   <li>File-type validation (JPEG / PNG / WEBP / GIF / BMP)</li>
 *   <li>Resizing: max 1920px on longest edge, preserving aspect ratio</li>
 *   <li>Compression: JPEG quality 0.80</li>
 *   <li>Thumbnail generation: max 200px on longest edge</li>
 * </ul>
 *
 * Uses only Java 21's built-in {@code javax.imageio} — no extra Maven dependency.
 */
@Slf4j
@Service
public class ImageProcessingService {

    private static final int MAX_FULL_PX      = 1920;
    private static final int MAX_THUMB_PX     = 200;
    private static final float JPEG_QUALITY   = 0.80f;
    private static final long MAX_BYTES       = 5L * 1024 * 1024; // 5 MB pre-compression gate

    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "image/jpeg", "image/jpg", "image/png",
            "image/webp", "image/gif", "image/bmp"
    );

    /** Output record: both base64 images + compressed file size. */
    public record ProcessedImageResult(
            String imageBase64,
            String thumbnailBase64,
            long fileSizeBytes
    ) {}

    public ImageProcessingService() {
        // No longer creating local directories
    }

    /**
     * Validates, compresses, and generates a thumbnail for an uploaded image file.
     *
     * @param file the raw multipart upload
     * @return paths to the compressed full image and its thumbnail
     * @throws IllegalArgumentException for unsupported type or size violations
     * @throws IOException              on disk I/O failure
     */
    public ProcessedImageResult processAndStore(MultipartFile file) throws IOException {
        validateMimeType(file);
        validateFileSize(file);

        BufferedImage original = readImage(file);

        // Compress + resize full image
        BufferedImage compressed = resizeIfNeeded(original, MAX_FULL_PX);
        byte[] fullBytes = encodeToJpeg(compressed, JPEG_QUALITY);
        String fullBase64 = Base64.getEncoder().encodeToString(fullBytes);

        // Generate thumbnail from the already-resized image
        BufferedImage thumb = resizeIfNeeded(compressed, MAX_THUMB_PX);
        byte[] thumbBytes = encodeToJpeg(thumb, JPEG_QUALITY);
        String thumbBase64 = Base64.getEncoder().encodeToString(thumbBytes);

        long sizeBytes = fullBytes.length;
        log.info("Processed reference image → size={}B", sizeBytes);

        return new ProcessedImageResult(
                fullBase64,
                thumbBase64,
                sizeBytes
        );
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private void validateMimeType(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException(
                    "Unsupported image type: " + contentType +
                    ". Accepted: jpeg, png, webp, gif, bmp.");
        }
    }

    private void validateFileSize(MultipartFile file) {
        if (file.getSize() > MAX_BYTES) {
            throw new IllegalArgumentException(
                    "File too large: " + (file.getSize() / 1024 / 1024) + "MB. Max: 5MB.");
        }
    }

    private BufferedImage readImage(MultipartFile file) throws IOException {
        try (InputStream in = file.getInputStream()) {
            BufferedImage img = ImageIO.read(in);
            if (img == null) {
                throw new IllegalArgumentException("Cannot decode image — unsupported or corrupt format.");
            }
            // Normalise to RGB — avoids JPEG writer failing on ARGB/indexed images
            if (img.getType() != BufferedImage.TYPE_INT_RGB) {
                BufferedImage rgb = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_RGB);
                Graphics2D g = rgb.createGraphics();
                g.drawImage(img, 0, 0, Color.WHITE, null);
                g.dispose();
                return rgb;
            }
            return img;
        }
    }

    /**
     * Returns a scaled copy of {@code src} if its longest edge exceeds {@code maxPx}.
     * If already within limits, returns the original instance unchanged.
     */
    private BufferedImage resizeIfNeeded(BufferedImage src, int maxPx) {
        int w = src.getWidth();
        int h = src.getHeight();
        if (Math.max(w, h) <= maxPx) {
            return src;
        }
        double scale = (double) maxPx / Math.max(w, h);
        int newW = (int) Math.round(w * scale);
        int newH = (int) Math.round(h * scale);

        BufferedImage out = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING,      RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(src, 0, 0, newW, newH, null);
        g.dispose();
        return out;
    }

    /** Encodes a BufferedImage to a JPEG byte array at the given quality (0.0–1.0). */
    private byte[] encodeToJpeg(BufferedImage img, float quality) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
        if (!writers.hasNext()) {
            throw new IllegalStateException("No JPEG ImageWriter found on this JVM");
        }
        ImageWriter writer = writers.next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(quality);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             javax.imageio.stream.ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {
            writer.setOutput(ios);
            writer.write(null, new IIOImage(img, null, null), param);
            return baos.toByteArray();
        } finally {
            writer.dispose();
        }
    }
}
