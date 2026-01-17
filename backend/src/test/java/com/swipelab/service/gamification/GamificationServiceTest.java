// package com.swipelab.service.gamification;

// import com.swipelab.model.entity.Badge;
// import com.swipelab.model.entity.User;
// import com.swipelab.repository.BadgeRepository;
// import com.swipelab.repository.UserRepository;
// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.Test;
// import org.junit.jupiter.api.extension.ExtendWith;
// import org.mockito.InjectMocks;
// import org.mockito.Mock;
// import org.mockito.junit.jupiter.MockitoExtension;

// import java.time.LocalDateTime;
// import java.util.HashSet;
// import java.util.Optional;

// import static org.junit.jupiter.api.Assertions.assertEquals;
// import static org.mockito.ArgumentMatchers.any;
// import static org.mockito.Mockito.*;

// @ExtendWith(MockitoExtension.class)
// class GamificationServiceTest {

// @Mock
// private UserRepository userRepository;

// @Mock
// private BadgeRepository badgeRepository;

// @InjectMocks
// private StreakService streakService;

// @InjectMocks
// private BadgeService badgeService;

// private User user;

// @BeforeEach
// void setUp() {
// user = User.builder()
// .username("testuser")
// .points(0L)
// .currentStreak(0)
// .badges(new HashSet<>())
// .totalClassifications(0)
// .build();
// }

// @Test
// void testStreakInitialize() {
// user.setLastStreakUpdate(null);

// streakService.updateStreak(user);

// assertEquals(1, user.getCurrentStreak());
// verify(userRepository, times(1)).save(user);
// }

// @Test
// void testStreakIncrement() {
// user.setLastStreakUpdate(LocalDateTime.now().minusDays(1));
// user.setCurrentStreak(5);

// streakService.updateStreak(user);

// assertEquals(6, user.getCurrentStreak());
// verify(userRepository, times(1)).save(user);
// }

// @Test
// void testStreakReset() {
// user.setLastStreakUpdate(LocalDateTime.now().minusDays(2));
// user.setCurrentStreak(5);

// streakService.updateStreak(user);

// assertEquals(1, user.getCurrentStreak());
// verify(userRepository, times(1)).save(user);
// }

// @Test
// void testSameDayNoUpdate() {
// user.setLastStreakUpdate(LocalDateTime.now());
// user.setCurrentStreak(5);

// streakService.updateStreak(user);

// assertEquals(5, user.getCurrentStreak());
// verify(userRepository, never()).save(user);
// }

// @Test
// void testAwardStreakBadge() {
// user.setCurrentStreak(3);
// when(badgeRepository.findByName("3 Day Streak"))
// .thenReturn(Optional.of(Badge.builder().name("3 Day Streak").build()));

// badgeService.checkForBadges(user);

// verify(userRepository, times(1)).save(user);
// assertEquals(1, user.getBadges().size());
// assertEquals("3 Day Streak", user.getBadges().iterator().next().getName());
// }

// @Test
// void testCalculatePointsWithMultiplier() {
// PointsService pointsServiceWithRealMethod = new
// PointsService(userRepository);

// // < 7 days: 1.0x
// user.setCurrentStreak(5);
// user.setPoints(0L);
// pointsServiceWithRealMethod.calculateAndAddPoints(user, 10);
// assertEquals(10, user.getPoints(), "Streak 5: Should be 10 points (1.0x)");

// // 7 days: 1.1x
// user.setCurrentStreak(7);
// user.setPoints(0L);
// pointsServiceWithRealMethod.calculateAndAddPoints(user, 10);
// assertEquals(11, user.getPoints(), "Streak 7: Should be 11 points (1.1x)");

// // 14 days: 1.25x
// user.setCurrentStreak(14);
// user.setPoints(0L);
// pointsServiceWithRealMethod.calculateAndAddPoints(user, 10);
// assertEquals(13, user.getPoints(), "Streak 14: Should be 12.5 -> 13 points
// (1.25x rounded)");

// // 30 days: 1.5x
// user.setCurrentStreak(30);
// user.setPoints(0L);
// pointsServiceWithRealMethod.calculateAndAddPoints(user, 10);
// assertEquals(15, user.getPoints(), "Streak 30: Should be 15 points (1.5x)");
// }
// }
