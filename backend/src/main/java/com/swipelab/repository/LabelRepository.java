package com.swipelab.repository;

import com.swipelab.model.Label;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LabelRepository extends JpaRepository<Label, Long> {
    // TODO: Add custom query methods (e.g., findByUser)
}
