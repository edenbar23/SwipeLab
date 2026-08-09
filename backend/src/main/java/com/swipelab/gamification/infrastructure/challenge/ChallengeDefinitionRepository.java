package com.swipelab.gamification.infrastructure.challenge;

import com.swipelab.gamification.domain.challenge.ChallengeDefinition;
import com.swipelab.gamification.domain.challenge.MetricType;
import org.springframework.data.jpa.repository.JpaRepository;
import com.swipelab.gamification.domain.challenge.ChallengeDefinition;
import com.swipelab.gamification.domain.challenge.MetricType;
import org.springframework.data.jpa.repository.Query;
import com.swipelab.gamification.domain.challenge.ChallengeDefinition;
import com.swipelab.gamification.domain.challenge.MetricType;
import org.springframework.stereotype.Repository;

import com.swipelab.gamification.domain.challenge.ChallengeDefinition;
import com.swipelab.gamification.domain.challenge.MetricType;
import java.time.LocalDateTime;
import com.swipelab.gamification.domain.challenge.ChallengeDefinition;
import com.swipelab.gamification.domain.challenge.MetricType;
import java.util.List;
import com.swipelab.gamification.domain.challenge.ChallengeDefinition;
import com.swipelab.gamification.domain.challenge.MetricType;
import java.util.Optional;
import com.swipelab.gamification.domain.challenge.ChallengeDefinition;
import com.swipelab.gamification.domain.challenge.MetricType;
import java.util.UUID;

@Repository
public interface ChallengeDefinitionRepository extends JpaRepository<ChallengeDefinition, UUID> {

    @Query("SELECT c FROM ChallengeDefinition c WHERE c.active = true " +
           "AND (c.availableFrom IS NULL OR c.availableFrom <= :now) " +
           "AND (c.availableUntil IS NULL OR c.availableUntil >= :now)")
    List<ChallengeDefinition> findActiveChallenges(LocalDateTime now);

    List<ChallengeDefinition> findByActiveTrueAndMetricType(MetricType metricType);

    Optional<ChallengeDefinition> findByBadgeId(UUID badgeId);
}


