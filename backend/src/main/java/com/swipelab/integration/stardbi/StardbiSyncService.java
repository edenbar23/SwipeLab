package com.swipelab.integration.stardbi;

import com.swipelab.integration.stardbi.dto.ExternalExperimentDto;
import com.swipelab.integration.stardbi.dto.ExternalCropDto;
import com.swipelab.tasks.domain.Task;
import com.swipelab.tasks.domain.TaskStatus;
import com.swipelab.tasks.infrastructure.TaskRepository;
import com.swipelab.classification.domain.Image;
import com.swipelab.classification.infrastructure.ImageRepository;
import com.swipelab.users.domain.User;
import com.swipelab.users.infrastructure.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Base64;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class StardbiSyncService {

    private final StardbiClient stardbiClient;
    private final TaskRepository taskRepository;
    private final ImageRepository imageRepository;
    private final UserRepository userRepository;

    /**
     * Periodically syncs experiments from STARdbi and creates local Tasks.
     * Can be configured via yaml. For now, runs hourly.
     */
    @Scheduled(fixedDelayString = "${stardbi.sync.delay:3600000}")
    @Transactional
    public void syncExperiments() {
        log.info("Starting STARdbi experiment synchronization...");
        
        try {
            List<ExternalExperimentDto> experiments = stardbiClient.getExperiments();
            User systemUser = userRepository.findAll().stream().findFirst().orElseThrow(() -> new IllegalStateException("No users in system to attach task to"));

            for (ExternalExperimentDto exp : experiments) {
                log.info("Processing external STARdbi experiment: {} (ID: {})", exp.getName(), exp.getId());

                // Find or create local Task
                Task task = taskRepository.findAll().stream()
                        .filter(t -> "STARDBI".equals(t.getSourceSystem()) && t.getExperiments().contains(exp.getId()))
                        .findFirst()
                        .orElseGet(() -> {
                            Task newTask = Task.builder()
                                    .title("STARDBI: " + exp.getName())
                                    .name(exp.getName())
                                    .sourceSystem("STARDBI")
                                    .experiments(List.of(exp.getId()))
                                    .createdBy(systemUser.getUsername())
                                    .status(TaskStatus.ACTIVE)
                                    .build();
                            return taskRepository.save(newTask);
                        });

                // Fetch metadata
                List<ExternalCropDto> crops = stardbiClient.getUnclassifiedImageIds(exp.getId());
                if (crops == null) continue;

                int savedCount = 0;
                for (ExternalCropDto crop : crops) {
                    if (imageRepository.existsByExternalBoxId(crop.getBoxId())) {
                        continue; // Already ingested
                    }

                    try {
                        byte[] imageBytes = stardbiClient.getImageBuffer(crop.getBoxId());
                        if (imageBytes != null && imageBytes.length > 0) {
                            String base64Image = Base64.getEncoder().encodeToString(imageBytes);

                            Image newImage = Image.builder()
                                    .task(task)
                                    .externalBoxId(crop.getBoxId())
                                    .parentImageId(crop.getImageId())
                                    .srcPath(base64Image)
                                    .experimentId(exp.getId())
                                    .build();
                            
                            imageRepository.save(newImage);
                            savedCount++;
                        }
                    } catch (Exception e) {
                        log.warn("Failed to download or save crop boxId={} for experiment {}: {}", crop.getBoxId(), exp.getId(), e.getMessage());
                    }
                }
                
                log.info("Ingested {} new crops for experiment {}", savedCount, exp.getId());
            }

            log.info("Finished STARdbi experiment synchronization.");
        } catch (Exception e) {
            log.error("Error during STARdbi synchronization", e);
        }
    }
}
