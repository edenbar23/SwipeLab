package com.swipelab.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.swipelab.model.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // TODO: Add custom query methods (e.g., findByUsername)
}
