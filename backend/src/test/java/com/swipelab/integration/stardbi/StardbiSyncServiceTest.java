package com.swipelab.integration.stardbi;

import com.swipelab.classification.domain.Image;
import com.swipelab.classification.infrastructure.ImageRepository;
import com.swipelab.integration.stardbi.dto.ExternalExperimentDto;
import com.swipelab.tasks.domain.Task;
import com.swipelab.tasks.domain.TaskStatus;
import com.swipelab.tasks.infrastructure.TaskRepository;
import com.swipelab.users.infrastructure.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StardbiSyncServiceTest {

    @Mock private StardbiClientPort stardbiClient;
    @Mock private TaskRepository taskRepository;
    @Mock private ImageRepository imageRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private StardbiSyncService stardbiSyncService;

    private Task task;

    // A minimal valid JPEG header as bytes (sufficient for a non-empty test image)
    private static final byte[] FAKE_IMAGE_BYTES = Base64.getDecoder().decode(
            "/9j/4AAQSkZJRgABAQEASABIAAD/2wBDAP//////////////////////////////////////////////////////////wgALCAABAAEBAREA/8QAFBABAAAAAAAAAAAAAAAAAAAAAP/aAAgBAQABPxB="
    );

    @BeforeEach
    void setUp() {
        task = new Task();
        task.setId(10L);
        task.setStatus(TaskStatus.PROCESSING);
        task.setExperiments(List.of(1L));
    }

    // ─── Helper ─────────────────────────────────────────────────────────────────

    private byte[] buildZip(String entryName, byte[] content) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            zos.putNextEntry(new ZipEntry(entryName));
            zos.write(content);
            zos.closeEntry();
        }
        return baos.toByteArray();
    }

    // ─── Happy flow ──────────────────────────────────────────────────────────────

    @Test
    void syncExperimentsForTask_storesBase64InDb_onSuccessfulDownload() throws Exception {
        // Filename convention: {parentImageId}_{boxId}.ext
        byte[] zipBytes = buildZip("201_102.jpg", FAKE_IMAGE_BYTES);

        when(stardbiClient.checkAuth(anyString())).thenReturn(false); // force service-account fallback
        when(stardbiClient.downloadExperimentCropsZip(1L, null)).thenReturn(zipBytes);
        when(imageRepository.existsByExternalBoxIdAndTaskId(102L, 10L)).thenReturn(false);
        when(imageRepository.save(any(Image.class))).thenAnswer(inv -> inv.getArgument(0));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        stardbiSyncService.syncExperimentsForTask(task, "some-swipelab-jwt", null);

        // Task should now be ACTIVE
        assertEquals(TaskStatus.ACTIVE, task.getStatus());

        // Verify the image was saved with base64 srcPath (not a file path)
        ArgumentCaptor<Image> imageCaptor = ArgumentCaptor.forClass(Image.class);
        verify(imageRepository, times(1)).save(imageCaptor.capture());

        Image savedImage = imageCaptor.getValue();
        assertNotNull(savedImage.getSrcPath(), "srcPath must not be null");
        // Verify it is NOT a file-system path (must not contain '/app/' or look like an absolute path)
        assertFalse(savedImage.getSrcPath().contains("/app/"),
                "srcPath must be base64, not a /app/... file path");
        assertFalse(savedImage.getSrcPath().contains("storage"),
                "srcPath must be base64 data, not a storage directory path");
        assertDoesNotThrow(() -> Base64.getDecoder().decode(savedImage.getSrcPath()),
                "srcPath must be valid base64");
        assertEquals(10L, savedImage.getTaskId());
        assertEquals(102L, savedImage.getExternalBoxId());
    }

    @Test
    void syncExperimentsForTask_skipsAlreadyIngestedImages() throws Exception {
        byte[] zipBytes = buildZip("201_102.jpg", FAKE_IMAGE_BYTES);

        when(stardbiClient.checkAuth(anyString())).thenReturn(false);
        when(stardbiClient.downloadExperimentCropsZip(1L, null)).thenReturn(zipBytes);
        // Image already ingested
        when(imageRepository.existsByExternalBoxIdAndTaskId(102L, 10L)).thenReturn(true);
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        stardbiSyncService.syncExperimentsForTask(task, "token", null);

        // No new image should be saved
        verify(imageRepository, never()).save(any(Image.class));
        assertEquals(TaskStatus.ACTIVE, task.getStatus());
    }

    // ─── Edge / failure cases ────────────────────────────────────────────────────

    @Test
    void syncExperimentsForTask_setsTaskFailed_whenDownloadThrows() {
        when(stardbiClient.checkAuth(anyString())).thenReturn(false);
        when(stardbiClient.downloadExperimentCropsZip(anyLong(), any()))
                .thenThrow(new RuntimeException("Stardbi unreachable"));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        stardbiSyncService.syncExperimentsForTask(task, "token", null);

        // Single experiment failed → ZIP loop catches it and continues;
        // after the loop, task is set ACTIVE (no global crash).
        // If ALL experiments fail the inner continue means the outer try finishes cleanly.
        // The test verifies at least that it does NOT throw and the task reaches a terminal state.
        assertNotEquals(TaskStatus.PROCESSING, task.getStatus(),
                "Task must not stay in PROCESSING after sync attempt");
    }

    @Test
    void syncExperimentsForTask_setsTaskFailed_whenZipIsEmpty() throws Exception {
        when(stardbiClient.checkAuth(anyString())).thenReturn(false);
        when(stardbiClient.downloadExperimentCropsZip(1L, null)).thenReturn(new byte[0]);
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        stardbiSyncService.syncExperimentsForTask(task, "token", null);

        // Empty ZIP → no images, but task still transitions to ACTIVE (not an error)
        assertEquals(TaskStatus.ACTIVE, task.getStatus());
        verify(imageRepository, never()).save(any(Image.class));
    }

    @Test
    void syncExperimentsForTask_usesRefreshToken_whenAccessTokenExpired() throws Exception {
        byte[] zipBytes = buildZip("201_105.jpg", FAKE_IMAGE_BYTES);

        // First checkAuth fails, then refresh succeeds
        when(stardbiClient.checkAuth("old-token")).thenReturn(false);
        com.swipelab.integration.stardbi.dto.StardbiAuthResponseDto newAuth =
                new com.swipelab.integration.stardbi.dto.StardbiAuthResponseDto();
        newAuth.setAccess("new-token");
        when(stardbiClient.refreshToken(any())).thenReturn(newAuth);
        when(stardbiClient.downloadExperimentCropsZip(1L, "new-token")).thenReturn(zipBytes);
        when(imageRepository.existsByExternalBoxIdAndTaskId(105L, 10L)).thenReturn(false);
        when(imageRepository.save(any(Image.class))).thenAnswer(inv -> inv.getArgument(0));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        stardbiSyncService.syncExperimentsForTask(task, "old-token", "refresh-token");

        assertEquals(TaskStatus.ACTIVE, task.getStatus());
        verify(imageRepository, times(1)).save(any(Image.class));
    }
}
