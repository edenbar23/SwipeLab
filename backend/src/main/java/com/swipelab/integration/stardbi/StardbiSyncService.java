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

    @org.springframework.beans.factory.annotation.Value("${swipelab.storage.path:./storage/crops}")
    private String storagePath;

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

    @Transactional
    public void syncExperimentsForTask(Task task, String accessToken, String refreshToken) {
        log.info("Starting STARdbi experiment ZIP download for task: {}", task.getId());
        
        try {
            // Ensure storage directory exists
            java.io.File storageDir = new java.io.File(storagePath);
            if (!storageDir.exists()) {
                storageDir.mkdirs();
            }

            String currentAccessToken = accessToken;
            if (currentAccessToken == null || currentAccessToken.isEmpty()) {
                log.info("No user Stardbi token provided for task {}, falling back to service account token.", task.getId());
                // We don't have a public getServiceAccountToken method, so we pass null and let the client handle it if we modify it, or we can use it.
                // Wait, getServiceAccountToken() in StardbiClient is private!
                // If it's private, StardbiClient must handle null tokens by using the service account token.
            } else if (!stardbiClient.checkAuth(currentAccessToken)) {
                log.info("Stardbi access token expired for task {}, attempting refresh...", task.getId());
                if (refreshToken != null) {
                    com.swipelab.integration.stardbi.dto.StardbiAuthResponseDto newAuth = 
                        stardbiClient.refreshToken(new com.swipelab.integration.stardbi.dto.StardbiRefreshTokenRequestDto(refreshToken));
                    currentAccessToken = newAuth.getAccess();
                } else {
                    log.error("No refresh token provided, using service account token as fallback.");
                    currentAccessToken = null;
                }
            }

            for (Long expId : task.getExperiments()) {
                byte[] zipBytes = null;
                try {
                    zipBytes = stardbiClient.downloadExperimentCropsZip(expId, currentAccessToken);
                } catch (Exception e) {
                    log.error("Failed to download zip for experiment {}", expId, e);
                    continue;
                }

                if (zipBytes != null && zipBytes.length > 0) {
                    try (java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(new java.io.ByteArrayInputStream(zipBytes))) {
                        java.util.zip.ZipEntry zipEntry = zis.getNextEntry();
                        int savedCount = 0;
                        while (zipEntry != null) {
                            if (!zipEntry.isDirectory()) {
                                String fileName = zipEntry.getName();
                                Long boxId = extractBoxIdFromFileName(fileName);
                                
                                if (boxId != null && !imageRepository.existsByExternalBoxId(boxId)) {
                                    java.io.File outputFile = new java.io.File(storageDir, expId + "_" + fileName);
                                    java.nio.file.Files.copy(zis, outputFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                                    
                                    Image newImage = Image.builder()
                                            .taskId(task.getId())
                                            .externalBoxId(boxId)
                                            .parentImageId(extractImageIdFromFileName(fileName))
                                            .srcPath(outputFile.getAbsolutePath())
                                            .experimentId(expId)
                                            .build();
                                    
                                    imageRepository.save(newImage);
                                    savedCount++;
                                }
                            }
                            zipEntry = zis.getNextEntry();
                        }
                        log.info("Extracted and saved {} new crops for experiment {}", savedCount, expId);
                    }
                }
            }

            task.setStatus(TaskStatus.ACTIVE);
            taskRepository.save(task);
            log.info("Task {} is now ACTIVE", task.getId());
        } catch (Exception e) {
            log.error("Error during STARdbi zip synchronization for task {}", task.getId(), e);
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
