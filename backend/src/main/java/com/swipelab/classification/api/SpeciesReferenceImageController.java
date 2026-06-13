package com.swipelab.classification.api;

import com.swipelab.classification.application.SpeciesReferenceImageService;
import com.swipelab.classification.domain.SpeciesReferenceImage;
import com.swipelab.classification.dto.api.SpeciesReferenceImageDto;
import com.swipelab.auth.application.SecurityAuthorizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * REST API for the per-species reference-image pool.
 *
 * <pre>
 * POST   /api/v1/species/{speciesName}/reference-images          – upload 1-3 images
 * GET    /api/v1/species/{speciesName}/reference-images          – list pool for one species
 * GET    /api/v1/species/reference-images?speciesNames=A,B,C     – batch fetch
 * DELETE /api/v1/species/reference-images/{id}               – delete from pool
 * GET    /api/v1/species/reference-images/{id}/image         – stream full image
 * GET    /api/v1/species/reference-images/{id}/thumbnail     – stream thumbnail
 * </pre>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/species")
@RequiredArgsConstructor
public class SpeciesReferenceImageController {

    private final SpeciesReferenceImageService service;
    private final SecurityAuthorizationService securityAuthorizationService;

    // ── Upload ────────────────────────────────────────────────────────────────

    /**
     * Upload 1–3 reference images for a species pool.
     * Images are compressed server-side; no raw bytes are stored.
     */
    @PostMapping(
            value = "/{speciesName}/reference-images",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @PreAuthorize("hasRole('RESEARCHER') or @securityAuthorizationService.isSuperAdmin(authentication.name)")
    public ResponseEntity<List<SpeciesReferenceImageDto>> uploadImages(
            @PathVariable String speciesName,
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam(value = "caption", required = false) String caption,
            @AuthenticationPrincipal UserDetails user) {

        List<SpeciesReferenceImageDto> saved = service.uploadImages(speciesName, files, user.getUsername(), caption);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // ── Read — single species ─────────────────────────────────────────────────

    @GetMapping("/{speciesName}/reference-images")
    @PreAuthorize("hasRole('RESEARCHER') or @securityAuthorizationService.isSuperAdmin(authentication.name)")
    public ResponseEntity<List<SpeciesReferenceImageDto>> getForSpecies(@PathVariable String speciesName) {
        return ResponseEntity.ok(service.getImagesForSpecies(speciesName));
    }

    // ── Read — batch (task creation) ──────────────────────────────────────────

    /**
     * Batch-fetch pool images for multiple species in a single round-trip.
     * Used by StepSpecies UI when the researcher selects several species.
     */
    @GetMapping("/reference-images")
    @PreAuthorize("hasRole('RESEARCHER') or @securityAuthorizationService.isSuperAdmin(authentication.name)")
    public ResponseEntity<Map<String, List<SpeciesReferenceImageDto>>> getBatch(
            @RequestParam List<String> speciesNames) {
        return ResponseEntity.ok(service.getImagesForSpeciesBatch(speciesNames));
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    @DeleteMapping("/reference-images/{id}")
    @PreAuthorize("hasRole('RESEARCHER') or @securityAuthorizationService.isSuperAdmin(authentication.name)")
    public ResponseEntity<Void> deleteImage(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails user) {

        boolean isSuperAdmin = securityAuthorizationService.isSuperAdmin(user.getUsername());
        service.deleteImage(id, user.getUsername(), isSuperAdmin);
        return ResponseEntity.noContent().build();
    }

    // ── Image streaming ───────────────────────────────────────────────────────

    /**
     * Streams the compressed full-resolution image bytes.
     * Authenticated — same pattern as GET /api/admin/gold-images/{id}/image.
     */
    @GetMapping("/reference-images/{id}/image")
    @PreAuthorize("hasRole('RESEARCHER') or hasRole('USER') or @securityAuthorizationService.isSuperAdmin(authentication.name)")
    public ResponseEntity<byte[]> serveImage(@PathVariable Long id) {
        return serveFile(id, false);
    }

    /**
     * Streams the 200px thumbnail.
     * Used by the pool-picker grid in StepSpecies / TaxonomyScreen.
     */
    @GetMapping("/reference-images/{id}/thumbnail")
    @PreAuthorize("hasRole('RESEARCHER') or hasRole('USER') or @securityAuthorizationService.isSuperAdmin(authentication.name)")
    public ResponseEntity<byte[]> serveThumbnail(@PathVariable Long id) {
        return serveFile(id, true);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private ResponseEntity<byte[]> serveFile(Long id, boolean thumbnail) {
        SpeciesReferenceImage entity = service.getEntityById(id);
        String base64Str = thumbnail ? entity.getThumbnailBase64() : entity.getImageBase64();

        if (base64Str == null || base64Str.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        byte[] data = Base64.getDecoder().decode(base64Str);

        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "max-age=86400, public")
                .contentType(MediaType.IMAGE_JPEG)
                .body(data);
    }
}
