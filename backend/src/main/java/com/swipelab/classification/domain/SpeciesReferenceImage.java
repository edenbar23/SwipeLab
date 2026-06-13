package com.swipelab.classification.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * One entry in the per-species reference-image pool.
 * Images are compressed (max 1920px, JPEG q=0.8) and a 200px thumbnail
 * is generated at upload time by ImageProcessingService.
 */
@Entity
@Table(name = "species_reference_images")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpeciesReferenceImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** FK to labels.id – the species this image belongs to. */
    @Column(name = "label_id", nullable = false)
    private Long labelId;

    /** Compressed full image as Base64 string */
    @Column(name = "image_base64", nullable = false, columnDefinition = "TEXT")
    private String imageBase64;

    /** 200px thumbnail as Base64 string */
    @Column(name = "thumbnail_base64", nullable = false, columnDefinition = "TEXT")
    private String thumbnailBase64;

    /** File size in bytes after compression (informational). */
    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(length = 255)
    private String caption;

    /** Researcher username who uploaded this image. */
    @Column(name = "uploaded_by", nullable = false)
    private String uploadedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
