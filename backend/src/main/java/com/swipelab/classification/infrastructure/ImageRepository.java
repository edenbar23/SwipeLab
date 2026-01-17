package com.swipelab.classification.infrastructure;

import com.swipelab.classification.domain.Image;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ImageRepository extends JpaRepository<Image, Long> {
    List<Image> findByTaskId(Long taskId);

    List<Image> findByIsGoldStandardTrue();

    // Find gold standard images for a specific task
    List<Image> findByTaskIdAndIsGoldStandardTrue(Long taskId);

    // Find regular (non-gold) images for a specific task
    List<Image> findByTaskIdAndIsGoldStandardFalse(Long taskId);
}
