package com.swipelab.analytics.application;

import com.swipelab.auth.application.SecurityAuthorizationService;
import com.swipelab.classification.domain.core.Classification;
import com.swipelab.classification.domain.core.Classification.UserResponse;
import com.swipelab.classification.domain.image.Image;
import com.swipelab.classification.infrastructure.ClassificationRepository;
import com.swipelab.classification.infrastructure.GoldImageRepository;
import com.swipelab.classification.infrastructure.ImageRepository;
import com.swipelab.exception.ResourceNotFoundException;
import com.swipelab.tasks.domain.Task;
import com.swipelab.tasks.infrastructure.TaskRepository;
import com.swipelab.users.domain.User;
import com.swipelab.users.infrastructure.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExportServiceTest {

    @Mock private ClassificationRepository classificationRepository;
    @Mock private ImageRepository imageRepository;
    @Mock private GoldImageRepository goldImageRepository;
    @Mock private TaskRepository taskRepository;
    @Mock private UserRepository userRepository;
    @Mock private SecurityAuthorizationService securityAuthorizationService;

    @InjectMocks
    private ExportService exportService;

    private Task task1;
    private Task task2;
    private Image image1;
    private Classification classification1;
    private User user1;

    @BeforeEach
    void setUp() {
        task1 = Task.builder().id(1L).name("Bird Survey").createdBy("researcher1").build();
        task2 = Task.builder().id(2L).name("Fish Study").createdBy("researcher1").build();

        image1 = Image.builder().id(10L).parentImageId(999L).externalBoxId(1234L).imageData("/images/bird1.jpg").taskId(1L).build();

        classification1 = Classification.builder()
                .id(100L)
                .taskId(1L)
                .username("labeler1")
                .userRole("USER")
                .querySpecies("Parus major")
                .userResponse(UserResponse.YES)
                .image(image1)
                .createdAt(LocalDateTime.of(2025, 6, 15, 10, 30, 0))
                .build();

        user1 = User.builder().username("labeler1").credibilityScore(0.85).build();
    }

    // ─── Happy flows ─────────────────────────────────────────────────────────

    @Test
    void exportMultiTaskCsv_happyFlow_researcherOwnsTasks() throws IOException {
        List<Long> taskIds = List.of(1L, 2L);

        when(taskRepository.findAllById(taskIds)).thenReturn(List.of(task1, task2));
        when(securityAuthorizationService.isSuperAdmin("researcher1")).thenReturn(false);
        when(classificationRepository.findByTaskIdIn(taskIds)).thenReturn(List.of(classification1));
        when(userRepository.findByUsernameIn(Set.of("labeler1"))).thenReturn(Set.of(user1));
        when(goldImageRepository.existsByImageId(10L)).thenReturn(false);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        exportService.exportMultiTaskClassificationsAsCsv(taskIds, "researcher1", outputStream);

        String csv = outputStream.toString();
        String[] lines = csv.split("\n");

        // Header + 1 data row
        assertEquals(2, lines.length);
        assertTrue(lines[0].trim().startsWith("classification_id,task_id,task_name"));
        assertTrue(lines[1].contains("100"));        // classification_id
        assertTrue(lines[1].contains("Bird Survey")); // task_name
        assertTrue(lines[1].contains("999"));         // parent_image_id
        assertTrue(lines[1].contains("1234"));        // crop_id
        assertTrue(lines[1].contains("labeler1"));     // username
        assertTrue(lines[1].contains("0.85"));         // credibility_score
        assertTrue(lines[1].contains("YES"));          // user_response
        assertTrue(lines[1].contains("false"));        // is_gold_standard
    }

    @Test
    void exportMultiTaskCsv_happyFlow_superAdminExportsAnyTask() throws IOException {
        Task otherTask = Task.builder().id(3L).name("Other Study").createdBy("someone_else").build();
        List<Long> taskIds = List.of(3L);

        when(taskRepository.findAllById(taskIds)).thenReturn(List.of(otherTask));
        when(securityAuthorizationService.isSuperAdmin("admin")).thenReturn(true);
        when(classificationRepository.findByTaskIdIn(taskIds)).thenReturn(List.of());
        // No users to fetch when there are no classifications

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        exportService.exportMultiTaskClassificationsAsCsv(taskIds, "admin", outputStream);

        String csv = outputStream.toString();
        String[] lines = csv.split("\n");

        // Header only, no data rows
        assertEquals(1, lines.length);
        assertTrue(lines[0].trim().startsWith("classification_id,task_id,task_name"));
    }

    // ─── Edge cases ──────────────────────────────────────────────────────────

    @Test
    void exportMultiTaskCsv_edgeCase_unauthorizedTask_throwsAccessDenied() {
        Task foreignTask = Task.builder().id(5L).name("Secret Study").createdBy("other_researcher").build();
        List<Long> taskIds = List.of(5L);

        when(taskRepository.findAllById(taskIds)).thenReturn(List.of(foreignTask));
        when(securityAuthorizationService.isSuperAdmin("researcher1")).thenReturn(false);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        AccessDeniedException ex = assertThrows(AccessDeniedException.class, () ->
                exportService.exportMultiTaskClassificationsAsCsv(taskIds, "researcher1", outputStream));

        assertTrue(ex.getMessage().contains("Secret Study"));
    }

    @Test
    void exportMultiTaskCsv_edgeCase_nonExistentTaskIds_throwsNotFound() {
        List<Long> taskIds = List.of(1L, 999L);

        // Only task 1 found, task 999 does not exist
        when(taskRepository.findAllById(taskIds)).thenReturn(List.of(task1));

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        assertThrows(ResourceNotFoundException.class, () ->
                exportService.exportMultiTaskClassificationsAsCsv(taskIds, "researcher1", outputStream));
    }

    @Test
    void exportMultiTaskCsv_edgeCase_noClassifications_headerOnly() throws IOException {
        List<Long> taskIds = List.of(1L);

        when(taskRepository.findAllById(taskIds)).thenReturn(List.of(task1));
        when(securityAuthorizationService.isSuperAdmin("researcher1")).thenReturn(false);
        when(classificationRepository.findByTaskIdIn(taskIds)).thenReturn(List.of());

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        exportService.exportMultiTaskClassificationsAsCsv(taskIds, "researcher1", outputStream);

        String csv = outputStream.toString();
        String[] lines = csv.trim().split("\n");

        // Only the header row
        assertEquals(1, lines.length);
        assertTrue(lines[0].trim().startsWith("classification_id,task_id,task_name"));
    }

    @Test
    void exportMultiTaskCsv_edgeCase_sharedResearcherCanExport() throws IOException {
        Task sharedTask = Task.builder()
                .id(7L).name("Shared Study").createdBy("other_researcher")
                .sharedWithResearchers(List.of("researcher1"))
                .build();
        List<Long> taskIds = List.of(7L);

        when(taskRepository.findAllById(taskIds)).thenReturn(List.of(sharedTask));
        when(securityAuthorizationService.isSuperAdmin("researcher1")).thenReturn(false);
        when(classificationRepository.findByTaskIdIn(taskIds)).thenReturn(List.of());

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        // Should NOT throw — researcher1 is in sharedWithResearchers
        assertDoesNotThrow(() ->
                exportService.exportMultiTaskClassificationsAsCsv(taskIds, "researcher1", outputStream));
    }
}
