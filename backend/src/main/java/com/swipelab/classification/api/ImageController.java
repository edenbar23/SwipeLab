package com.swipelab.classification.api;

import com.swipelab.dto.request.ImageUploadRequest;
import com.swipelab.dto.response.ImageBatchResponse;
import com.swipelab.dto.response.ImageResponse;
import com.swipelab.classification.domain.ImageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/images")
@RequiredArgsConstructor
public class ImageController {

    private final ImageService imageService;

    @PostMapping("/upload")
    public ResponseEntity<ImageResponse> uploadImage(@Valid @RequestBody ImageUploadRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(imageService.uploadImage(request));
    }

    @GetMapping("/batch")
    public ResponseEntity<ImageBatchResponse> getImageBatch(
            @RequestParam Long taskId,
            @AuthenticationPrincipal UserDetails userDetails) {
        // Extract username from authenticated user to filter already classified images
        String username = userDetails != null ? userDetails.getUsername() : null;
        return ResponseEntity.ok(imageService.getImageBatch(taskId, username));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ImageResponse> getImageById(@PathVariable Long id) {
        return ResponseEntity.ok(imageService.getImageById(id));
    }
}
