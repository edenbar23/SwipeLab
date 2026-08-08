package com.swipelab.classification;

import com.swipelab.classification.application.ClassificationService;
import com.swipelab.classification.domain.distribution.TaskDistributionService;
import com.swipelab.classification.domain.threshold.ConsensusResult;
import com.swipelab.classification.infrastructure.ConsensusResultRepository;
import com.swipelab.classification.infrastructure.ImageRepository;
import com.swipelab.classification.dto.api.SubmitClassificationRequest;
import com.swipelab.dto.request.ImageUploadRequest;
import com.swipelab.tasks.application.port.out.TargetSpeciesProvider;
import com.swipelab.tasks.domain.Task;
import com.swipelab.tasks.infrastructure.TaskRepository;
import com.swipelab.users.domain.User;
import com.swipelab.users.infrastructure.UserRepository;
import com.swipelab.classification.domain.image.Image;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;

@SpringBootTest
@ActiveProfiles("integration")
public class ThresholdIntegrationTest {

    @Autowired
    private ClassificationService classificationService;

    @Autowired
    private ConsensusResultRepository consensusResultRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private TaskDistributionService taskDistributionService;

    private Task testTask;
    private Image testImage;
    private User user1, user2, user3, user4;

    @Autowired
    private com.swipelab.classification.infrastructure.ClassificationRepository classificationRepository;

    @AfterEach
    void tearDown() {
        classificationRepository.deleteAll();
        consensusResultRepository.deleteAll();
        imageRepository.deleteAll();
        taskRepository.deleteAll();
        userRepository.deleteAll();
    }

    @BeforeEach
    void setUp() {
        // Create a task with a threshold of 3.0
        testTask = Task.builder()
                .name("Threshold Test Task")
                .title("Test Task")
                .status(com.swipelab.tasks.domain.TaskStatus.ACTIVE)
                .createdBy("admin")
                .consensusThreshold(3.0)
                .minClassificationsPerImage(5)
                .build();
        testTask = taskRepository.save(testTask);

        // Create an image
        testImage = Image.builder()
                .imageData("test-image.jpg")
                .taskId(testTask.getId())
                .build();
        testImage = imageRepository.save(testImage);

        // Create 4 users with credibility 1.0 (default)
        user1 = createUser("user1");
        user2 = createUser("user2");
        user3 = createUser("user3");
        user4 = createUser("user4");
    }

    private User createUser(String username) {
        User user = User.builder()
                .username(username)
                .email(username + "@test.com")
                .credibilityScore(100.0)
                .build();
        return userRepository.save(user);
    }

    @Test
    void testConsensusReachedAndRemovedFromDistribution() throws Exception {
        String querySpecies = "Lion";
        
        // Ensure image is available to user4 initially
        Optional<TaskDistributionService.ImageSpeciesPair> pair1 = taskDistributionService.getNextRegularImagePair(
                user4.getUsername(), testTask.getId(), List.of(querySpecies));
        assertThat(pair1).isPresent();
        assertThat(pair1.get().image().getId()).isEqualTo(testImage.getId());

        // User 1 classifies as Lion
        SubmitClassificationRequest req1 = new SubmitClassificationRequest();
        req1.setImageId(testImage.getId());
        req1.setTaskId(testTask.getId());
        req1.setQuestion("Is this a " + querySpecies + "?");
        req1.setDecision(com.swipelab.classification.domain.core.Classification.UserResponse.YES);
        classificationService.submitClassification(user1.getUsername(), "ROLE_USER", 100.0, req1);
        
        // Consensus not yet reached
        assertThat(consensusResultRepository.findAll()).isEmpty();

        // User 2 classifies as Lion
        SubmitClassificationRequest req2 = new SubmitClassificationRequest();
        req2.setImageId(testImage.getId());
        req2.setTaskId(testTask.getId());
        req2.setQuestion("Is this a " + querySpecies + "?");
        req2.setDecision(com.swipelab.classification.domain.core.Classification.UserResponse.YES);
        classificationService.submitClassification(user2.getUsername(), "ROLE_USER", 100.0, req2);
        
        assertThat(consensusResultRepository.findAll()).isEmpty();

        // User 3 classifies as Lion (Sum is now 3.0 = threshold)
        SubmitClassificationRequest req3 = new SubmitClassificationRequest();
        req3.setImageId(testImage.getId());
        req3.setTaskId(testTask.getId());
        req3.setQuestion("Is this a " + querySpecies + "?");
        req3.setDecision(com.swipelab.classification.domain.core.Classification.UserResponse.YES);
        classificationService.submitClassification(user3.getUsername(), "ROLE_USER", 100.0, req3);

        // Let async event listener finish
        Thread.sleep(500);

        // Verify ConsensusResult was saved
        List<ConsensusResult> results = consensusResultRepository.findAll();
        assertThat(results).hasSize(1);
        ConsensusResult result = results.get(0);
        assertThat(result.getImageId()).isEqualTo(testImage.getId());
        assertThat(result.getSpecies()).isEqualTo(querySpecies);
        assertThat(result.getWinningDecision()).isEqualTo("YES");

        // Verify Image is NO LONGER available to User 4 for this species
        Optional<TaskDistributionService.ImageSpeciesPair> pair2 = taskDistributionService.getNextRegularImagePair(
                user4.getUsername(), testTask.getId(), List.of(querySpecies));
        assertThat(pair2).isEmpty();
    }
}
