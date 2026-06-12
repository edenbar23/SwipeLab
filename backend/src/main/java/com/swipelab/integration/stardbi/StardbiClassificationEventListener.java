package com.swipelab.integration.stardbi;

import com.swipelab.classification.events.ClassificationSubmittedEvent;
import com.swipelab.classification.domain.Image;
import com.swipelab.classification.infrastructure.ImageRepository;
import com.swipelab.integration.stardbi.dto.ExternalLabelDto;
import com.swipelab.integration.stardbi.dto.ExternalTaxonomyDto;
import com.swipelab.tasks.domain.Task;
import com.swipelab.tasks.infrastructure.TaskRepository;
import com.swipelab.users.domain.User;
import com.swipelab.users.infrastructure.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class StardbiClassificationEventListener {

    private final StardbiClientPort stardbiClient;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final ImageRepository imageRepository;
    
    // Thread-safe cache for resolving String species names to numeric species_id from Stardbi
    private final ConcurrentHashMap<String, Long> speciesTaxonomyCache = new ConcurrentHashMap<>();

    private Long resolveSpeciesId(String speciesName) {
        if (speciesName == null) return null;
        
        if (speciesTaxonomyCache.isEmpty()) {
            synchronized (speciesTaxonomyCache) {
                if (speciesTaxonomyCache.isEmpty()) {
                    List<ExternalTaxonomyDto> taxonomy = stardbiClient.getTaxonomy();
                    if (taxonomy != null) {
                        for (ExternalTaxonomyDto tax : taxonomy) {
                            if (tax.getSpecies() != null) {
                                speciesTaxonomyCache.put(tax.getSpecies().toLowerCase(), tax.getSpeciesId());
                            }
                        }
                    }
                }
            }
        }
        return speciesTaxonomyCache.get(speciesName.toLowerCase());
    }

    @Retryable(
            value = { Exception.class },
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000, multiplier = 2)
    )
    @Async
    @EventListener
    @Transactional(readOnly = true)
    public void handleClassificationEvent(ClassificationSubmittedEvent event) {
        log.info("Received classification event for task {}", event.getTaskId());

        // 1. Fetch task to check if it's a STARdbi task
        Optional<Task> taskOpt = taskRepository.findById(event.getTaskId());
        if (taskOpt.isEmpty() || !"STARDBI".equals(taskOpt.get().getSourceSystem())) {
            return; // Not an external task
        }

        // 2. Fetch user
        Optional<User> userOpt = userRepository.findByUsername(event.getUsername());
        
        // 3. Fetch specific Image (crop) to get parent data
        Optional<Image> imageOpt = imageRepository.findById(event.getImageId());
        
        if (userOpt.isPresent() && imageOpt.isPresent()) {
            Image image = imageOpt.get();
            
            ExternalLabelDto label = ExternalLabelDto.builder()
                    .swipeLabUserId(event.getUsername()) // Mapping natively as a string
                    .userGrade(event.isCorrect() ? 3 : 1)
                    .boxId(image.getExternalBoxId()) // Use external box ID instead of local DB sequence ID
                    .imageId(image.getParentImageId()) // The original full-resolution parent image ID
                    .speciesId(resolveSpeciesId(event.getSpecies())) 
                    .build();

            stardbiClient.postLabel(label);
            log.info("Successfully synced label to STARdbi for box {}", event.getImageId());
        }
    }

    @Recover
    public void recoverClassificationSync(Exception e, ClassificationSubmittedEvent event) {
        log.error("SEVERE EXCEPTION: Exhausted all retries while syncing classification event for box {}. Manual dead-letter queue processing may be required. Error: {}", 
                event != null ? event.getImageId() : "UNKNOWN", e.getMessage(), e);
    }
}
