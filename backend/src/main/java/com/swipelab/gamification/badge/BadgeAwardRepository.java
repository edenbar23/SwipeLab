package com.swipelab.gamification.badge;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface BadgeAwardRepository extends JpaRepository<BadgeAward, UUID> {
    
    boolean existsByUsernameAndChallengeDefinitionIdAndWindowStart(
            String username, UUID challengeDefinitionId, LocalDateTime windowStart);
            
    List<BadgeAward> findByUsername(String username);
}
