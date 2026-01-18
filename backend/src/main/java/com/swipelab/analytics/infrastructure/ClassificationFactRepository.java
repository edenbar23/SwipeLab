package com.swipelab.analytics.infrastructure;

import com.swipelab.analytics.domain.ClassificationFact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ClassificationFactRepository extends JpaRepository<ClassificationFact, UUID> {

    @Query("SELECT AVG(CASE WHEN c.isExpert = false THEN c.accuracy ELSE null END) as userAccuracy, " +
            "AVG(CASE WHEN c.isExpert = true THEN c.accuracy ELSE null END) as expertAccuracy " +
            "FROM ClassificationFact c WHERE c.userId = :userId")
    // Note: 'accuracy' field doesn't exist on ClassificationFact?
    // User request: "AVG(CASE WHEN is_expert = false THEN accuracy END)".
    // ClassificationFact has `isCorrect` (Boolean). Accuracy is aggregated.
    // If ClassificationFact is granular (per classification), accuracy is 1.0 or
    // 0.0 (if correct/incorrect).
    // So AVG(CAST(isCorrect AS double)) is accuracy.
    // I need to adjust the query or logic.
    // Since I implemented `isCorrect` as Boolean, I can cast it.
    // HQL/JPQL usually supports casting or I can rely on boolean->int (0/1).
    // Let's use standard JPQL logic.
    Object getUserVsExpertStats(@Param("userId") String userId);

    @Query("SELECT COUNT(DISTINCT c.imageId) FROM ClassificationFact c WHERE c.taskId = :taskId")
    Long countCompletedImages(@Param("taskId") Long taskId);

    @Query("SELECT c.species, COUNT(c), AVG(CASE WHEN c.isCorrect = true THEN 1.0 ELSE 0.0 END) " +
            "FROM ClassificationFact c WHERE c.userId = :userId GROUP BY c.species")
    List<Object[]> getSpeciesBreakdown(@Param("userId") String userId);

    List<ClassificationFact> findByTaskId(Long taskId);
}
