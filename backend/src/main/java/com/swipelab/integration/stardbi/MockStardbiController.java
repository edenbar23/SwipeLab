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
import java.util.Base64;
import java.time.Instant;

@Slf4j
@RestController
@RequestMapping("/stardbi")
@Profile({"mock", "e2e"})
public class MockStardbiController {

    @PostMapping("/auth/get_token/")
    public ResponseEntity<Object> getToken(@RequestBody Map<String, Object> request) {
        String username = (String) request.get("username");
        log.info("Mock Stardbi login for {}", username);
        
        String header = Base64.getUrlEncoder().withoutPadding().encodeToString("{\"alg\":\"none\",\"typ\":\"JWT\"}".getBytes());
        String payload = Base64.getUrlEncoder().withoutPadding().encodeToString(String.format(
            "{\"user_id\":1,\"username\":\"%s\",\"exp\":%d}",
            username != null ? username : "mockuser",
            Instant.now().plusSeconds(3600).getEpochSecond()
        ).getBytes());
        String mockAccessToken = header + "." + payload + ".mock-signature";
        String mockRefreshToken = header + "." + payload + ".mock-refresh-sig";

        return ResponseEntity.ok(Map.of(
                "access", mockAccessToken,
                "refresh", mockRefreshToken,
                "lifetime", 3600,
                "id", 1L,
                "username", username != null ? username : "mockuser",
                "first_name", "Mock",
                "last_name", "Researcher",
                "email", (username != null ? username : "mockuser") + "@stardbi.external"
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
        String header = Base64.getUrlEncoder().withoutPadding().encodeToString("{\"alg\":\"none\",\"typ\":\"JWT\"}".getBytes());
        String payload = Base64.getUrlEncoder().withoutPadding().encodeToString(String.format(
            "{\"user_id\":1,\"username\":\"%s\",\"exp\":%d}",
            "mockuser",
            Instant.now().plusSeconds(3600).getEpochSecond()
        ).getBytes());
        String mockAccessToken = header + "." + payload + ".mock-signature";
        String mockRefreshToken = header + "." + payload + ".mock-refresh-sig";

        return ResponseEntity.ok(Map.of(
                "access", mockAccessToken,
                "refresh", mockRefreshToken,
                "lifetime", 3600
        ));
    }

    @PostMapping("/auth/logout/")
    public ResponseEntity<Void> logout(@RequestBody Map<String, Object> request, @RequestHeader("Authorization") String authHeader) {
        log.info("Mock Stardbi logout");
        return ResponseEntity.ok().build();
    }

    @GetMapping("/swipe_lab/experiments/")
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

    @GetMapping("/swipe_lab/crops/")
    public ResponseEntity<Object> getCrops(@RequestParam("experiment") Long experimentId) {
        log.info("Mock Stardbi get crops for experiment {}", experimentId);
        return ResponseEntity.ok(List.of(
                Map.of("box_id", 101L, "image_id", 201L, "species_id", 301L),
                Map.of("box_id", 102L, "image_id", 201L, "species_id", 301L),
                Map.of("box_id", 103L, "image_id", 202L, "species_id", 302L),
                Map.of("box_id", 104L, "image_id", 202L, "species_id", 302L),
                Map.of("box_id", 105L, "image_id", 203L, "species_id", 303L)
        ));
    }

    @GetMapping("/swipe_lab/crops/{id}/image/")
    public ResponseEntity<Resource> getCropImage(@PathVariable("id") Long id) {
        log.info("Mock Stardbi get crop image {}", id);
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

    @GetMapping("/swipe_lab/taxonomy/")
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

    @PostMapping("/swipe_lab/labels/")
    public ResponseEntity<Object> postLabel(@RequestBody Map<String, Object> label) {
        log.info("Mock Stardbi post label: {}", label);
        return ResponseEntity.ok(Map.of("label_id", 1L));
    }
}
