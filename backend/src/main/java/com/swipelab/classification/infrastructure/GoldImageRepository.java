package com.swipelab.classification.infrastructure;

import com.swipelab.classification.domain.GoldImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GoldImageRepository extends JpaRepository<GoldImage, Long> {
    Optional<GoldImage> findByImageId(Long imageId);

    boolean existsByImageId(Long imageId);

    /**
     * Find all gold images for a specific task.
     * Uses database query instead of findAll() + filter for better performance.
     */
    @Query("SELECT g FROM GoldImage g WHERE g.image.task.id = :taskId")
    List<GoldImage> findByImageTaskId(@Param("taskId") Long taskId);
}
