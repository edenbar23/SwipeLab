package com.swipelab.gamification.infrastructure;

import com.swipelab.gamification.domain.Leaderboard;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeaderboardRepository extends JpaRepository<Leaderboard, Long> {

}
