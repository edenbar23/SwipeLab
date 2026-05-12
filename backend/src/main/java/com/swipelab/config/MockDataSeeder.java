package com.swipelab.config;

import com.swipelab.auth.infrastructure.AuthProvider;
import com.swipelab.classification.domain.Image;
import com.swipelab.classification.domain.Label;
import com.swipelab.classification.domain.Classification;
import com.swipelab.classification.domain.GoldImage;
import com.swipelab.classification.domain.CredibilityRecord;
import com.swipelab.classification.infrastructure.ImageRepository;
import com.swipelab.classification.infrastructure.LabelRepository;
import com.swipelab.classification.infrastructure.ClassificationRepository;
import com.swipelab.classification.infrastructure.GoldImageRepository;
import com.swipelab.classification.infrastructure.CredibilityRepository;
import com.swipelab.gamification.domain.Gamification;
import com.swipelab.gamification.infrastructure.GamificationRepository;
import com.swipelab.model.enums.UserRole;
import com.swipelab.recipients.domain.RecipientGroup;
import com.swipelab.recipients.domain.RecipientUser;
import com.swipelab.recipients.infrastructure.RecipientGroupRepository;
import com.swipelab.recipients.infrastructure.RecipientUserRepository;
import com.swipelab.tasks.domain.Task;
import com.swipelab.tasks.domain.TaskStatus;
import com.swipelab.tasks.infrastructure.TaskRepository;
import com.swipelab.users.domain.User;
import com.swipelab.users.infrastructure.UserRepository;
import com.swipelab.analytics.domain.*;
import com.swipelab.analytics.infrastructure.*;
import com.swipelab.gamification.badge.BadgeDefinition;
import com.swipelab.gamification.badge.BadgeDefinitionRepository;
import com.swipelab.gamification.challenge.AggregationType;
import com.swipelab.gamification.challenge.ChallengeDefinition;
import com.swipelab.gamification.challenge.ChallengeDefinitionRepository;
import com.swipelab.gamification.challenge.MetricType;
import com.swipelab.gamification.challenge.TimeWindowType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * Seeds the database with mock data.
 * ONLY runs when the "mock" profile is active.
 */
@Slf4j
@RequiredArgsConstructor
@Component
@Profile("mock")
public class MockDataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final LabelRepository labelRepository;
    private final ImageRepository imageRepository;
    private final PasswordEncoder passwordEncoder;
    
    private final GamificationRepository gamificationRepository;
    private final RecipientGroupRepository recipientGroupRepository;
    private final RecipientUserRepository recipientUserRepository;
    private final GoldImageRepository goldImageRepository;
    private final CredibilityRepository credibilityRepository;
    private final ClassificationRepository classificationRepository;
    
    private final UserRankingRepository userRankingRepository;
    private final UserDailyStatsRepository userDailyStatsRepository;
    private final TaskSpeciesStatsRepository taskSpeciesStatsRepository;
    private final TaskDailyStatsRepository taskDailyStatsRepository;
    private final ClassificationFactRepository classificationFactRepository;
    private final BadgeDefinitionRepository badgeDefinitionRepository;
    private final ChallengeDefinitionRepository challengeDefinitionRepository;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("🌱 Starting Mock Data Seeding for 'mock' profile...");

        seedUsers();
        seedLabels();
        seedTasksAndImages();
        
        seedGamification();
        seedRecipientsAndAssignTasks();
        seedGoldImagesAndClassifications();
        seedAnalytics();
        seedChallenges();

        log.info("✅ Mock Data Seeding complete!");
    }

    private void seedUsers() {
        if (!userRepository.existsById("admin_mock")) {
            String passwordHash = passwordEncoder.encode("password");

            User admin = User.builder()
                    .username("admin_mock")
                    .email("admin@mock.com")
                    .passwordHash(passwordHash)
                    .emailVerified(true)
                    .provider(AuthProvider.LOCAL)
                    .role(UserRole.RESEARCHER)
                    .displayName("Mock Admin")
                    .active(true)
                    .build();

            User user = User.builder()
                    .username("user_mock")
                    .email("user@mock.com")
                    .passwordHash(passwordHash)
                    .emailVerified(true)
                    .provider(AuthProvider.LOCAL)
                    .role(UserRole.USER)
                    .displayName("Mock User")
                    .active(true)
                    .build();
                    
            User reviewer = User.builder()
                    .username("reviewer_mock")
                    .email("reviewer@mock.com")
                    .passwordHash(passwordHash)
                    .emailVerified(true)
                    .provider(AuthProvider.LOCAL)
                    .role(UserRole.USER)
                    .displayName("Mock Reviewer")
                    .active(true)
                    .build();

            userRepository.saveAll(List.of(admin, user, reviewer));
            log.info("Seeded Users. Login with user_mock/password or admin_mock/password.");
        }
    }

    private void seedLabels() {
        if (labelRepository.count() == 0) {
            Label cat = Label.builder()
                    .name("CAT")
                    .commonName("Cat")
                    .description("Felis catus")
                    .build();

            Label dog = Label.builder()
                    .name("DOG")
                    .commonName("Dog")
                    .description("Canis lupus familiaris")
                    .build();

            labelRepository.saveAll(List.of(cat, dog));
            log.info("Seeded Labels.");
        }
    }

    private void seedTasksAndImages() {
        if (taskRepository.count() == 0) {
            User admin = userRepository.findById("admin_mock").orElseThrow();

            Task task = Task.builder()
                    .title("Mock Identification Task")
                    .name("mock_identification_task")
                    .description("Identify animals in these mock images")
                    .querySpecies("Mammals")
                    .question("Is this a Cat?")
                    .createdBy(admin.getUsername())
                    .status(TaskStatus.ACTIVE)
                    .minClassificationsPerImage(3)
                    .consensusThreshold(80.0)
                    .deadline(LocalDateTime.now().plusDays(30))
                    .build();

            taskRepository.save(task);

            Image img1 = Image.builder()
                    .srcPath("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=") // Mock Stardbi ID 101
                    .taskId(task.getId())
                    .priority(1)
                    .build();

            Image img2 = Image.builder()
                    .srcPath("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=") // Mock Stardbi ID 102
                    .taskId(task.getId())
                    .priority(1)
                    .build();

            imageRepository.saveAll(List.of(img1, img2));
            log.info("Seeded Tasks and Images.");
        }
    }
    
    private void seedGamification() {
        if (gamificationRepository.count() == 0) {
            Gamification userStats = Gamification.builder()
                    .username("user_mock")
                    .startStreak(LocalDateTime.now().minusDays(5))
                    .endStreak(LocalDateTime.now())
                    .currentStreak(5)
                    .longestStreak(12)
                    .score(4500L)
                    .badge("Eagle Eye, Quick Swiper")
                    .rank("EXPERT")
                    .build();
            
            Gamification adminStats = Gamification.builder()
                    .username("admin_mock")
                    .startStreak(LocalDateTime.now().minusDays(1))
                    .endStreak(LocalDateTime.now())
                    .currentStreak(1)
                    .longestStreak(3)
                    .score(250L)
                    .badge("Novice")
                    .rank("BEGINNER")
                    .build();
                    
            gamificationRepository.saveAll(List.of(userStats, adminStats));
            log.info("Seeded Gamification Leaderboard Data.");
        }
    }
    
    private void seedRecipientsAndAssignTasks() {
        if (recipientGroupRepository.count() == 0) {
            RecipientUser mockUserRef = RecipientUser.builder()
                    .username("user_mock")
                    .active(true)
                    .build();
                    
            RecipientUser reviewUserRef = RecipientUser.builder()
                    .username("reviewer_mock")
                    .active(true)
                    .build();
                    
            recipientUserRepository.saveAll(List.of(mockUserRef, reviewUserRef));
            
            RecipientGroup betaTesters = RecipientGroup.builder()
                    .name("Beta Testers")
                    .users(Set.of(mockUserRef, reviewUserRef))
                    .build();
                    
            recipientGroupRepository.save(betaTesters);
            
            // Assign the group to the mock task
            Task task = taskRepository.findAll().stream().findFirst().orElseThrow();
            task.getRecipientGroups().add(betaTesters.getId());
            taskRepository.save(task);
            
            log.info("Seeded Recipient Groups and Assigned Task.");
        }
    }
    
    private void seedGoldImagesAndClassifications() {
        if (goldImageRepository.count() == 0) {
            Task task = taskRepository.findAll().stream().findFirst().orElseThrow();
            List<Image> images = imageRepository.findAll();
            
            if (!images.isEmpty()) {
                Image beeImage = images.stream().filter(img -> img.getSrcPath().contains("BEE")).findFirst().orElse(images.get(0));
                
                GoldImage goldImage = GoldImage.builder()
                        .image(beeImage)
                        .species("BEE")
                        .correctAnswer(GoldImage.UserResponse.YES)
                        .build();
                        
                goldImageRepository.save(goldImage);
                
                Classification c1 = Classification.builder()
                        .username("user_mock")
                        .userRole("USER")
                        .taskId(task.getId())
                        .image(beeImage)
                        .querySpecies("BEE")
                        .userResponse(Classification.UserResponse.YES)
                        .build();
                
                classificationRepository.save(c1);
                
                CredibilityRecord cr1 = CredibilityRecord.builder()
                        .username("user_mock")
                        .taskId(task.getId())
                        .goldImage(goldImage)
                        .querySpecies("BEE")
                        .userResponse(Classification.UserResponse.YES)
                        .correctAnswer(GoldImage.UserResponse.YES)
                        .build();
                        
                credibilityRepository.save(cr1);
                
                log.info("Seeded Gold Images, Classifications, and Credibility Records.");
            }
        }
    }
    
    private void seedAnalytics() {
        if (userRankingRepository.count() == 0) {
            Task task = taskRepository.findAll().stream().findFirst().orElseThrow();
            LocalDate today = LocalDate.now();
            
            UserRanking userRank = UserRanking.builder()
                    .period("ALL_TIME")
                    .userId("user_mock")
                    .rank(1)
                    .accuracy(95.5)
                    .percentile(99)
                    .build();
            userRankingRepository.save(userRank);
                    
            UserDailyStats dailyStats = UserDailyStats.builder()
                    .userId("user_mock")
                    .day(today)
                    .total(120)
                    .correct(115)
                    .accuracy(95.8)
                    .build();
            userDailyStatsRepository.save(dailyStats);
            
            TaskDailyStats taskStats = TaskDailyStats.builder()
                    .taskId(task.getId())
                    .day(today)
                    .classifications(500)
                    .completedImages(150)
                    .consensusReached(145)
                    .build();
            taskDailyStatsRepository.save(taskStats);
            
            TaskSpeciesStats speciesStats = TaskSpeciesStats.builder()
                    .taskId(task.getId())
                    .species("BEE")
                    .classificationCount(300)
                    .agreementRate(98.2)
                    .truePositive(250)
                    .falsePositive(5)
                    .falseNegative(10)
                    .trueNegative(35)
                    .build();
            taskSpeciesStatsRepository.save(speciesStats);
            
            List<Image> images = imageRepository.findAll();
            if (!images.isEmpty()) {
                ClassificationFact fact = ClassificationFact.builder()
                        .classificationId(1L)
                        .taskId(task.getId())
                        .imageId(images.get(0).getId())
                        .userId("user_mock")
                        .species("BEE")
                        .isCorrect(true)
                        .isExpert(false)
                        .credibilityAtTime(0.95)
                        .responseTimeMs(1250L)
                        .day(today)
                        .build();
                classificationFactRepository.save(fact);
            }
            log.info("Seeded Analytics metrics for Dashboard visibility.");
        }
    }

    private void seedChallenges() {
        if (badgeDefinitionRepository.count() == 0) {
            BadgeDefinition legendBadge = BadgeDefinition.builder()
                    .title("LabSwiper Legend Badge")
                    .code("LEGEND_500")
                    .description("Reach 500 total classifications")
                    .iconUrl("/badges/legend.png")
                    .build();

            BadgeDefinition silverBadge = BadgeDefinition.builder()
                    .title("Silver Badge")
                    .code("SILVER_DAILY")
                    .description("Classify 20 images today")
                    .iconUrl("/badges/silver.png")
                    .build();

            BadgeDefinition firstSwipeBadge = BadgeDefinition.builder()
                    .title("First Swipe")
                    .code("FIRST_SWIPE")
                    .description("Classify your very first image")
                    .iconUrl("/badges/first_swipe.png")
                    .build();

            badgeDefinitionRepository.saveAll(List.of(legendBadge, silverBadge, firstSwipeBadge));

            if (challengeDefinitionRepository.count() == 0) {
                ChallengeDefinition legendChallenge = ChallengeDefinition.builder()
                        .name("Reach 500 total classifications")
                        .description("Lifetime challenge for classifications")
                        .metricType(MetricType.CLASSIFICATION)
                        .aggregationType(AggregationType.COUNT)
                        .targetValue(500)
                        .timeWindowType(TimeWindowType.LIFETIME)
                        .badgeId(legendBadge.getId())
                        .active(true)
                        .build();

                ChallengeDefinition dailyChallenge = ChallengeDefinition.builder()
                        .name("Classify 20 images today")
                        .description("Daily challenge for classifications")
                        .metricType(MetricType.CLASSIFICATION)
                        .aggregationType(AggregationType.COUNT)
                        .targetValue(20)
                        .timeWindowType(TimeWindowType.DAILY)
                        .badgeId(silverBadge.getId())
                        .active(true)
                        .build();

                ChallengeDefinition firstSwipeChallenge = ChallengeDefinition.builder()
                        .name("Classify 1 image")
                        .description("Classify your very first image to earn a badge quickly")
                        .metricType(MetricType.CLASSIFICATION)
                        .aggregationType(AggregationType.COUNT)
                        .targetValue(1)
                        .timeWindowType(TimeWindowType.LIFETIME)
                        .badgeId(firstSwipeBadge.getId())
                        .active(true)
                        .build();

                challengeDefinitionRepository.saveAll(List.of(legendChallenge, dailyChallenge, firstSwipeChallenge));
                log.info("Seeded Challenges and Badge Definitions.");
            }
        }
    }
}
