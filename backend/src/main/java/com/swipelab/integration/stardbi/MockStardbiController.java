package com.swipelab.integration.stardbi;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@Profile("mock")
public class MockStardbiController {

    @PostMapping("/auth/get_token/")
    public ResponseEntity<Object> getToken(@RequestBody Map<String, Object> request) {
        String username = (String) request.get("username");
        log.info("Mock Stardbi login for {}", username);
        return ResponseEntity.ok(Map.of(
                "access", "mock-access-" + UUID.randomUUID(),
                "refresh", "mock-refresh-" + UUID.randomUUID(),
                "lifetime", 3600,
                "id", 1L,
                "username", username != null ? username : "mockuser",
                "first_name", "Mock",
                "last_name", "User",
                "email", (username != null ? username : "mockuser") + "@mock.com"
        ));
    }

    @GetMapping("/auth/check_auth/")
    public ResponseEntity<Void> checkAuth(@RequestHeader("Authorization") String authHeader) {
        log.info("Mock Stardbi check auth");
        return ResponseEntity.ok().build();
    }

    @PostMapping("/auth/token_refresh/")
    public ResponseEntity<Object> refreshToken(@RequestBody Map<String, Object> request) {
        log.info("Mock Stardbi refresh token");
        return ResponseEntity.ok(Map.of(
                "access", "mock-new-access-" + UUID.randomUUID(),
                "refresh", "mock-new-refresh-" + UUID.randomUUID(),
                "lifetime", 3600
        ));
    }

    @PostMapping("/auth/logout/")
    public ResponseEntity<Void> logout(@RequestBody Map<String, Object> request, @RequestHeader("Authorization") String authHeader) {
        log.info("Mock Stardbi logout");
        return ResponseEntity.ok().build();
    }

    @GetMapping("/swipelab/experiments/")
    public ResponseEntity<Object> getExperiments() {
        log.info("Mock Stardbi get experiments");
        return ResponseEntity.ok(List.of(
                Map.of(
                        "id", 1L,
                        "name", "Mock Experiment 1",
                        "startDate", "2025-01-01",
                        "emdDate", "2025-12-31",
                        "notes", "Mock experiment notes"
                ),
                Map.of(
                        "id", 2L,
                        "name", "Mock Experiment 2",
                        "startDate", "2026-01-01",
                        "emdDate", "2026-12-31",
                        "notes", "Another mock experiment"
                )
        ));
    }

    @GetMapping("/swipelab/bounding_boxes/")
    public ResponseEntity<Object> getBoundingBoxes(@RequestParam("experiment") Long experimentId) {
        log.info("Mock Stardbi get bounding boxes for experiment {}", experimentId);
        return ResponseEntity.ok(List.of(101L, 102L, 103L, 104L, 105L));
    }

    @GetMapping("/swipelab/bounding_boxes/{id}/")
    public ResponseEntity<Resource> getBoundingBoxImage(@PathVariable("id") Long id) {
        log.info("Mock Stardbi get bounding box image {}", id);
        try {
            Path imagePath = Paths.get("src/main/resources/mock-images/" + id + ".png");
            MediaType mediaType = MediaType.IMAGE_PNG;
            
            if (!Files.exists(imagePath)) {
                imagePath = Paths.get("src/main/resources/mock-images/" + id + ".jpg");
                mediaType = MediaType.IMAGE_JPEG;
            }
            
            if (Files.exists(imagePath)) {
                Resource resource = new UrlResource(imagePath.toUri());
                return ResponseEntity.ok()
                        .contentType(mediaType)
                        .body(resource);
            }
        } catch (Exception e) {
            log.warn("Could not load mock image {}", id, e);
        }
        
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/swipelab/taxonomy/")
    public ResponseEntity<Object> getTaxonomy() {
        log.info("Mock Stardbi get taxonomy");
        return ResponseEntity.ok(List.of(
                Map.of(
                        "species_id", 1L,
                        "clazz", "insecta", // Notice it maps to clazz or class depending on how the DTO is written. We will use `class` if possible, but map.of won't care. Wait: Jackson usually serializes map string as is. DTO might annotate it as @JsonProperty("class").
                        "class", "insecta", 
                        "order", "Lepidoptera",
                        "family", "Nymphalidae",
                        "genus", "Danaus",
                        "species", "D. plexippus"
                ),
                Map.of(
                        "species_id", 2L,
                        "class", "insecta",
                        "order", "Coleoptera",
                        "family", "Coccinellidae",
                        "genus", "Harmonia",
                        "species", "H. axyridis"
                )
        ));
    }

    @PostMapping("/swipelab/classification/")
    public ResponseEntity<Object> submitClassifications(@RequestBody List<Map<String, Object>> classifications) {
        log.info("Mock Stardbi submit classifications: {}", classifications.size());
        return ResponseEntity.ok(classifications);
    }
}
