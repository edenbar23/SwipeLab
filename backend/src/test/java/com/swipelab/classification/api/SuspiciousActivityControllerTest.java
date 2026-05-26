package com.swipelab.classification.api;

import com.swipelab.classification.application.SuspiciousActivityService;
import com.swipelab.classification.domain.WarningLevel;
import com.swipelab.dto.response.SuspiciousActivityResponse;
import com.swipelab.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SuspiciousActivityController")
class SuspiciousActivityControllerTest {

    private MockMvc mockMvc;

    @Mock private SuspiciousActivityService suspiciousActivityService;

    @InjectMocks
    private SuspiciousActivityController suspiciousActivityController;

    private SuspiciousActivityResponse sampleRecord;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(suspiciousActivityController).build();

        sampleRecord = SuspiciousActivityResponse.builder()
                .id(1L)
                .username("badactor")
                .reason("Fast response: 100ms (threshold 300ms)")
                .responseTimeMs(100L)
                .taskId(5L)
                .severity(WarningLevel.STRIKE)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ── GET /api/admin/suspicious-activity ───────────────────────────────────

    @Test
    @DisplayName("GET / → 200 with all records")
    void getAllActivity_returnsAllRecords() throws Exception {
        when(suspiciousActivityService.getAllActivity()).thenReturn(List.of(sampleRecord));

        mockMvc.perform(get("/api/admin/suspicious-activity"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("badactor"))
                .andExpect(jsonPath("$[0].severity").value("STRIKE"))
                .andExpect(jsonPath("$[0].responseTimeMs").value(100));

        verify(suspiciousActivityService).getAllActivity();
    }

    @Test
    @DisplayName("GET / → 200 empty list when no records")
    void getAllActivity_emptyList() throws Exception {
        when(suspiciousActivityService.getAllActivity()).thenReturn(List.of());

        mockMvc.perform(get("/api/admin/suspicious-activity"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    // ── GET /api/admin/suspicious-activity/{username} ────────────────────────

    @Test
    @DisplayName("GET /{username} → 200 with user's records")
    void getActivityForUser_returnsUserRecords() throws Exception {
        when(suspiciousActivityService.getActivityForUser("badactor"))
                .thenReturn(List.of(sampleRecord));

        mockMvc.perform(get("/api/admin/suspicious-activity/badactor"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("badactor"));

        verify(suspiciousActivityService).getActivityForUser("badactor");
    }

    // ── POST /api/admin/suspicious-activity/{username}/reset ─────────────────

    @Test
    @DisplayName("POST /{username}/reset → 200 with confirmation message")
    void resetStrikes_returnsConfirmation() throws Exception {
        doNothing().when(suspiciousActivityService).resetStrikes("badactor");

        mockMvc.perform(post("/api/admin/suspicious-activity/badactor/reset"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("badactor"))
                .andExpect(jsonPath("$.message").exists());

        verify(suspiciousActivityService).resetStrikes("badactor");
    }

    @Test
    @DisplayName("POST /{username}/reset on unknown user → exception propagated")
    void resetStrikes_unknownUser_throws() throws Exception {
        doThrow(new ResourceNotFoundException("User not found: ghost"))
                .when(suspiciousActivityService).resetStrikes("ghost");

        mockMvc.perform(post("/api/admin/suspicious-activity/ghost/reset"))
                .andExpect(status().is4xxClientError());
    }
}
