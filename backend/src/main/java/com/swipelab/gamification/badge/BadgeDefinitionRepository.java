package com.swipelab.gamification.badge;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface BadgeDefinitionRepository extends JpaRepository<BadgeDefinition, UUID> {
}
