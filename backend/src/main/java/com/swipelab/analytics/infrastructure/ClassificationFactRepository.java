package com.swipelab.analytics.infrastructure;

import com.swipelab.analytics.domain.ClassificationFact;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ClassificationFactRepository extends JpaRepository<ClassificationFact, UUID> {

    // ─── Existing user-scoped queries ────────────────────────────────────────

    @Query("SELECT AVG(CASE WHEN c.isCorrect = true THEN 1.0 ELSE 0.0 END) FROM ClassificationFact c WHERE c.userId = :userId AND c.isCorrect IS NOT NULL")
    Double getUserAccuracy(@Param("userId") String userId);

    @Query("SELECT AVG(CASE WHEN c.isCorrect = true THEN 1.0 ELSE 0.0 END) FROM ClassificationFact c WHERE c.isExpert = true AND c.isCorrect IS NOT NULL")
    Double getGlobalExpertAccuracy();

    @Query("SELECT AVG(CASE WHEN c.isCorrect = true THEN 1.0 ELSE 0.0 END) FROM ClassificationFact c WHERE c.isCorrect IS NOT NULL")
    Double getGlobalAverageAccuracy();

    @Query("SELECT COUNT(DISTINCT c.imageId) FROM ClassificationFact c WHERE c.taskId = :taskId")
    Long countCompletedImages(@Param("taskId") Long taskId);

    @Query("SELECT c.species, COUNT(c), AVG(CASE WHEN c.isCorrect = true THEN 1.0 ELSE 0.0 END) " +
            "FROM ClassificationFact c WHERE c.userId = :userId AND c.isCorrect IS NOT NULL GROUP BY c.species")
    List<Object[]> getSpeciesBreakdown(@Param("userId") String userId);

    List<ClassificationFact> findByTaskId(Long taskId);

    // ─── Platform-wide time-windowed aggregate queries ────────────────────────

    @Query("SELECT COUNT(c) FROM ClassificationFact c WHERE c.day >= :since")
    long countClassificationsSince(@Param("since") LocalDate since);

    @Query("SELECT COUNT(DISTINCT c.imageId) FROM ClassificationFact c WHERE c.day >= :since")
    long countDistinctImagesSince(@Param("since") LocalDate since);

    @Query("SELECT COUNT(DISTINCT c.userId) FROM ClassificationFact c WHERE c.day >= :since")
    long countDistinctUsersSince(@Param("since") LocalDate since);

    @Query("SELECT COUNT(DISTINCT c.taskId) FROM ClassificationFact c WHERE c.day >= :since")
    long countDistinctTasksSince(@Param("since") LocalDate since);

    /**
     * Counts distinct STARDBI experiment IDs referenced by tasks that had
     * at least one classification in the given window.
     * Uses a native join through the task_experiments join table.
     */
    @Query(value = "SELECT COUNT(DISTINCT te.experiment_id) " +
            "FROM classification_facts cf " +
            "JOIN task_experiments te ON cf.task_id = te.task_id " +
            "WHERE cf.day >= :since",
            nativeQuery = true)
    long countDistinctExperimentsSince(@Param("since") LocalDate since);

    /**
     * Returns (day, avgCredibility, count) for each day since :since,
     * ordered ascending — used for the platform confidence trend chart.
     */
    @Query("SELECT c.day, AVG(c.credibilityAtTime), COUNT(c) " +
            "FROM ClassificationFact c " +
            "WHERE c.day >= :since AND c.credibilityAtTime IS NOT NULL " +
            "GROUP BY c.day ORDER BY c.day ASC")
    List<Object[]> getDailyCredibilityTrend(@Param("since") LocalDate since);

    // ─── User-performance aggregation (for admin endpoints) ──────────────────

    /**
     * Returns (userId, totalCount, correctCount, avgCredibility) per user.
     * When taskId is null all tasks are included.
     */
    @Query("SELECT c.userId, COUNT(c), " +
            "SUM(CASE WHEN c.isCorrect = true THEN 1 ELSE 0 END), " +
            "SUM(CASE WHEN c.isCorrect IS NOT NULL THEN 1 ELSE 0 END), " +
            "AVG(c.credibilityAtTime) " +
            "FROM ClassificationFact c " +
            "WHERE (:taskId IS NULL OR c.taskId = :taskId) " +
            "GROUP BY c.userId")
    List<Object[]> getUserPerformanceAggregation(@Param("taskId") Long taskId);

    /**
     * Same aggregation ordered by total DESC — used by the top-performers endpoint.
     */
    @Query("SELECT c.userId, COUNT(c) as total, " +
            "SUM(CASE WHEN c.isCorrect = true THEN 1 ELSE 0 END), " +
            "SUM(CASE WHEN c.isCorrect IS NOT NULL THEN 1 ELSE 0 END), " +
            "AVG(c.credibilityAtTime) " +
            "FROM ClassificationFact c " +
            "GROUP BY c.userId ORDER BY total DESC")
    List<Object[]> getTopPerformersAggregation(Pageable pageable);
}
