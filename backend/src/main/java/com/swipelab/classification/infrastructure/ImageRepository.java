package com.swipelab.classification.infrastructure;

import com.swipelab.classification.domain.Image;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ImageRepository extends JpaRepository<Image, Long> {

    List<Image> findByTaskId(Long taskId);

    // Find gold standard images for a specific task
    // Since Image doesn't have isGoldStandard, we must join or use IN clause with
    // GoldImage entity
    @Query("SELECT i FROM Image i WHERE i.task.id = :taskId AND i.id IN (SELECT g.image.id FROM GoldImage g)")
    List<Image> findGoldStandardImagesByTaskId(@Param("taskId") Long taskId);

    // Find regular (non-gold) images for a specific task
    @Query("SELECT i FROM Image i WHERE i.task.id = :taskId AND i.id NOT IN (SELECT g.image.id FROM GoldImage g)")
    List<Image> findRegularImagesByTaskId(@Param("taskId") Long taskId);
}
