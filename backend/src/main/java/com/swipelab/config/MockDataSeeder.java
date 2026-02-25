package com.swipelab.config;

import com.swipelab.auth.infrastructure.AuthProvider;
import com.swipelab.classification.domain.Image;
import com.swipelab.classification.domain.Label;
import com.swipelab.classification.infrastructure.ImageRepository;
import com.swipelab.classification.infrastructure.LabelRepository;
import com.swipelab.model.enums.UserRole;
import com.swipelab.tasks.domain.Task;
import com.swipelab.tasks.domain.TaskStatus;
import com.swipelab.tasks.infrastructure.TaskRepository;
import com.swipelab.users.domain.User;
import com.swipelab.users.infrastructure.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.time.LocalDateTime;
import java.util.List;

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

    @Override
    @Transactional
    public void run(String... args) {
        log.info("🌱 Starting Mock Data Seeding for 'mock' profile...");

        seedUsers();
        seedLabels();
        seedTasksAndImages();

        log.info("✅ Mock Data Seeding complete!");
    }

    private void seedUsers() {
        if (userRepository.count() == 0) {
            // Hash for 'password' using BCrypt
            String passwordHash = passwordEncoder.encode("password");

            User admin = User.builder()
                    .username("admin_mock")
                    .email("admin@mock.com")
                    .passwordHash(passwordHash)
                    .emailVerified(true)
                    .provider(AuthProvider.LOCAL)
                    .role(UserRole.ADMIN)
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

            userRepository.saveAll(List.of(admin, user));
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
                    .description("Identify animals in these mock images")
                    .querySpecies("Mammals")
                    .question("Is this a Cat?")
                    .createdBy(admin)
                    .status(TaskStatus.ACTIVE)
                    .minClassificationsPerImage(3)
                    .consensusThreshold(80.0)
                    .deadline(LocalDateTime.now().plusDays(30))
                    .build();

            taskRepository.save(task);

            Image img1 = Image.builder()
                    .srcPath("https://i.ibb.co/QFRzshqz/trash.png") // TRASH
                    .task(task)
                    .priority(1)
                    .build();

            Image img2 = Image.builder()
                    .srcPath("https://i.ibb.co/JjKkDWqC/bee.png") // BEE
                    .task(task)
                    .priority(1)
                    .build();

            imageRepository.saveAll(List.of(img1, img2));
            log.info("Seeded Tasks and Images.");
        }
    }
}
