package com.swipelab.gamification.infrastructure.challenge;

import com.swipelab.gamification.domain.challenge.UserChallenge;
import org.springframework.data.jpa.repository.JpaRepository;
import com.swipelab.gamification.domain.challenge.UserChallenge;
import org.springframework.stereotype.Repository;

import com.swipelab.gamification.domain.challenge.UserChallenge;
import java.time.LocalDateTime;
import com.swipelab.gamification.domain.challenge.UserChallenge;
import java.util.List;
import com.swipelab.gamification.domain.challenge.UserChallenge;
import java.util.Optional;
import com.swipelab.gamification.domain.challenge.UserChallenge;
import java.util.UUID;

@Repository
public interface UserChallengeRepository extends JpaRepository<UserChallenge, UUID> {
    
    Optional<UserChallenge> findByUsernameAndDefinitionIdAndWindowStart(
            String username, UUID challengeDefinitionId, LocalDateTime windowStart);
            
    List<UserChallenge> findByUsernameAndWindowStartBetween(
            String username, LocalDateTime start, LocalDateTime end);
            
    List<UserChallenge> findByUsername(String username);
}


