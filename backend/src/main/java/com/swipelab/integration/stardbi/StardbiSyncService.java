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

    private final StardbiClientPort stardbiClient;
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
                                    .taskId(task.getId())
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

    public void syncExperimentsForTask(Task task, String accessToken, String refreshToken) {
        log.info("Starting STARdbi experiment ZIP download for task: {}", task.getId());

        String currentAccessToken = accessToken;
        // If access token is a non-null but non-Stardbi token (e.g. a SwipeLab JWT),
        // checkAuth against the real Stardbi will return false and we fall back to
        // the service account — which is fine. We deliberately pass null so the client
        // uses its own getServiceAccountToken() fallback.
        if (currentAccessToken != null && !currentAccessToken.isEmpty()
                && !stardbiClient.checkAuth(currentAccessToken)) {
            log.info("Provided token failed Stardbi auth check for task {}; falling back to service account.", task.getId());
            if (refreshToken != null && !refreshToken.isEmpty()) {
                try {
                    com.swipelab.integration.stardbi.dto.StardbiAuthResponseDto newAuth =
                            stardbiClient.refreshToken(new com.swipelab.integration.stardbi.dto.StardbiRefreshTokenRequestDto(refreshToken));
                    currentAccessToken = newAuth.getAccess();
                    log.info("Stardbi token refreshed successfully for task {}", task.getId());
                } catch (Exception e) {
                    log.warn("Token refresh failed for task {}; falling back to service account.", task.getId(), e);
                    currentAccessToken = null;
                }
            } else {
                currentAccessToken = null; // Let StardbiClient use service account
            }
        }

        int totalSaved = 0;
        try {
            for (Long expId : task.getExperiments()) {
                byte[] zipBytes;
                try {
                    zipBytes = stardbiClient.downloadExperimentCropsZip(expId, currentAccessToken);
                } catch (Exception e) {
                    log.error("Failed to download ZIP for experiment {} (task {}): {}", expId, task.getId(), e.getMessage());
                    continue;
                }

                if (zipBytes == null || zipBytes.length == 0) {
                    log.warn("Empty ZIP received for experiment {} (task {})", expId, task.getId());
                    continue;
                }

                try (java.util.zip.ZipInputStream zis =
                             new java.util.zip.ZipInputStream(new java.io.ByteArrayInputStream(zipBytes))) {
                    java.util.zip.ZipEntry zipEntry = zis.getNextEntry();
                    int savedCount = 0;
                    int entryIndex = 0;
                    while (zipEntry != null) {
                        if (!zipEntry.isDirectory()) {
                            String fileName = zipEntry.getName();

                            // Log the first 5 filenames so we can diagnose the real StarDBI format
                            if (entryIndex < 5) {
                                log.warn("[StarDBI ZIP] experiment={} entry[{}] filename='{}' — used for boxId parsing",
                                        expId, entryIndex, fileName);
                            }
                            entryIndex++;

                            Long boxId = extractBoxIdFromFileName(fileName);

                            // Fallback: if filename doesn't match {imageId}_{boxId}.ext,
                            // derive a synthetic unique ID from the filename hash so the
                            // image is not silently skipped.
                            if (boxId == null) {
                                boxId = (long) (expId * 1_000_000L + Math.abs(fileName.hashCode() % 1_000_000L));
                                log.warn("[StarDBI ZIP] Could not parse boxId from '{}' — using synthetic id {} (experiment {})",
                                        fileName, boxId, expId);
                            }

                            if (!imageRepository.existsByExternalBoxIdAndTaskId(boxId, task.getId())) {
                                // Store image bytes as base64 in the DB — avoids Docker volume dependency
                                // and survives container restarts without any mounted filesystem.
                                byte[] imageBytes = zis.readAllBytes();
                                String base64 = Base64.getEncoder().encodeToString(imageBytes);

                                Image newImage = Image.builder()
                                        .taskId(task.getId())
                                        .externalBoxId(boxId)
                                        .parentImageId(extractImageIdFromFileName(fileName))
                                        .srcPath(base64)
                                        .experimentId(expId)
                                        .build();

                                imageRepository.save(newImage);
                                savedCount++;
                            }
                        }
                        zipEntry = zis.getNextEntry();
                    }
                    totalSaved += savedCount;
                    log.info("Saved {} new crops (base64) for experiment {} (task {})", savedCount, expId, task.getId());
                }
            }

            task.setStatus(TaskStatus.ACTIVE);
            taskRepository.save(task);
            log.info("Task {} is now ACTIVE with {} total new images ingested.", task.getId(), totalSaved);

        } catch (Exception e) {
            log.error("Fatal error during crop sync for task {}; marking task as FAILED.", task.getId(), e);
            try {
                task.setStatus(TaskStatus.FAILED);
                taskRepository.save(task);
            } catch (Exception saveEx) {
                log.error("Could not persist FAILED status for task {}", task.getId(), saveEx);
            }
        }
    }

    private Long extractBoxIdFromFileName(String fileName) {
        try {
            String nameWithoutExt = fileName.substring(0, fileName.lastIndexOf('.'));
            String[] parts = nameWithoutExt.split("_");
            return Long.parseLong(parts[parts.length - 1]);
        } catch (Exception e) {
            return null;
        }
    }

    private Long extractImageIdFromFileName(String fileName) {
        try {
            String nameWithoutExt = fileName.substring(0, fileName.lastIndexOf('.'));
            String[] parts = nameWithoutExt.split("_");
            return Long.parseLong(String.join("", java.util.Arrays.copyOf(parts, parts.length - 1)));
        } catch (Exception e) {
            return null;
        }
    }
}
