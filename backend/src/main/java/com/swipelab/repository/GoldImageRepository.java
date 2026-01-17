package com.swipelab.repository;

import com.swipelab.classification.domain.GoldImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GoldImageRepository extends JpaRepository<GoldImage, Long> {

    List<GoldImage> findByImage_Task_Id(Long taskId);

    Optional<GoldImage> findByImage_Id(Long imageId);

    boolean existsByImage_Id(Long imageId);
}
