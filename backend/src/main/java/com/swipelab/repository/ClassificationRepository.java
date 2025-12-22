package com.swipelab.repository;

import com.swipelab.model.entity.Classification;
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
    List<Classification> findByUser_Username(String username);

    /**
     * Find all classifications for a specific image
     */
    List<Classification> findByImageId(Long imageId);

    /**
     * Check if a user has already classified a specific image
     */
    boolean existsByUser_UsernameAndImage_Id(String username, Long imageId);

    /**
     * Find all classifications made by experts (RESEARCHER role)
     * Used for calculating Cohen's Kappa with regular users
     */
    @Query("SELECT c FROM Classification c WHERE c.user.role = 'RESEARCHER'")
    List<Classification> findExpertClassifications();

    /**
     * Find all expert classifications for a specific image
     * Used when an expert classifies and we need to recalculate user credibility
     */
    @Query("SELECT c FROM Classification c WHERE c.image.id = :imageId AND c.user.role = 'RESEARCHER'")
    List<Classification> findExpertClassificationsByImageId(@Param("imageId") Long imageId);

    /**
     * Find all non-expert (regular user) classifications for a specific image
     * Used when an expert classifies and we need to update all regular users who classified this image
     */
    @Query("SELECT c FROM Classification c WHERE c.image.id = :imageId AND c.user.role != 'RESEARCHER'")
    List<Classification> findNonExpertClassificationsByImageId(@Param("imageId") Long imageId);

    /**
     * Count total classifications by a user
     */
    @Query("SELECT COUNT(c) FROM Classification c WHERE c.user.username = :username")
    Long countByUsername(@Param("username") String username);

    /**
     * Find all classifications by a user for images also classified by experts
     * Useful for getting the overlap between user and expert classifications
     */
    @Query("SELECT c FROM Classification c WHERE c.user.username = :username " +
            "AND EXISTS (SELECT ec FROM Classification ec WHERE ec.image.id = c.image.id AND ec.user.role = 'RESEARCHER')")
    List<Classification> findUserClassificationsWithExpertOverlap(@Param("username") String username);
}