package com.swipelab.integration.stardbi;

import com.swipelab.classification.events.ClassificationSubmittedEvent;
import com.swipelab.integration.stardbi.dto.ExternalClassificationDto;
import com.swipelab.tasks.domain.Task;
// import com.swipelab.tasks.infrastructure.TaskRepository;
// import com.swipelab.users.domain.User;
// import com.swipelab.users.infrastructure.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class StardbiClassificationEventListener {

    private final StardbiClient stardbiClient;
    // private final TaskRepository taskRepository;
    // private final UserRepository userRepository;

    @KafkaListener(topics = "classification-events", groupId = "stardbi-integration-group")
    @Transactional(readOnly = true)
    public void handleClassificationEvent(ClassificationSubmittedEvent event) {
        try {
            log.info("Received classification event for task {}", event.getTaskId());

            // 1. Fetch task to check if it's a STARdbi task
            // Optional<Task> taskOpt = taskRepository.findById(event.getTaskId());
            // if (taskOpt.isEmpty() || !"STARDBI".equals(taskOpt.get().getSourceSystem())) {
            //     return; // Not an external task
            // }

            // 2. Fetch user to get external ID mapping
            // Optional<User> userOpt = userRepository.findByUsername(event.getUsername());
            
            // if (userOpt.isPresent()) {
                ExternalClassificationDto dto = ExternalClassificationDto.builder()
                        .userId(event.getUsername()) // Using username as ID fallback for now
                        .userClassificationGrade(event.isCorrect() ? 10 : 1) // Mapping logic
                        .imageId(event.getImageId()) // Matches bounding box ID
                        .classificationId(1L) // Needs mapping from species string to species_id
                        .build();

                // TODO: Retrieve user's actual external access token from the database
                stardbiClient.submitClassifications(Collections.singletonList(dto), "USER_SPECIFIC_MOCK_TOKEN");
                log.info("Successfully synced classification to STARdbi for image {}", event.getImageId());
            // }

        } catch (Exception e) {
            log.error("Failed to process classification event for STARdbi sync", e);
            // In a real system, we might want to leverage dead-letter queues here.
        }
    }
}
