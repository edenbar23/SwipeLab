package com.swipelab.gamification.application.badge;

import com.swipelab.gamification.domain.badge.*;
import com.swipelab.gamification.infrastructure.badge.*;
import com.swipelab.gamification.domain.challenge.UserChallenge;
import com.swipelab.gamification.domain.badge.*;
import com.swipelab.gamification.infrastructure.badge.*;
import lombok.RequiredArgsConstructor;
import com.swipelab.gamification.domain.badge.*;
import com.swipelab.gamification.infrastructure.badge.*;
import lombok.extern.slf4j.Slf4j;
import com.swipelab.gamification.domain.badge.*;
import com.swipelab.gamification.infrastructure.badge.*;
import org.springframework.stereotype.Service;
import com.swipelab.gamification.domain.badge.*;
import com.swipelab.gamification.infrastructure.badge.*;
import org.springframework.transaction.annotation.Transactional;

import com.swipelab.gamification.domain.badge.*;
import com.swipelab.gamification.infrastructure.badge.*;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class RewardService {

    private final BadgeAwardRepository badgeAwardRepository;

    @Transactional
    public void grantBadge(UserChallenge challenge) {
        boolean alreadyAwarded = badgeAwardRepository.existsByUsernameAndChallengeDefinitionIdAndWindowStart(
                challenge.getUsername(),
                challenge.getDefinition().getId(),
                challenge.getWindowStart()
        );

        if (alreadyAwarded) {
            log.debug("Badge already awarded for user {} in this window for challenge {}", 
                    challenge.getUsername(), challenge.getDefinition().getName());
            return;
        }

        BadgeAward award = BadgeAward.builder()
                .username(challenge.getUsername())
                .badgeId(challenge.getDefinition().getBadgeId())
                .challengeDefinitionId(challenge.getDefinition().getId())
                .windowStart(challenge.getWindowStart())
                .windowEnd(challenge.getWindowEnd())
                .awardedAt(LocalDateTime.now())
                .build();

        badgeAwardRepository.save(award);
        log.info("Granted badge ID {} to user {} for challenge {}", 
                award.getBadgeId(), challenge.getUsername(), challenge.getDefinition().getName());
                
        // Optional: Publish a BadgeEarnedEvent here for frontend toast notifications.
    }
}



