package com.swipelab.analytics.application;

import com.swipelab.auth.application.SecurityAuthorizationService;
import com.swipelab.classification.domain.core.Classification;
import com.swipelab.classification.domain.image.Image;
import com.swipelab.classification.infrastructure.ClassificationRepository;
import com.swipelab.classification.infrastructure.GoldImageRepository;
import com.swipelab.classification.infrastructure.ImageRepository;
import com.swipelab.exception.ResourceNotFoundException;
import com.swipelab.tasks.domain.Task;
import com.swipelab.tasks.infrastructure.TaskRepository;
import com.swipelab.users.domain.User;
import com.swipelab.users.infrastructure.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExportService {

    private final ClassificationRepository classificationRepository;
    private final ImageRepository imageRepository;
    private final GoldImageRepository goldImageRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final SecurityAuthorizationService securityAuthorizationService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    // Legacy header kept for backward-compatible single-task endpoints
    private static final String LEGACY_CSV_HEADER = "parent_image_id,crop_id,src_path,username,user_response,classified_at,is_gold_standard";

    private static final String MULTI_EXPORT_CSV_HEADER =
            "classification_id,task_id,task_name,parent_image_id,crop_id,image_src_path,username,user_role,query_species,user_response,credibility_score,is_gold_standard,classified_at";

    // ─── Multi-task export (Issue #257) ───────────────────────────────────────

    /**
     * Exports classifications for multiple tasks as a single CSV stream.
     * Authorization: super admin can export any task; researchers can only export
     * tasks they created or that are shared with them.
     */
    @Transactional(readOnly = true)
    public void exportMultiTaskClassificationsAsCsv(List<Long> taskIds, String username, OutputStream outputStream)
            throws IOException {

        List<Task> tasks = taskRepository.findAllById(taskIds);

        if (tasks.size() != taskIds.size()) {
            throw new ResourceNotFoundException("One or more requested task IDs do not exist");
        }

        // Authorization check per task
        if (!securityAuthorizationService.isSuperAdmin(username)) {
            for (Task task : tasks) {
                boolean isOwner = task.getCreatedBy().equals(username);
                boolean isShared = task.getSharedWithResearchers() != null
                        && task.getSharedWithResearchers().contains(username);
                if (!isOwner && !isShared) {
                    throw new AccessDeniedException(
                            "You do not have access to export task: " + task.getName());
                }
            }
        }

        // Lookup maps to avoid N+1 queries
        Map<Long, String> taskNameMap = tasks.stream()
                .collect(Collectors.toMap(Task::getId, Task::getName));

        List<Classification> classifications = classificationRepository.findByTaskIdIn(taskIds);

        // Batch-fetch user credibility scores
        Set<String> usernames = classifications.stream()
                .map(Classification::getUsername)
                .collect(Collectors.toSet());
        Map<String, Double> credibilityMap = new HashMap<>();
        if (!usernames.isEmpty()) {
            userRepository.findByUsernameIn(usernames).forEach(user ->
                    credibilityMap.put(user.getUsername(), user.getCredibilityScore()));
        }

        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8))) {
            writer.println(MULTI_EXPORT_CSV_HEADER);

            for (Classification c : classifications) {
                Image image = c.getImage();
                boolean isGold = goldImageRepository.existsByImageId(image.getId());

                String row = String.format("%d,%d,\"%s\",%s,%s,\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",%s,%b,\"%s\"",
                        c.getId(),
                        c.getTaskId(),
                        escapeCsv(taskNameMap.getOrDefault(c.getTaskId(), "")),
                        image.getParentImageId() != null ? image.getParentImageId().toString() : "",
                        image.getExternalBoxId() != null ? image.getExternalBoxId().toString() : "",
                        escapeCsv(image.getSrcPath()),
                        escapeCsv(c.getUsername()),
                        escapeCsv(c.getUserRole() != null ? c.getUserRole() : ""),
                        escapeCsv(c.getQuerySpecies() != null ? c.getQuerySpecies() : ""),
                        escapeCsv(c.getUserResponse().name()),
                        credibilityMap.containsKey(c.getUsername())
                                ? String.format("%.4f", credibilityMap.get(c.getUsername()))
                                : "",
                        isGold,
                        c.getCreatedAt() != null ? c.getCreatedAt().format(DATE_FORMATTER) : "");

                writer.println(row);
            }

            writer.flush();
        }
    }

    // ─── Legacy single-task export methods (unchanged) ────────────────────────

    /**
     * Export classifications for a task as CSV with streaming support.
     * Uses streaming to handle large datasets efficiently.
     */
    @Transactional(readOnly = true)
    public void exportClassificationsAsCsv(Long taskId, OutputStream outputStream) throws IOException {
        List<Image> images = imageRepository.findByTaskId(taskId);

        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8))) {
            writer.println(LEGACY_CSV_HEADER);

            for (Image image : images) {
                boolean isGold = goldImageRepository.existsByImageId(image.getId());
                List<Classification> classifications = classificationRepository.findByImageId(image.getId());

                for (Classification classification : classifications) {
                    writer.println(formatLegacyCsvRow(classification, isGold));
                }
            }

            writer.flush();
        }
    }

    /**
     * Export classifications for a task as JSON.
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> exportClassificationsAsJson(Long taskId) {
        List<Image> images = imageRepository.findByTaskId(taskId);
        List<Map<String, Object>> result = new ArrayList<>();

        for (Image image : images) {
            boolean isGold = goldImageRepository.existsByImageId(image.getId());
            List<Classification> classifications = classificationRepository.findByImageId(image.getId());

            for (Classification classification : classifications) {
                result.add(formatJsonObject(classification, isGold));
            }
        }

        return result;
    }

    /**
     * Get export summary for a task (counts before download).
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getExportSummary(Long taskId) {
        List<Image> images = imageRepository.findByTaskId(taskId);
        int totalImages = images.size();
        int totalClassifications = 0;

        for (Image image : images) {
            totalClassifications += classificationRepository.findByImageId(image.getId()).size();
        }

        Map<String, Object> summary = new HashMap<>();
        summary.put("taskId", taskId);
        summary.put("totalImages", totalImages);
        summary.put("totalClassifications", totalClassifications);
        summary.put("estimatedCsvSizeKb", totalClassifications * 100 / 1024); // ~100 bytes per row

        return summary;
    }

    // ─── Private helpers ─────────────────────────────────────────────────────

    private String formatLegacyCsvRow(Classification classification, boolean isGoldStandard) {
        Image image = classification.getImage();
        return String.format("%s,%s,\"%s\",\"%s\",\"%s\",\"%s\",%b",
                image.getParentImageId() != null ? image.getParentImageId().toString() : "",
                image.getExternalBoxId() != null ? image.getExternalBoxId().toString() : "",
                escapeCsv(image.getSrcPath()),
                escapeCsv(classification.getUsername()),
                escapeCsv(classification.getUserResponse().name()),
                classification.getCreatedAt() != null ? classification.getCreatedAt().format(DATE_FORMATTER) : "",
                isGoldStandard);
    }

    private Map<String, Object> formatJsonObject(Classification classification, boolean isGoldStandard) {
        Image image = classification.getImage();
        Map<String, Object> obj = new HashMap<>();
        obj.put("imageId", image.getParentImageId());
        obj.put("cropId", image.getExternalBoxId());
        obj.put("srcPath", image.getSrcPath());
        obj.put("username", classification.getUsername());
        obj.put("userResponse", classification.getUserResponse().name());
        obj.put("classifiedAt",
                classification.getCreatedAt() != null ? classification.getCreatedAt().toString() : null);
        obj.put("isGoldStandard", isGoldStandard);
        return obj;
    }

    private String escapeCsv(String value) {
        if (value == null)
            return "";
        return value.replace("\"", "\"\"");
    }
}
