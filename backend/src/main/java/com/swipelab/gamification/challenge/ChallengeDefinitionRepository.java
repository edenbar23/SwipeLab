package com.swipelab.gamification.challenge;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ChallengeDefinitionRepository extends JpaRepository<ChallengeDefinition, UUID> {
    
    @Query("SELECT c FROM ChallengeDefinition c WHERE c.active = true " +
           "AND (c.availableFrom IS NULL OR c.availableFrom <= :now) " +
           "AND (c.availableUntil IS NULL OR c.availableUntil >= :now)")
    List<ChallengeDefinition> findActiveChallenges(LocalDateTime now);
    
    List<ChallengeDefinition> findByActiveTrueAndMetricType(MetricType metricType);
}
