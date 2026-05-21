package com.swipelab.classification.infrastructure;

import com.swipelab.classification.domain.GoldImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GoldImageRepository extends JpaRepository<GoldImage, Long> {
    Optional<GoldImage> findByImageId(Long imageId);

    // Used during classification to skip deactivated gold images
    Optional<GoldImage> findByImageIdAndActiveTrue(Long imageId);

    List<GoldImage> findAllByActiveTrue();

    boolean existsByImageId(Long imageId);
}
