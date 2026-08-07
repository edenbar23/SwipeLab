package com.swipelab.classification.infrastructure;

import com.swipelab.classification.domain.image.SpeciesReferenceImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpeciesReferenceImageRepository extends JpaRepository<SpeciesReferenceImage, Long> {

    List<SpeciesReferenceImage> findByLabelId(Long labelId);

    /** Batch-load images for multiple species IDs at once (used in task creation). */
    @Query("SELECT s FROM SpeciesReferenceImage s WHERE s.labelId IN :labelIds ORDER BY s.createdAt ASC")
    List<SpeciesReferenceImage> findByLabelIdIn(@Param("labelIds") List<Long> labelIds);

    long countByLabelId(Long labelId);

    /** Returns IDs of the pool images selected for a given task. */
    @Query(value = """
            SELECT sri.*
            FROM species_reference_images sri
            JOIN task_species_reference_images tsri ON tsri.species_reference_image_id = sri.id
            WHERE tsri.task_id = :taskId
            """, nativeQuery = true)
    List<SpeciesReferenceImage> findByTaskId(@Param("taskId") Long taskId);
}
