package com.swipelab.recipients.infrastructure;

import com.swipelab.recipients.domain.RecipientUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Set;

public interface RecipientUserRepository extends JpaRepository<RecipientUser, String> {

    Set<RecipientUser> findByUsernameIn(Collection<String> usernames);

}
