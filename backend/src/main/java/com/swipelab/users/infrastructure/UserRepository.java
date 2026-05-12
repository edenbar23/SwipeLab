package com.swipelab.users.infrastructure;

import com.swipelab.users.domain.User;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    Optional<User> findByEmailVerificationToken(String token);

    Optional<User> findByResetPasswordToken(String token);

    Set<User> findByUsernameIn(Collection<String> usernames);

    List<User> findByRole(com.swipelab.model.enums.UserRole role);

    /**
     * Revokes the refresh token for a user directly via UPDATE — no prior SELECT needed.
     * Eliminates the redundant DB roundtrip in the logout flow.
     */
    @Modifying
    @Query("UPDATE User u SET u.refreshTokenHash = null WHERE u.username = :username")
    int revokeRefreshToken(@Param("username") String username);
}