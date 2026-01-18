package com.swipelab.classification.infrastructure;

import com.swipelab.classification.domain.CredibilityRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CredibilityRepository extends JpaRepository<CredibilityRecord, Long> {
    List<CredibilityRecord> findByUsername(String username);

    Optional<CredibilityRecord> findByUsernameAndGoldImageId(String username, Long goldImageId);

    // FIX: Using goldImage.image.id to check because the service checks against
    // generic Image ID
    boolean existsByUsernameAndGoldImage_Image_Id(String username, Long imageId);

    // Alias if service uses "Image_Id" directly but assuming relation via GoldImage
    default boolean existsByUsernameAndImageId(String username, Long imageId) {
        return existsByUsernameAndGoldImage_Image_Id(username, imageId);
    }
}
