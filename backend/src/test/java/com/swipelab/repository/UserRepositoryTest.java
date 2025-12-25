package com.swipelab.repository;

import com.swipelab.model.entity.User;
import com.swipelab.model.enums.AuthProvider;
import com.swipelab.model.enums.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void findByUsername_ShouldReturnUser_WhenExists() {
        // Arrange
        User user = User.builder()
                .username("testuser")
                .email("test@example.com")
                .passwordHash("encodedPassword")
                .role(UserRole.USER)
                .provider(AuthProvider.LOCAL)
                .build();
        userRepository.save(user);

        // Act
        Optional<User> found = userRepository.findByUsername("testuser");

        // Assert
        assertTrue(found.isPresent());
        assertEquals("test@example.com", found.get().getEmail());
    }

    @Test
    void findByEmail_ShouldReturnUser_WhenExists() {
        // Arrange
        User user = User.builder()
                .username("testemail")
                .email("email@example.com")
                .provider(AuthProvider.LOCAL)
                .build();
        userRepository.save(user);

        // Act
        Optional<User> found = userRepository.findByEmail("email@example.com");

        // Assert
        assertTrue(found.isPresent());
        assertEquals("testemail", found.get().getUsername());
    }

    @Test
    void existsByUsername_ShouldReturnTrue_WhenExists() {
        User user = User.builder().username("exist").email("e@e.com").build();
        userRepository.save(user);

        assertTrue(userRepository.existsByUsername("exist"));
        assertFalse(userRepository.existsByUsername("nonoptimistic"));
    }

    @Test
    void existsByEmail_ShouldReturnTrue_WhenExists() {
        User user = User.builder().username("exist2").email("exist@e.com").build();
        userRepository.save(user);

        assertTrue(userRepository.existsByEmail("exist@e.com"));
        assertFalse(userRepository.existsByEmail("non@e.com"));
    }
}
