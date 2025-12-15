package com.swipelab.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.swipelab.model.entity.Label;

@Repository
public interface LabelRepository extends JpaRepository<Label, Long> {
    // TODO: Add custom query methods (e.g., findByUser)
}
