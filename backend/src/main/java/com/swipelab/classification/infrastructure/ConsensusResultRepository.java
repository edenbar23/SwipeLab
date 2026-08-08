package com.swipelab.classification.infrastructure;

import com.swipelab.classification.domain.threshold.ConsensusResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConsensusResultRepository extends JpaRepository<ConsensusResult, Long> {
    
    @Query("SELECT c.species FROM ConsensusResult c WHERE c.imageId = :imageId")
    List<String> findCompletedSpeciesByImageId(@Param("imageId") Long imageId);

    Optional<ConsensusResult> findByImageIdAndSpecies(Long imageId, String species);
}
