package com.swipelab.classification.infrastructure;

import com.swipelab.classification.domain.Classification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClassificationRepository extends JpaRepository<Classification, Long> {

    /**
     * Find all classifications by a specific user
     */
    List<Classification> findByUsername(String username);

    /**
     * Find all classifications for a specific image
     */
    List<Classification> findByImageId(Long imageId);

    /**
     * Check if a user has already classified a specific image
     */
    boolean existsByUsernameAndImageId(String username, Long imageId);

    /**
     * Find all classifications made by experts (RESEARCHER role)
     * Used for calculating Cohen's Kappa with regular users
     */
    @Query("SELECT c FROM Classification c WHERE c.userRole = 'RESEARCHER'")
    List<Classification> findExpertClassifications();

    /**
     * Find all expert classifications for a specific image
     * Used when an expert classifies and we need to recalculate user credibility
     */
    @Query("SELECT c FROM Classification c WHERE c.image.id = :imageId AND c.userRole = 'RESEARCHER'")
    List<Classification> findExpertClassificationsByImageId(@Param("imageId") Long imageId);

    /**
     * Find all non-expert (regular user) classifications for a specific image
     * Used when an expert classifies and we need to update all regular users who
     * classified this image
     */
    @Query("SELECT c FROM Classification c WHERE c.image.id = :imageId AND c.userRole != 'RESEARCHER'")
    List<Classification> findNonExpertClassificationsByImageId(@Param("imageId") Long imageId);

    /**
     * Count total classifications by a user
     */
    @Query("SELECT COUNT(c) FROM Classification c WHERE c.username = :username")
    Long countByUsername(@Param("username") String username);

    /**
     * Find all classifications by a user for images also classified by experts
     * Useful for getting the overlap between user and expert classifications
     */
    @Query("SELECT c FROM Classification c WHERE c.username = :username " +
            "AND EXISTS (SELECT ec FROM Classification ec WHERE ec.image.id = c.image.id AND ec.userRole = 'RESEARCHER')")
    List<Classification> findUserClassificationsWithExpertOverlap(@Param("username") String username);

    /**
     * Count total classifications for a specific image
     */
    long countByImage_Id(Long imageId);

    @Query("SELECT COUNT(c) FROM Classification c WHERE c.image.id = :imageId")
    long countByImageId(@Param("imageId") Long imageId);

    /**
     * Count total classifications by a user for a specific task
     */
    @Query("SELECT COUNT(c) FROM Classification c WHERE c.username = :username AND c.taskId = :taskId")
    Long countByUsernameAndTaskId(@Param("username") String username, @Param("taskId") Long taskId);

    /**
     * Check if a user has already classified a specific image for a specific species.
     * This allows the same image to appear again if a different species is queried.
     */
    boolean existsByUsernameAndImage_IdAndQuerySpecies(String username, Long imageId, String querySpecies);

    /**
     * Get all species already queried for a given image by a user.
     */
    @Query("SELECT c.querySpecies FROM Classification c WHERE c.username = :username AND c.image.id = :imageId")
    List<String> findQueriedSpeciesByUsernameAndImageId(@Param("username") String username, @Param("imageId") Long imageId);
}