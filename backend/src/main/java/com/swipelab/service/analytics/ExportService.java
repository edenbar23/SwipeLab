package com.swipelab.service.analytics;

import com.swipelab.classification.domain.Classification;
import com.swipelab.classification.domain.Image;
import com.swipelab.classification.infrastructure.ClassificationRepository;
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

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final String CSV_HEADER = "image_id,image_url,username,label,classified_at,is_gold_standard";

    /**
     * Export classifications for a task as CSV with streaming support.
     * Uses streaming to handle large datasets efficiently.
     */
    @Transactional(readOnly = true)
    public void exportClassificationsAsCsv(Long taskId, OutputStream outputStream) throws IOException {
        List<Image> images = imageRepository.findByTaskId(taskId);

        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8))) {
            writer.println(CSV_HEADER);

            for (Image image : images) {
                List<Classification> classifications = classificationRepository.findByImageId(image.getId());

                for (Classification classification : classifications) {
                    writer.println(formatCsvRow(classification));
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
            List<Classification> classifications = classificationRepository.findByImageId(image.getId());

            for (Classification classification : classifications) {
                result.add(formatJsonObject(classification));
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

    private String formatCsvRow(Classification classification) {
        Image image = classification.getImage();
        return String.format("%d,\"%s\",\"%s\",\"%s\",\"%s\",%b",
                image.getId(),
                escapeCsv(image.getImageUrl()),
                escapeCsv(classification.getUser().getUsername()),
                escapeCsv(classification.getLabel().getName()),
                classification.getCreatedAt() != null ? classification.getCreatedAt().format(DATE_FORMATTER) : "",
                image.getIsGoldStandard());
    }

    private Map<String, Object> formatJsonObject(Classification classification) {
        Image image = classification.getImage();
        Map<String, Object> obj = new HashMap<>();
        obj.put("imageId", image.getId());
        obj.put("imageUrl", image.getImageUrl());
        obj.put("username", classification.getUser().getUsername());
        obj.put("label", classification.getLabel().getName());
        obj.put("classifiedAt",
                classification.getCreatedAt() != null ? classification.getCreatedAt().toString() : null);
        obj.put("isGoldStandard", image.getIsGoldStandard());
        return obj;
    }

    private String escapeCsv(String value) {
        if (value == null)
            return "";
        return value.replace("\"", "\"\"");
    }
}
