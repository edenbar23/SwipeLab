package com.swipelab.recipients.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import com.swipelab.recipients.domain.RecipientGroup;

import java.util.Optional;

public interface RecipientGroupRepository
        extends JpaRepository<RecipientGroup, Long> {

    Optional<RecipientGroup> findByName(String name);

    boolean existsByName(String name);

    java.util.List<RecipientGroup> findByUsers_Username(String username);
}
