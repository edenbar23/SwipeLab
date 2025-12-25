package com.swipelab.repository;

import com.swipelab.model.entity.Leaderboard;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeaderboardRepository extends JpaRepository<Leaderboard, Long> {
}
