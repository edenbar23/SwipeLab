package com.swipelab.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.swipelab.model.entity.Image;

@Repository
public interface ImageRepository extends JpaRepository<Image, Long> {
    // TODO: Add custom query methods (e.g., findNextBatch)
}
