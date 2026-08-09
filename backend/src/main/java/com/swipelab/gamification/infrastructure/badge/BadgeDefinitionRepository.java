package com.swipelab.gamification.infrastructure.badge;

import com.swipelab.gamification.domain.badge.BadgeDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import com.swipelab.gamification.domain.badge.BadgeDefinition;
import org.springframework.stereotype.Repository;

import com.swipelab.gamification.domain.badge.BadgeDefinition;
import java.util.Optional;
import com.swipelab.gamification.domain.badge.BadgeDefinition;
import java.util.UUID;

@Repository
public interface BadgeDefinitionRepository extends JpaRepository<BadgeDefinition, UUID> {

    Optional<BadgeDefinition> findByCode(String code);
}


