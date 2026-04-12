package com.swipelab.gamification.challenge;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserChallengeRepository extends JpaRepository<UserChallenge, UUID> {
    
    Optional<UserChallenge> findByUsernameAndDefinitionIdAndWindowStart(
            String username, UUID challengeDefinitionId, LocalDateTime windowStart);
            
    List<UserChallenge> findByUsernameAndWindowStartBetween(
            String username, LocalDateTime start, LocalDateTime end);
            
    List<UserChallenge> findByUsername(String username);
}
