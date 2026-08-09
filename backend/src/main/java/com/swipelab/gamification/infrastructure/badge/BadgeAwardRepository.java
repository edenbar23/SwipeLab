package com.swipelab.gamification.infrastructure.badge;

import com.swipelab.gamification.domain.badge.BadgeAward;
import org.springframework.data.jpa.repository.JpaRepository;
import com.swipelab.gamification.domain.badge.BadgeAward;
import org.springframework.stereotype.Repository;

import com.swipelab.gamification.domain.badge.BadgeAward;
import java.time.LocalDateTime;
import com.swipelab.gamification.domain.badge.BadgeAward;
import java.util.List;
import com.swipelab.gamification.domain.badge.BadgeAward;
import java.util.UUID;

@Repository
public interface BadgeAwardRepository extends JpaRepository<BadgeAward, UUID> {
    
    boolean existsByUsernameAndChallengeDefinitionIdAndWindowStart(
            String username, UUID challengeDefinitionId, LocalDateTime windowStart);
            
    List<BadgeAward> findByUsername(String username);
}


