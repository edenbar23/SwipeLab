package com.swipelab.classification.infrastructure;

import com.swipelab.classification.domain.Image;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ImageRepository extends JpaRepository<Image, Long> {

    List<Image> findByTaskId(Long taskId);

    boolean existsByExternalBoxId(Long externalBoxId);
    Image findByExternalBoxId(Long externalBoxId);

    // Find all unclassified gold standard images for a specific task and user
    @Query("SELECT i FROM Image i WHERE i.task.id = :taskId " +
            "AND i.id IN (SELECT g.image.id FROM GoldImage g) " +
            "AND NOT EXISTS (SELECT c FROM Classification c WHERE c.image.id = i.id AND c.username = :username)")
    List<Image> findUnclassifiedGoldImages(@Param("username") String username, @Param("taskId") Long taskId);

    /**
     * Find regular (non-gold) images for a task where the user still has at least one
     * unqueried species left. An image is included when the count of species the user
     * has already classified for it is less than the total species count in the task.
     *
     * Prioritises images with fewer total classifications overall.
     */
    @Query("SELECT i FROM Image i " +
            "WHERE i.task.id = :taskId " +
            "AND i.id NOT IN (SELECT g.image.id FROM GoldImage g) " +
            "AND (SELECT COUNT(DISTINCT c.querySpecies) FROM Classification c " +
            "     WHERE c.image.id = i.id AND c.username = :username) < :speciesCount " +
            "GROUP BY i.id " +
            "ORDER BY i.priority DESC, " +
            "(SELECT COUNT(c2.id) FROM Classification c2 WHERE c2.image.id = i.id) ASC")
    List<Image> findRegularImageCandidatesForUser(
            @Param("username") String username,
            @Param("taskId") Long taskId,
            @Param("speciesCount") int speciesCount,
            Pageable pageable);
}

