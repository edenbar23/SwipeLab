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
 * Seeds the database with E2E data.
 * ONLY runs when the "e2e" profile is active.
 */
@Slf4j
@RequiredArgsConstructor
@Component
@Profile("e2e")
public class E2eDataSeeder implements CommandLineRunner {

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
        log.info("🌱 Starting E2E Data Seeding for 'e2e' profile...");

        seedUsers();
        seedLabels();
        seedTasksAndImages();
        
        seedGamification();
        seedRecipientsAndAssignTasks();
        seedGoldImagesAndClassifications();
        seedAnalytics();
        seedChallenges();

        log.info("✅ E2E Data Seeding complete!");
    }

    private void seedUsers() {
        if (!userRepository.existsById("admin_e2e")) {
            String passwordHash = passwordEncoder.encode("superpassword123");

            User admin = User.builder()
                    .username("admin_e2e")
                    .email("admin@e2e.com")
                    .passwordHash(passwordHash)
                    .emailVerified(true)
                    .provider(AuthProvider.LOCAL)
                    .role(UserRole.RESEARCHER)
                    .displayName("E2E Admin")
                    .active(true)
                    .score(50L)
                    .build();

            User user = User.builder()
                    .username("e2e_user")
                    .email("user@e2e.com")
                    .passwordHash(passwordEncoder.encode("password"))
                    .emailVerified(true)
                    .provider(AuthProvider.LOCAL)
                    .role(UserRole.USER)
                    .displayName("E2E User")
                    .active(true)
                    .score(20L)
                    .build();
                    
            User reviewer = User.builder()
                    .username("reviewer_e2e")
                    .email("reviewer@e2e.com")
                    .passwordHash(passwordEncoder.encode("password"))
                    .emailVerified(true)
                    .provider(AuthProvider.LOCAL)
                    .role(UserRole.USER)
                    .displayName("E2E Reviewer")
                    .active(true)
                    .score(85L)
                    .build();

            userRepository.saveAll(List.of(admin, user, reviewer));
            log.info("Seeded E2E Users. Login with e2e_user/password or admin_e2e/superpassword123.");
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
            User admin = userRepository.findById("admin_e2e").orElseThrow();

            List<Long> speciesIds = labelRepository.findAll().stream()
                    .map(Label::getId)
                    .collect(java.util.stream.Collectors.toList());

            Task task = Task.builder()
                    .title("E2E Identification Task")
                    .name("e2e_identification_task")
                    .description("Identify animals in these e2e images")
                    .querySpecies("Mammals")
                    .question("Is this a Cat?")
                    .createdBy(admin.getUsername())
                    .status(TaskStatus.ACTIVE)
                    .minClassificationsPerImage(3)
                    .consensusThreshold(80.0)
                    .isPublic(false)
                    .deadline(LocalDateTime.now().plusDays(30))
                    .targetSpeciesIds(speciesIds)
                    .build();

            taskRepository.save(task);

            java.io.File folder = new java.io.File("src/main/resources/e2e-crops");
            List<Image> e2eImages = new java.util.ArrayList<>();
            
            if (folder.exists() && folder.isDirectory()) {
                java.io.File[] files = folder.listFiles();
                if (files != null) {
                    for (java.io.File file : files) {
                        if (file.isFile() && (file.getName().endsWith(".jpg") || file.getName().endsWith(".png"))) {
                            Long experimentId = 1L;
                            Long parentImageId = null;
                            Long boxId = null;
                            
                            try {
                                String nameWithoutExt = file.getName().substring(0, file.getName().lastIndexOf('.'));
                                String[] parts = nameWithoutExt.split("_");
                                if (parts.length >= 3) {
                                    experimentId = Long.parseLong(parts[0]);
                                    boxId = Long.parseLong(parts[parts.length - 1]);
                                    parentImageId = Long.parseLong(String.join("", java.util.Arrays.copyOfRange(parts, 1, parts.length - 1)));
                                }
                            } catch (Exception e) {
                                log.warn("Could not parse image name for metadata: {}", file.getName());
                            }

                            e2eImages.add(Image.builder()
                                    .srcPath(file.getPath().replace("\\", "/"))
                                    .taskId(task.getId())
                                    .priority(1)
                                    .experimentId(experimentId)
                                    .parentImageId(parentImageId)
                                    .externalBoxId(boxId)
                                    .build());
                        }
                    }
                }
            }
            
            if (e2eImages.isEmpty()) {
                e2eImages.add(Image.builder()
                        .srcPath("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=")
                        .taskId(task.getId())
                        .priority(1)
                        .build());
            }

            // Seed a second, public task that is not assigned to e2e_user
            // to allow testing of the task assignment and cache-update flow.
            Task exploreTask = Task.builder()
                    .title("Explore E2E Task")
                    .name("explore_e2e_task")
                    .description("Identify birds in these e2e images")
                    .querySpecies("Birds")
                    .question("Is this a Bird?")
                    .createdBy(admin.getUsername())
                    .status(TaskStatus.ACTIVE)
                    .minClassificationsPerImage(3)
                    .consensusThreshold(80.0)
                    .isPublic(true)
                    .deadline(LocalDateTime.now().plusDays(30))
                    .targetSpeciesIds(speciesIds)
                    .build();

            taskRepository.save(exploreTask);

            Image img3 = Image.builder()
                    .srcPath("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=")
                    .taskId(exploreTask.getId())
                    .priority(1)
                    .build();

            e2eImages.add(img3);
            imageRepository.saveAll(e2eImages);
            log.info("Seeded Tasks and Images (including public explore task).");
        }
    }
    
    private void seedGamification() {
        if (gamificationRepository.count() == 0) {
            Gamification userStats = Gamification.builder()
                    .username("e2e_user")
                    .startStreak(LocalDateTime.now().minusDays(5))
                    .endStreak(LocalDateTime.now())
                    .currentStreak(5)
                    .longestStreak(12)
                    .score(4500L)
                    .badge("Eagle Eye, Quick Swiper")
                    .rank("EXPERT")
                    .build();
            
            Gamification adminStats = Gamification.builder()
                    .username("admin_e2e")
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
                    .username("e2e_user")
                    .active(true)
                    .build();
                    
            RecipientUser reviewUserRef = RecipientUser.builder()
                    .username("reviewer_e2e")
                    .active(true)
                    .build();
                    
            recipientUserRepository.saveAll(List.of(mockUserRef, reviewUserRef));
            
            RecipientGroup betaTesters = RecipientGroup.builder()
                    .name("E2E Testers")
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
                        .username("e2e_user")
                        .userRole("USER")
                        .taskId(task.getId())
                        .image(beeImage)
                        .querySpecies("BEE")
                        .userResponse(Classification.UserResponse.YES)
                        .build();
                
                classificationRepository.save(c1);
                
                CredibilityRecord cr1 = CredibilityRecord.builder()
                        .username("e2e_user")
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
                    .userId("e2e_user")
                    .rank(1)
                    .accuracy(95.5)
                    .percentile(99)
                    .build();
            userRankingRepository.save(userRank);
                    
            UserDailyStats dailyStats = UserDailyStats.builder()
                    .userId("e2e_user")
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
                        .userId("e2e_user")
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
