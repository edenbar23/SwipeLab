package com.swipelab.repository;

import com.swipelab.model.entity.Classification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClassificationRepository extends JpaRepository<Classification, Long> {
    List<Classification> findByUser_Username(String username);

    List<Classification> findByImageId(Long imageId);

    boolean existsByUser_UsernameAndImage_Id(String username, Long imageId);
}
