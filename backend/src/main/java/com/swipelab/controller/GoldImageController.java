package com.swipelab.controller;

import com.swipelab.dto.request.GoldImageRequest;
import com.swipelab.dto.response.GoldImageResponse;
import com.swipelab.classification.application.GoldImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/gold-images")
@RequiredArgsConstructor
public class GoldImageController {

    private final GoldImageService goldImageService;

    @PostMapping
    public ResponseEntity<GoldImageResponse> createGoldImage(@RequestBody GoldImageRequest request) {
        GoldImageResponse response = goldImageService.createGoldImage(request);
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
}
