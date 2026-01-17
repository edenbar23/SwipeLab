// package com.swipelab.repository;

// import com.swipelab.model.entity.Badge;
// import com.swipelab.model.entity.User;
// import com.swipelab.model.entity.UserBadge;
// import org.junit.jupiter.api.Test;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
// import org.springframework.test.context.ActiveProfiles;

// import java.util.List;
// import java.util.Optional;

// import static org.assertj.core.api.Assertions.assertThat;

// @DataJpaTest
// @ActiveProfiles("test")
// public class BadgeRepositoryIntegrationTest {

// @Autowired
// private BadgeRepository badgeRepository;

// @Autowired
// private UserBadgeRepository userBadgeRepository;

// @Autowired
// private UserRepository userRepository;

// @Test
// void testSeedDataExists() {
// long count = badgeRepository.count();
// System.out.println("DEBUG: Badge Count = " + count);

// // V5 migration should have seeded these
// Optional<Badge> firstSwipe = badgeRepository.findByName("First Swipe");
// Optional<Badge> streak3 = badgeRepository.findByName("3 Day Streak");

// assertThat(firstSwipe).isPresent();
// assertThat(streak3).isPresent();
// }

// @Test
// void testAssignBadgeToUser() {
// // Create User
// User user = User.builder()
// .username("badgetest")
// .email("badge@test.com")
// .build();
// userRepository.save(user);

// // Get Badge
// Badge badge = badgeRepository.findByName("First Swipe").orElseThrow();

// // Assign Badge
// UserBadge userBadge = UserBadge.builder()
// .user(user)
// .badge(badge)
// .build();
// userBadgeRepository.save(userBadge);

// // Retrieve
// List<UserBadge> userBadges =
// userBadgeRepository.findByUser_Username("badgetest");
// assertThat(userBadges).hasSize(1);
// assertThat(userBadges.get(0).getBadge().getName()).isEqualTo("First Swipe");
// }
// }
