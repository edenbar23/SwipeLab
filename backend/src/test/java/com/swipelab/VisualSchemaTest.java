package com.swipelab;

import com.swipelab.model.entity.Badge;
import com.swipelab.model.entity.GoldImage;
import com.swipelab.model.entity.User;
import com.swipelab.model.enums.AuthProvider;
import com.swipelab.model.enums.UserRole;
import com.swipelab.repository.BadgeRepository;
import com.swipelab.repository.GoldImageRepository;
import com.swipelab.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

@SpringBootTest
@ActiveProfiles("integration")
public class VisualSchemaTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BadgeRepository badgeRepository;

    @Autowired
    private GoldImageRepository goldImageRepository;

    @Test
    public void visualizeDummyData() {
        System.out.println("\n\n================ CHECKING DATABASE TABLES =================\n");

        // 1. Create and Save User
        User user = User.builder()
                .username("visual_tester")
                .email("visual@test.com")
                .provider(AuthProvider.LOCAL)
                .role(UserRole.USER)
                .credibilityScore(85.5)
                .build();
        userRepository.save(user);

        // 2. Create and Save Badge
        Badge badge = new Badge();
        badge.setName("Bug Exterminator");
        badge.setDescription("Awarded for validating 100 images");
        badge.setIconUrl("http://example.com/badge.png");
        badge.setCreatedAt(LocalDateTime.now());
        badgeRepository.save(badge);

        System.out.println("--- USERS TABLE ---");
        List<User> users = userRepository.findAll();
        users.forEach(u -> System.out.println("User: " + u.getUsername() + " | Email: " + u.getEmail()
                + " | Credibility: " + u.getCredibilityScore()));

        System.out.println("\n--- BADGES TABLE ---");
        List<Badge> badges = badgeRepository.findAll();
        badges.forEach(b -> System.out.println("Badge: " + b.getName() + " | Desc: " + b.getDescription()));

        System.out.println("\n================ TEST COMPLETE =================\n\n");
    }
}
