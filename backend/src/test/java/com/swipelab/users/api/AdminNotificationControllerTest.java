package com.swipelab.users.api;

import com.swipelab.dto.response.AdminNotificationResponse;
import com.swipelab.exception.ResourceNotFoundException;
import com.swipelab.users.application.AdminNotificationService;
import com.swipelab.users.domain.NotificationSeverity;
import com.swipelab.users.domain.NotificationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Standalone MockMvc unit tests for AdminNotificationController.
 *
 * Security enforcement (@PreAuthorize) relies on Spring's ApplicationContext to resolve
 * @beans, which is not available in standalone setup. We disable method-security evaluation
 * here; authorization is covered by integration tests against the full security config.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AdminNotificationController")
class AdminNotificationControllerTest {

    private MockMvc mockMvc;

    @Mock private AdminNotificationService adminNotificationService;

    @InjectMocks
    private AdminNotificationController adminNotificationController;

    private AdminNotificationResponse sampleNotification;

    @BeforeEach
    void setUp() {
        // Standalone — @PreAuthorize requires ApplicationContext; not available here.
        // Security-level access control is tested via integration tests.
        // Disable method security by registering a no-op handler interceptor override:
        // the DispatcherServlet in standaloneSetup does NOT load AOP proxies for @PreAuthorize.
        mockMvc = MockMvcBuilders.standaloneSetup(adminNotificationController).build();

        sampleNotification = AdminNotificationResponse.builder()
                .id(1L)
                .type(NotificationType.USER_WARNED)
                .severity(NotificationSeverity.WARNING)
                .title("Suspicious labeler warned: testuser")
                .message("User received WARNING_1")
                .targetUsername("testuser")
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ── GET /api/admin/notifications ──────────────────────────────────────────

    @Test
    @DisplayName("GET / → 200 with paginated notifications")
    void getNotifications_returnsPage() throws Exception {
        when(adminNotificationService.getNotifications(any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(sampleNotification), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/admin/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].type").value("USER_WARNED"))
                .andExpect(jsonPath("$.content[0].severity").value("WARNING"))
                .andExpect(jsonPath("$.content[0].isRead").value(false));
    }

    @Test
    @DisplayName("GET / with isRead=false filter → delegates filter to service")
    void getNotifications_withReadFilter_delegatesToService() throws Exception {
        when(adminNotificationService.getNotifications(eq(false), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(sampleNotification), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/admin/notifications").param("isRead", "false"))
                .andExpect(status().isOk());

        verify(adminNotificationService).getNotifications(eq(false), isNull(), isNull(), any());
    }

    // ── GET /api/admin/notifications/unread-count ─────────────────────────────

    @Test
    @DisplayName("GET /unread-count → returns count as JSON")
    void getUnreadCount_returnsCount() throws Exception {
        when(adminNotificationService.getUnreadCount()).thenReturn(7L);

        mockMvc.perform(get("/api/admin/notifications/unread-count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(7));
    }

    // ── PATCH /api/admin/notifications/{id}/read ──────────────────────────────

    @Test
    @DisplayName("PATCH /{id}/read → 204 No Content")
    void markAsRead_returns204() throws Exception {
        when(adminNotificationService.markAsRead(1L)).thenReturn(null);

        mockMvc.perform(patch("/api/admin/notifications/1/read"))
                .andExpect(status().isNoContent());

        verify(adminNotificationService).markAsRead(1L);
    }

    @Test
    @DisplayName("PATCH /{id}/read on unknown id → ResourceNotFoundException → standalone returns 500")
    void markAsRead_unknownId_throws() throws Exception {
        when(adminNotificationService.markAsRead(99L))
                .thenThrow(new ResourceNotFoundException("Notification not found: 99"));

        // The global @ControllerAdvice maps ResourceNotFoundException to 404.
        mockMvc.perform(patch("/api/admin/notifications/99/read"))
                .andExpect(status().is4xxClientError());
    }

    // ── PATCH /api/admin/notifications/read-all ───────────────────────────────

    @Test
    @DisplayName("PATCH /read-all → 200 with updated count")
    void markAllAsRead_returnsUpdatedCount() throws Exception {
        when(adminNotificationService.markAllAsRead()).thenReturn(5);

        mockMvc.perform(patch("/api/admin/notifications/read-all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updated").value(5));
    }
}
