package com.swipelab.repository;

import com.swipelab.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // TODO: Add custom query methods (e.g., findByUsername)
}
