package com.swipelab.analytics.application;

import com.swipelab.classification.domain.Classification;
import com.swipelab.classification.domain.Image;
import com.swipelab.classification.infrastructure.ClassificationRepository;
import com.swipelab.classification.infrastructure.GoldImageRepository;
import com.swipelab.classification.infrastructure.ImageRepository;
import lombok.RequiredArgsConstructor;
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

@Service
@RequiredArgsConstructor
public class ExportService {

    private final ClassificationRepository classificationRepository;
    private final ImageRepository imageRepository;
    private final GoldImageRepository goldImageRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final String CSV_HEADER = "image_id,src_path,username,user_response,classified_at,is_gold_standard";

    /**
     * Export classifications for a task as CSV with streaming support.
     * Uses streaming to handle large datasets efficiently.
     */
    @Transactional(readOnly = true)
    public void exportClassificationsAsCsv(Long taskId, OutputStream outputStream) throws IOException {
        List<Image> images = imageRepository.findByTaskId(taskId);

        // Prefetch gold status for performance (if many images) - simplistic approach:
        // check existence
        // For strict optimization, would fetch all GoldImage IDs for task.
        // Assuming not massive specific performance requirement yet.

        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8))) {
            writer.println(CSV_HEADER);

            for (Image image : images) {
                boolean isGold = goldImageRepository.existsByImageId(image.getId());
                List<Classification> classifications = classificationRepository.findByImageId(image.getId());

                for (Classification classification : classifications) {
                    writer.println(formatCsvRow(classification, isGold));
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

    private String formatCsvRow(Classification classification, boolean isGoldStandard) {
        Image image = classification.getImage();
        return String.format("%d,\"%s\",\"%s\",\"%s\",\"%s\",%b",
                image.getId(),
                escapeCsv(image.getSrcPath()),
                escapeCsv(classification.getUsername()),
                escapeCsv(classification.getUserResponse().name()),
                classification.getCreatedAt() != null ? classification.getCreatedAt().format(DATE_FORMATTER) : "",
                isGoldStandard);
    }

    private Map<String, Object> formatJsonObject(Classification classification, boolean isGoldStandard) {
        Image image = classification.getImage();
        Map<String, Object> obj = new HashMap<>();
        obj.put("imageId", image.getId());
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
