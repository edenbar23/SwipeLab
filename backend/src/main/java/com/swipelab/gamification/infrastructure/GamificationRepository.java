package com.swipelab.gamification.infrastructure;

import com.swipelab.gamification.domain.Gamification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GamificationRepository extends JpaRepository<Gamification, String> {
}
