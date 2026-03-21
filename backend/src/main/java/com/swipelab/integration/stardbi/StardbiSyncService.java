package com.swipelab.integration.stardbi;

import com.swipelab.integration.stardbi.dto.ExternalExperimentDto;
import com.swipelab.tasks.domain.Task;
import com.swipelab.tasks.domain.TaskStatus;
// import com.swipelab.tasks.infrastructure.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StardbiSyncService {

    private final StardbiClient stardbiClient;
    // private final TaskRepository taskRepository;

    /**
     * Periodically syncs experiments from STARdbi and creates local Tasks.
     * Can be configured via yaml. For now, runs hourly.
     */
    @Scheduled(fixedDelayString = "${stardbi.sync.delay:3600000}")
    @Transactional
    public void syncExperiments() {
        log.info("Starting STARdbi experiment synchronization...");
        
        try {
            // TODO: Fetch token securely for a user or admin account performing the sync
            List<ExternalExperimentDto> experiments = stardbiClient.getExperiments("MOCK_TOKEN");

            for (ExternalExperimentDto exp : experiments) {
                // Here we would check if taskRepository.findByExternalExperimentId(exp.getId()) exists.
                // If not, we create a new Task with `sourceSystem = "STARDBI"` and `externalExperimentId = exp.getId()`.
                // Then fetch bounding boxes via stardbiClient.getUnclassifiedImageIds(exp.getId())
                // And insert them into the Image table.
                log.info("Discovered external STARdbi experiment: {} (ID: {})", exp.getName(), exp.getId());
            }

            log.info("Finished STARdbi experiment synchronization.");
        } catch (Exception e) {
            log.error("Error during STARdbi synchronization", e);
        }
    }
}
