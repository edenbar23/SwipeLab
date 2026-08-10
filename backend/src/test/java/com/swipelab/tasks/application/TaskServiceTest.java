package com.swipelab.tasks.application;

import com.swipelab.tasks.dto.CreateTaskRequest;
import com.swipelab.tasks.dto.UpdateTaskRequest;
import com.swipelab.tasks.dto.TaskPageResponse;
import com.swipelab.tasks.dto.TaskProgressResponse;
import com.swipelab.tasks.dto.TaskResponse;
import com.swipelab.exception.DuplicateResourceException;
import com.swipelab.exception.ResourceNotFoundException;
import com.swipelab.recipients.domain.RecipientGroup;
import com.swipelab.recipients.domain.RecipientUser;
import com.swipelab.recipients.infrastructure.RecipientGroupRepository;
import com.swipelab.tasks.domain.Task;
import com.swipelab.tasks.domain.TaskMapper;
import com.swipelab.tasks.domain.TaskStatus;
import com.swipelab.tasks.infrastructure.TaskRepository;
import com.swipelab.classification.infrastructure.ImageRepository;
import com.swipelab.classification.infrastructure.ClassificationRepository;
import com.swipelab.classification.infrastructure.LabelRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private RecipientGroupRepository recipientGroupRepository;

    @Mock
    private LabelRepository labelRepository;

    @Mock
    private TaskMapper taskMapper;

    @Mock
    private com.swipelab.integration.stardbi.StardbiSyncService stardbiSyncService;

    @Mock
    private com.swipelab.auth.application.SecurityAuthorizationService securityAuthorizationService;

    @Mock
    private ImageRepository imageRepository;

    @Mock
    private ClassificationRepository classificationRepository;

    @InjectMocks
    private TaskService taskService;

    private Task task;
    private TaskResponse taskResponse;
    private RecipientGroup group;

    @BeforeEach
    void setUp() {
        task = new Task();
        task.setId(1L);
        task.setTitle("Test Task");
        task.setStatus(TaskStatus.ACTIVE);
        task.setRecipientGroups(Collections.singletonList(100L));

        taskResponse = TaskResponse.builder()
                .taskId(1L)
                .name("Test Task")
                .status("ACTIVE")
                .build();

        RecipientUser user = new RecipientUser();
        user.setUsername("testuser");
        
        group = new RecipientGroup();
        group.setId(100L);
        group.setName("Group 100");
        group.setUsers(new HashSet<>(Collections.singletonList(user)));
    }

    @Test
    void getTasksForUser_ShouldReturnTasks_WhenGroupsMatch() {
        when(recipientGroupRepository.findByUsers_Username("testuser")).thenReturn(Collections.singletonList(group));
        
        Pageable pageable = PageRequest.of(0, 20);
        Page<Task> taskPage = new PageImpl<>(Collections.singletonList(task), pageable, 1);
        
        when(taskRepository.findAccessibleTasksForUser(eq(TaskStatus.ACTIVE), eq("testuser"), anySet(), eq(pageable))).thenReturn(taskPage);
        when(taskMapper.toResponse(any(Task.class), eq(true), any(TaskProgressResponse.class))).thenReturn(taskResponse);

        TaskPageResponse response = taskService.getTasksForUser("testuser", pageable);

        assertNotNull(response);
        assertEquals(1, response.getTotalTasks());
        assertEquals("Test Task", response.getTasks().get(0).getName());
    }

    @Test
    void getTasksForUser_ShouldReturnEmpty_WhenNoGroups() {
        when(recipientGroupRepository.findByUsers_Username("testuser")).thenReturn(Collections.emptyList());
        
        Pageable pageable = PageRequest.of(0, 20);
        Page<Task> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);
        when(taskRepository.findAccessibleTasksForUser(eq(TaskStatus.ACTIVE), eq("testuser"), eq(Set.of(-1L)), eq(pageable))).thenReturn(emptyPage);

        TaskPageResponse response = taskService.getTasksForUser("testuser", pageable);

        assertNotNull(response);
        assertEquals(0, response.getTotalTasks());
        verify(taskRepository).findAccessibleTasksForUser(eq(TaskStatus.ACTIVE), eq("testuser"), eq(Set.of(-1L)), eq(pageable));
    }

    @Test
    void getTaskForUser_ShouldReturnTask_WhenActiveAndAssigned() {
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(recipientGroupRepository.findByUsers_Username("testuser")).thenReturn(Collections.singletonList(group));
        when(taskMapper.toResponse(eq(task), eq(true), any(TaskProgressResponse.class))).thenReturn(taskResponse);

        TaskResponse response = taskService.getTaskForUser(1L, "testuser");

        assertNotNull(response);
        assertEquals("Test Task", response.getName());
    }

    @Test
    void getTaskForUser_ShouldThrowException_WhenNotActive() {
        task.setStatus(TaskStatus.DRAFT);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        assertThrows(ResourceNotFoundException.class, () -> taskService.getTaskForUser(1L, "testuser"));
    }

    @Test
    void getTaskForUser_ShouldThrowException_WhenNotAssigned() {
        // Task has group 100, user is in group 200
        RecipientGroup group200 = new RecipientGroup();
        group200.setId(200L);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(recipientGroupRepository.findByUsers_Username("testuser")).thenReturn(Collections.singletonList(group200));

        assertThrows(ResourceNotFoundException.class, () -> taskService.getTaskForUser(1L, "testuser"));
    }

    @Test
    void getResearcherDashboard_ShouldReturnAllTasks() {
        when(securityAuthorizationService.isSuperAdmin(anyString())).thenReturn(true);
        when(taskRepository.findAll()).thenReturn(Collections.singletonList(task));
        when(imageRepository.countByTaskId(1L)).thenReturn(10L);
        when(classificationRepository.countDistinctImagesByTaskId(1L)).thenReturn(6L);
        when(taskMapper.toResponse(eq(task), eq(false), any(TaskProgressResponse.class))).thenReturn(taskResponse);

        List<TaskResponse> responses = taskService.getResearcherDashboard("superadmin");

        assertEquals(1, responses.size());
    }

    @Test
    void createTask_ShouldSetStatusActiveAndSave() {
        CreateTaskRequest request = new CreateTaskRequest();
        request.setName("New Task");
        request.setDescription("Valid desc");

        when(taskRepository.existsByCreatedByAndName(anyString(), anyString())).thenReturn(false);
        when(taskMapper.toEntity(request)).thenReturn(task);
        when(taskRepository.save(task)).thenReturn(task);
        when(imageRepository.countByTaskId(1L)).thenReturn(0L);
        when(classificationRepository.countDistinctImagesByTaskId(1L)).thenReturn(0L);
        when(taskMapper.toResponse(eq(task), eq(false), any(TaskProgressResponse.class))).thenReturn(taskResponse);

        TaskResponse response = taskService.createTask(request, "admin_mock");

        assertNotNull(response);
        assertEquals(TaskStatus.PROCESSING, task.getStatus());
        verify(taskRepository, times(1)).save(task);
    }


    
    @Test
    void createTask_ShouldThrowException_WhenNameExists() {
        CreateTaskRequest request = new CreateTaskRequest();
        request.setName("New Task");
        request.setDescription("Valid desc");
        
        when(taskRepository.existsByCreatedByAndName(anyString(), anyString())).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> taskService.createTask(request, "admin_mock"));
    }

    @Test
    void archiveTask_ShouldSetStatusArchived() {
        when(securityAuthorizationService.isSuperAdmin(anyString())).thenReturn(true);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(imageRepository.countByTaskId(1L)).thenReturn(5L);
        when(classificationRepository.countDistinctImagesByTaskId(1L)).thenReturn(3L);
        when(taskMapper.toResponse(eq(task), eq(false), any(TaskProgressResponse.class))).thenReturn(taskResponse);

        taskService.archiveTask(1L, "superadmin");

        assertEquals(TaskStatus.ARCHIVED, task.getStatus());
    }

    @Test
    void activateTask_ShouldSetStatusActive() {
        task.setStatus(TaskStatus.PAUSED);
        when(securityAuthorizationService.isSuperAdmin(anyString())).thenReturn(true);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(imageRepository.countByTaskId(1L)).thenReturn(5L);
        when(classificationRepository.countDistinctImagesByTaskId(1L)).thenReturn(2L);
        when(taskMapper.toResponse(eq(task), eq(false), any(TaskProgressResponse.class))).thenReturn(taskResponse);

        taskService.activateTask(1L, "superadmin");

        assertEquals(TaskStatus.ACTIVE, task.getStatus());
    }

    @Test
    void pauseTask_ShouldSetStatusPaused() {
        when(securityAuthorizationService.isSuperAdmin(anyString())).thenReturn(true);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(imageRepository.countByTaskId(1L)).thenReturn(5L);
        when(classificationRepository.countDistinctImagesByTaskId(1L)).thenReturn(1L);
        when(taskMapper.toResponse(eq(task), eq(false), any(TaskProgressResponse.class))).thenReturn(taskResponse);

        taskService.pauseTask(1L, "superadmin");

        assertEquals(TaskStatus.PAUSED, task.getStatus());
    }

    @Test
    void updateTask_ShouldUpdateFields() {
        when(securityAuthorizationService.isSuperAdmin(anyString())).thenReturn(true);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        UpdateTaskRequest request = new UpdateTaskRequest();
        when(imageRepository.countByTaskId(1L)).thenReturn(5L);
        when(classificationRepository.countDistinctImagesByTaskId(1L)).thenReturn(0L);
        when(taskMapper.toResponse(eq(task), eq(false), any(TaskProgressResponse.class))).thenReturn(taskResponse);

        taskService.updateTask(1L, request, "superadmin");

        verify(taskMapper, times(1)).updateEntity(task, request);
    }

    // ===================================================
    // Issue #205 — Explore exclusion & assign guard tests
    // ===================================================

    @Test
    void getExploreTasksForUser_ShouldExcludeAlreadyAssignedTasks() {
        // group 100 is the user's group; the query is expected to exclude tasks the user can access
        when(recipientGroupRepository.findByUsers_Username("testuser")).thenReturn(Collections.singletonList(group));

        Pageable pageable = PageRequest.of(0, 20);
        // Simulate the DB already filtering: result is an empty page (all public tasks were already assigned)
        Page<Task> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);
        when(taskRepository.findPublicTasksExcludingAssignedUser(
                eq(TaskStatus.ACTIVE), eq("testuser"), anySet(), eq(pageable)))
                .thenReturn(emptyPage);

        TaskPageResponse response = taskService.getExploreTasksForUser("testuser", pageable);

        assertNotNull(response);
        assertEquals(0, response.getTotalTasks());
        verify(taskRepository).findPublicTasksExcludingAssignedUser(
                eq(TaskStatus.ACTIVE), eq("testuser"), anySet(), eq(pageable));
    }

    @Test
    void assignTaskToUser_ShouldThrowDuplicateException_WhenAlreadyAssigned() {
        task.setIsPublic(true);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        // Simulate the user already being in task_assigned_users
        when(taskRepository.existsByIdAndUsernameInAssignedUsers(1L, "testuser")).thenReturn(true);

        assertThrows(DuplicateResourceException.class,
                () -> taskService.assignTaskToUser(1L, "testuser"));

        verify(taskRepository, never()).save(any());
    }

    @Test
    void assignTaskToUser_ShouldAddUsername_WhenNotYetAssigned() {
        task.setIsPublic(true);
        task.setAssignedUsernames(new ArrayList<>());
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(taskRepository.existsByIdAndUsernameInAssignedUsers(1L, "testuser")).thenReturn(false);
        when(taskMapper.toResponse(eq(task), eq(true), any(TaskProgressResponse.class))).thenReturn(taskResponse);

        TaskResponse response = taskService.assignTaskToUser(1L, "testuser");

        assertNotNull(response);
        assertTrue(task.getAssignedUsernames().contains("testuser"));
    }

    // ===================================================
    // buildProgress — image count correctness
    // ===================================================

    @Test
    void getResearcherDashboard_ShouldReflectRealImageCounts_WhenImagesExist() {
        // Happy flow: task has 10 images, 6 have been classified
        when(securityAuthorizationService.isSuperAdmin(anyString())).thenReturn(true);
        when(taskRepository.findAll()).thenReturn(Collections.singletonList(task));
        when(imageRepository.countByTaskId(1L)).thenReturn(10L);
        when(classificationRepository.countDistinctImagesByTaskId(1L)).thenReturn(6L);

        TaskProgressResponse[] captured = new TaskProgressResponse[1];
        when(taskMapper.toResponse(eq(task), eq(false), any(TaskProgressResponse.class)))
                .thenAnswer(inv -> {
                    captured[0] = inv.getArgument(2);
                    return taskResponse;
                });

        taskService.getResearcherDashboard("superadmin");

        assertNotNull(captured[0]);
        assertEquals(10, captured[0].getTotalImages(),   "totalImages should be 10");
        assertEquals(6,  captured[0].getImagesClassified(), "imagesClassified should be 6");
    }

    @Test
    void getResearcherDashboard_ShouldReturnZeroProgress_WhenNoImagesExist() {
        // Edge case: brand-new task with no images synced yet
        when(securityAuthorizationService.isSuperAdmin(anyString())).thenReturn(true);
        when(taskRepository.findAll()).thenReturn(Collections.singletonList(task));
        when(imageRepository.countByTaskId(1L)).thenReturn(0L);
        when(classificationRepository.countDistinctImagesByTaskId(1L)).thenReturn(0L);

        TaskProgressResponse[] captured = new TaskProgressResponse[1];
        when(taskMapper.toResponse(eq(task), eq(false), any(TaskProgressResponse.class)))
                .thenAnswer(inv -> {
                    captured[0] = inv.getArgument(2);
                    return taskResponse;
                });

        taskService.getResearcherDashboard("superadmin");

        assertNotNull(captured[0]);
        assertEquals(0, captured[0].getTotalImages(),    "totalImages should be 0 for empty task");
        assertEquals(0, captured[0].getImagesClassified(), "imagesClassified should be 0 for empty task");
    }
}

