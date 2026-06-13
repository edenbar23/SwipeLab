package com.swipelab.classification.application;

import com.swipelab.classification.domain.Label;
import com.swipelab.classification.domain.SpeciesReferenceImage;
import com.swipelab.classification.dto.api.SpeciesReferenceImageDto;
import com.swipelab.classification.infrastructure.LabelRepository;
import com.swipelab.classification.infrastructure.SpeciesReferenceImageRepository;
import com.swipelab.exception.ResourceNotFoundException;
import com.swipelab.infrastructure.ImageProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Manages the per-species reference-image pool.
 * <p>
 * Upload strategy: <strong>deferred</strong> — images are NOT uploaded during task
 * creation UI interaction; they are uploaded in bulk when the researcher submits
 * the task form (handled by the caller).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SpeciesReferenceImageService {

    private static final int MAX_POOL_SIZE = 10;

    private final SpeciesReferenceImageRepository repository;
    private final LabelRepository                  labelRepository;
    private final ImageProcessingService           imageProcessingService;

    // ── Upload ────────────────────────────────────────────────────────────────

    /**
     * Processes and stores up to 3 images for a species pool entry.
     * Enforces the per-species pool cap ({@value MAX_POOL_SIZE}).
     *
     * @param labelId   the species label ID
     * @param files     1–3 image files
     * @param username  authenticated researcher's username
     * @param caption   optional caption (applied to all files in this batch)
     * @return saved DTOs (one per file)
     */
    @Transactional
    public List<SpeciesReferenceImageDto> uploadImages(
            String speciesName,
            List<MultipartFile> files,
            String username,
            String caption) {

        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("At least one image file is required.");
        }
        if (files.size() > 3) {
            throw new IllegalArgumentException("Maximum 3 images per upload batch.");
        }

        Long labelId = labelRepository.findByName(speciesName)
                .orElseGet(() -> labelRepository.save(Label.builder().name(speciesName).build()))
                .getId();

        long existingCount = repository.countByLabelId(labelId);
        if (existingCount + files.size() > MAX_POOL_SIZE) {
            throw new IllegalArgumentException(
                    "Pool for this species already has " + existingCount +
                    " images. Max pool size is " + MAX_POOL_SIZE + ".");
        }

        return files.stream().map(file -> {
            try {
                ImageProcessingService.ProcessedImageResult result =
                        imageProcessingService.processAndStore(file);

                SpeciesReferenceImage entity = SpeciesReferenceImage.builder()
                        .labelId(labelId)
                        .imageBase64(result.imageBase64())
                        .thumbnailBase64(result.thumbnailBase64())
                        .fileSizeBytes(result.fileSizeBytes())
                        .caption(caption)
                        .uploadedBy(username)
                        .build();

                return toDto(repository.save(entity));
            } catch (IOException e) {
                throw new RuntimeException("Failed to process image: " + file.getOriginalFilename(), e);
            }
        }).collect(Collectors.toList());
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    /** Returns all pool images for a single species, ordered by upload date. */
    @Transactional(readOnly = true)
    public List<SpeciesReferenceImageDto> getImagesForSpecies(String speciesName) {
        return labelRepository.findByName(speciesName)
                .map(label -> repository.findByLabelId(label.getId()).stream()
                        .map(this::toDto)
                        .collect(Collectors.toList()))
                .orElse(List.of());
    }

    /**
     * Batch-loads images for multiple species in a single query.
     *
     * @return map of speciesName → list of DTOs
     */
    @Transactional(readOnly = true)
    public Map<String, List<SpeciesReferenceImageDto>> getImagesForSpeciesBatch(List<String> speciesNames) {
        if (speciesNames == null || speciesNames.isEmpty()) {
            return Map.of();
        }

        List<Label> labels = labelRepository.findByNameIn(speciesNames);
        if (labels.isEmpty()) {
            return Map.of();
        }

        Map<Long, String> labelIdToName = labels.stream()
                .collect(Collectors.toMap(Label::getId, Label::getName));

        List<Long> labelIds = labels.stream().map(Label::getId).collect(Collectors.toList());

        return repository.findByLabelIdIn(labelIds).stream()
                .collect(Collectors.groupingBy(
                        image -> labelIdToName.get(image.getLabelId()),
                        Collectors.mapping(this::toDto, Collectors.toList())
                ));
    }

    /**
     * Returns the raw entity for file-serving (used by controller byte-stream endpoints).
     */
    @Transactional(readOnly = true)
    public SpeciesReferenceImage getEntityById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reference image not found: " + id));
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    /**
     * Deletes a pool image. Only the uploader or a super-admin may delete.
     * Disk files are removed best-effort (logged on failure, not thrown).
     */
    @Transactional
    public void deleteImage(Long id, String username, boolean isSuperAdmin) {
        SpeciesReferenceImage image = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reference image not found: " + id));

        if (!isSuperAdmin && !image.getUploadedBy().equals(username)) {
            throw new AccessDeniedException("You can only delete images you uploaded.");
        }

        repository.delete(image);
        log.info("Deleted reference image id={} by username={}", id, username);
    }

    // ── Mapping ───────────────────────────────────────────────────────────────

    private SpeciesReferenceImageDto toDto(SpeciesReferenceImage e) {
        return SpeciesReferenceImageDto.builder()
                .id(e.getId())
                .labelId(e.getLabelId())
                // Serve via authenticated streaming endpoints
                .imageUrl("/api/v1/species/reference-images/" + e.getId() + "/image")
                .thumbnailUrl("/api/v1/species/reference-images/" + e.getId() + "/thumbnail")
                .fileSizeBytes(e.getFileSizeBytes())
                .caption(e.getCaption())
                .uploadedBy(e.getUploadedBy())
                .createdAt(e.getCreatedAt())
                .build();
    }

}
