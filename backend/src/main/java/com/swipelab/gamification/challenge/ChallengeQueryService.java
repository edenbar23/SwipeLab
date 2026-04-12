package com.swipelab.gamification.challenge;

import com.swipelab.gamification.badge.BadgeAward;
import com.swipelab.gamification.badge.BadgeAwardRepository;
import com.swipelab.gamification.badge.BadgeDefinition;
import com.swipelab.gamification.badge.BadgeDefinitionRepository;
import com.swipelab.gamification.dto.BadgeDto;
import com.swipelab.gamification.dto.ChallengeDto;
import com.swipelab.gamification.dto.UserBadgeDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChallengeQueryService {

    private final ChallengeDefinitionRepository challengeDefinitionRepository;
    private final UserChallengeRepository userChallengeRepository;
    private final BadgeAwardRepository badgeAwardRepository;
    private final BadgeDefinitionRepository badgeDefinitionRepository;

    @Transactional(readOnly = true)
    public List<ChallengeDto> getActiveChallengesForUser(String username) {
        LocalDateTime now = LocalDateTime.now();
        List<ChallengeDefinition> activeDefs = challengeDefinitionRepository.findActiveChallenges(now);
        
        // Cache badges for fast lookup
        Map<java.util.UUID, BadgeDefinition> badgeMap = badgeDefinitionRepository.findAll().stream()
                .collect(Collectors.toMap(BadgeDefinition::getId, b -> b));

        // Group user challenges by definitionid, picking the most recent or active one
        // For simplicity, we fetch all and match
        List<UserChallenge> userChallenges = userChallengeRepository.findByUsername(username);

        return activeDefs.stream().map(def -> {
            // Find active user challenge for this def
            UserChallenge currentUserChallenge = userChallenges.stream()
                    .filter(uc -> uc.getDefinition().getId().equals(def.getId()) &&
                            uc.getWindowStart().isBefore(now) &&
                            uc.getWindowEnd().isAfter(now))
                    .findFirst()
                    .orElse(null);

            int progress = currentUserChallenge != null ? currentUserChallenge.getCurrentProgress() : 0;
            boolean completed = currentUserChallenge != null && currentUserChallenge.isCompleted();
            LocalDateTime start = currentUserChallenge != null ? currentUserChallenge.getWindowStart() : null;
            LocalDateTime end = currentUserChallenge != null ? currentUserChallenge.getWindowEnd() : null;
            
            BadgeDefinition badgeDef = badgeMap.get(def.getBadgeId());

            return ChallengeDto.builder()
                    .challengeId(def.getId())
                    .name(def.getName())
                    .description(def.getDescription())
                    .progress(progress)
                    .target(def.getTargetValue())
                    .completed(completed)
                    .windowStart(start)
                    .windowEnd(end)
                    .badge(badgeDef != null ? BadgeDto.builder()
                            .title(badgeDef.getTitle())
                            .iconUrl(badgeDef.getIconUrl())
                            .build() : null)
                    .build();
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<UserBadgeDto> getUserBadges(String username) {
        List<BadgeAward> awards = badgeAwardRepository.findByUsername(username);
        Map<java.util.UUID, BadgeDefinition> badgeMap = badgeDefinitionRepository.findAll().stream()
                .collect(Collectors.toMap(BadgeDefinition::getId, b -> b));

        return awards.stream().map(award -> {
            BadgeDefinition def = badgeMap.get(award.getBadgeId());
            return UserBadgeDto.builder()
                    .title(def != null ? def.getTitle() : "Unknown")
                    .description(def != null ? def.getDescription() : "")
                    .iconUrl(def != null ? def.getIconUrl() : "")
                    .earnedAt(award.getAwardedAt())
                    .build();
        }).collect(Collectors.toList());
    }
}
