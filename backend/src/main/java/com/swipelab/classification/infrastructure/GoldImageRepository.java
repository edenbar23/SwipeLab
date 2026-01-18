package com.swipelab.classification.infrastructure;

import com.swipelab.classification.domain.GoldImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GoldImageRepository extends JpaRepository<GoldImage, Long> {
    Optional<GoldImage> findByImageId(Long imageId);

    boolean existsByImageId(Long imageId);
}
