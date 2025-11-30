package com.swipelab.repository;

import com.swipelab.model.Image;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ImageRepository extends JpaRepository<Image, Long> {
    // TODO: Add custom query methods (e.g., findNextBatch)
}
