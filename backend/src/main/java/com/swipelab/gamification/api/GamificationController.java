package com.swipelab.gamification.api;

import com.swipelab.gamification.dto.GamificationUserInfoResponse;
import com.swipelab.gamification.domain.Gamification;
import com.swipelab.gamification.domain.LeaderboardService;
import com.swipelab.gamification.domain.RankService;
import com.swipelab.gamification.domain.RankTier;
import com.swipelab.gamification.dto.RankResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/gamification")
@PreAuthorize("hasAnyRole('USER', 'RESEARCHER') or @securityAuthorizationService.isSuperAdmin(authentication.name)")
@RequiredArgsConstructor
public class GamificationController {

    private final LeaderboardService leaderboardService;
    private final RankService rankService;

    @GetMapping("/user-info")
    public ResponseEntity<GamificationUserInfoResponse> getUserInfo(
            @AuthenticationPrincipal UserDetails userDetails) {
        Gamification gamification = leaderboardService.getGamification(userDetails.getUsername());

        return ResponseEntity.ok(GamificationUserInfoResponse.builder()
                .score(gamification.getScore())
                .badge(gamification.getBadge())
                .currentStreak(gamification.getCurrentStreak())
                .build());
    }

    @GetMapping("/leaderboard")
    public ResponseEntity<List<Gamification>> getGlobalLeaderboard(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(leaderboardService.getGlobalLeaderboard(limit));
    }

    /**
     * Returns the authenticated user's current rank tier, YES tag count,
     * threshold for the next tier, and progress percentage.
     */
    @GetMapping("/rank")
    public ResponseEntity<RankResponse> getCurrentUserRank(
            @AuthenticationPrincipal UserDetails userDetails) {
        Gamification gamification = leaderboardService.getGamification(userDetails.getUsername());
        int yesTagCount = gamification.getYesTagCount() == null ? 0 : gamification.getYesTagCount();
        RankTier tier = rankService.computeRank(yesTagCount);

        return ResponseEntity.ok(RankResponse.builder()
                .tier(tier.name())
                .yesTagCount(yesTagCount)
                .nextTierAt(rankService.nextTierThreshold(yesTagCount))
                .progressPercent(rankService.progressPercent(yesTagCount))
                .build());
    }
}




