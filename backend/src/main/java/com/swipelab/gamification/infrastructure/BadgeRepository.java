package com.swipelab.gamification.infrastructure;

import com.swipelab.gamification.domain.Badge;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BadgeRepository extends JpaRepository<Badge, Long> {
    java.util.Optional<Badge> findByName(String name);
}
