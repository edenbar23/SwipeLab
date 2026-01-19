package com.swipelab.analytics.infrastructure;

import com.swipelab.analytics.domain.ClassificationFact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ClassificationFactRepository extends JpaRepository<ClassificationFact, UUID> {

        // Calculate accuracy for a specific user: AVG of (1.0 if correct else 0.0)
        @Query("SELECT AVG(CASE WHEN c.isCorrect = true THEN 1.0 ELSE 0.0 END) FROM ClassificationFact c WHERE c.userId = :userId")
        Double getUserAccuracy(@Param("userId") String userId);

        // Calculate global expert accuracy: AVG of (1.0 if correct else 0.0) where
        // isExpert is true
        @Query("SELECT AVG(CASE WHEN c.isCorrect = true THEN 1.0 ELSE 0.0 END) FROM ClassificationFact c WHERE c.isExpert = true")
        Double getGlobalExpertAccuracy();

        @Query("SELECT COUNT(DISTINCT c.imageId) FROM ClassificationFact c WHERE c.taskId = :taskId")
        Long countCompletedImages(@Param("taskId") Long taskId);

        @Query("SELECT c.species, COUNT(c), AVG(CASE WHEN c.isCorrect = true THEN 1.0 ELSE 0.0 END) " +
                        "FROM ClassificationFact c WHERE c.userId = :userId GROUP BY c.species")
        List<Object[]> getSpeciesBreakdown(@Param("userId") String userId);

        List<ClassificationFact> findByTaskId(Long taskId);
}
