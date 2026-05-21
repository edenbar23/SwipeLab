package com.swipelab.classification.api;

import com.swipelab.dto.request.GoldImageRequest;
import com.swipelab.dto.response.GoldImageResponse;
import com.swipelab.classification.domain.GoldImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@PreAuthorize("hasRole('RESEARCHER') or @securityAuthorizationService.isSuperAdmin(authentication.name)")
@RequestMapping("/api/admin/gold-images")
@RequiredArgsConstructor
public class GoldImageController {

    private final GoldImageService goldImageService;

    //get all tasks 
    @GetMapping("/get-all")
    public ResponseEntity<List<GoldImageResponse>> getAllGoldImages() {
        List<GoldImageResponse> responses = goldImageService.getAllGoldImages();
        return ResponseEntity.ok(responses);
    }

    @PostMapping
    public ResponseEntity<GoldImageResponse> createGoldImage(@RequestBody GoldImageRequest request) {
        GoldImageResponse response = goldImageService.createGoldImage(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping(value = "/upload", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<GoldImageResponse> uploadGoldImage(
            @RequestParam(value = "file", required = false) org.springframework.web.multipart.MultipartFile file,
            @RequestParam(value = "imageUrl", required = false) String imageUrl,
            @RequestParam("taskId") Long taskId,
            @RequestParam("species") String species,
            @RequestParam(value = "correctAnswer", defaultValue = "YES") String correctAnswer) {
        GoldImageResponse response = goldImageService.uploadGoldImage(file, imageUrl, taskId, species, correctAnswer);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<GoldImageResponse>> getGoldImagesByTask(@RequestParam Long taskId) {
        List<GoldImageResponse> responses = goldImageService.getGoldImagesByTask(taskId);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<GoldImageResponse> getGoldImageById(@PathVariable Long id) {
        GoldImageResponse response = goldImageService.getGoldImageById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<GoldImageResponse> updateGoldImage(
            @PathVariable Long id,
            @RequestBody GoldImageRequest request) {
        GoldImageResponse response = goldImageService.updateGoldImage(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGoldImage(@PathVariable Long id) {
        goldImageService.deleteGoldImage(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Streams the raw image bytes for a locally-uploaded gold image.
     * This endpoint is permitted without authentication (see SecurityConfig) so
     * that React Native's Image component can load it without custom headers.
     * No filesystem path is exposed — only the gold-image ID is used.
     */
    @GetMapping("/{id}/image")
    @PreAuthorize("permitAll()")
    public ResponseEntity<Resource> serveImage(@PathVariable Long id) {
        Resource resource = goldImageService.getImageResource(id);
        String contentType = "image/jpeg"; // fallback
        try {
            String filename = resource.getFilename();
            if (filename != null) {
                if (filename.endsWith(".png")) contentType = "image/png";
                else if (filename.endsWith(".gif")) contentType = "image/gif";
                else if (filename.endsWith(".webp")) contentType = "image/webp";
            }
        } catch (Exception ignored) {}
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "max-age=86400, public")
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
    }
}
