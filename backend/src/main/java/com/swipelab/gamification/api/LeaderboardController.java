package com.swipelab.gamification.api;

import com.swipelab.gamification.domain.Gamification;
import com.swipelab.gamification.domain.LeaderboardService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/leaderboard")
@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
@RequiredArgsConstructor
public class LeaderboardController {

    private final LeaderboardService leaderboardService;

    @GetMapping("/global")
    public ResponseEntity<List<Gamification>> getGlobalLeaderboard(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(leaderboardService.getGlobalLeaderboard(limit));
    }
}
