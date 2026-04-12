package com.swipelab.gamification.api;

import com.swipelab.gamification.challenge.ChallengeQueryService;
import com.swipelab.gamification.dto.ChallengeDto;
import com.swipelab.gamification.dto.UserBadgeDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.security.Principal;

@RestController
@RequestMapping("/api/v1/gamification")
@RequiredArgsConstructor
public class ChallengesController {

    private final ChallengeQueryService challengeQueryService;

    @GetMapping("/challenges")
    public ResponseEntity<List<ChallengeDto>> getActiveChallenges(Principal principal) {
        String username = extractUsername(principal);
        List<ChallengeDto> challenges = challengeQueryService.getActiveChallengesForUser(username);
        return ResponseEntity.ok(challenges);
    }

    @GetMapping("/me/badges")
    public ResponseEntity<List<UserBadgeDto>> getMyBadges(Principal principal) {
        String username = extractUsername(principal);
        List<UserBadgeDto> badges = challengeQueryService.getUserBadges(username);
        return ResponseEntity.ok(badges);
    }

    private String extractUsername(Principal principal) {
        if (principal == null) {
            return "anonymous"; // or throw Unauthorized exception depending on filter
        }
        return principal.getName();
    }
}
