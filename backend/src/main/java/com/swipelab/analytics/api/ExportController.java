package com.swipelab.analytics.api;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.swipelab.analytics.application.ExportService;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/export")
@RequiredArgsConstructor
@PreAuthorize("hasRole('RESEARCHER') or @securityAuthorizationService.isSuperAdmin(authentication.name)")
public class ExportController {

    private final ExportService exportService;

    /**
     * Get export summary before downloading.
     */
    @GetMapping("/tasks/{taskId}/summary")
    public ResponseEntity<Map<String, Object>> getExportSummary(@PathVariable Long taskId) {
        Map<String, Object> summary = exportService.getExportSummary(taskId);
        return ResponseEntity.ok(summary);
    }

    /**
     * Export classifications as CSV with streaming.
     */
    @GetMapping("/tasks/{taskId}/csv")
    public void exportAsCsv(
            @PathVariable Long taskId,
            HttpServletResponse response) throws IOException {

        response.setContentType("text/csv");
        response.setCharacterEncoding("UTF-8");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"task_" + taskId + "_classifications.csv\"");

        exportService.exportClassificationsAsCsv(taskId, response.getOutputStream());
    }

    /**
     * Export classifications as JSON.
     */
    @GetMapping("/tasks/{taskId}/json")
    public ResponseEntity<List<Map<String, Object>>> exportAsJson(@PathVariable Long taskId) {
        List<Map<String, Object>> data = exportService.exportClassificationsAsJson(taskId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"task_" + taskId + "_classifications.json\"")
                .body(data);
    }
}
