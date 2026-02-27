package com.swipelab.tasks.application;

import com.swipelab.dto.request.CreateTaskRequest;
import com.swipelab.dto.request.UpdateTaskRequest;
import com.swipelab.dto.response.TaskPageResponse;
import com.swipelab.dto.response.TaskResponse;
import com.swipelab.exception.ResourceNotFoundException;
import com.swipelab.recipients.domain.RecipientGroup;
import com.swipelab.recipients.domain.RecipientUser;
import com.swipelab.recipients.infrastructure.RecipientGroupRepository;
import com.swipelab.tasks.domain.Task;
import com.swipelab.tasks.domain.TaskMapper;
import com.swipelab.tasks.domain.TaskStatus;
import com.swipelab.tasks.infrastructure.TaskRepository;
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
    private TaskMapper taskMapper;

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
        
        when(taskRepository.findByStatusAndRecipientGroupsIn(eq(TaskStatus.ACTIVE), anySet(), eq(pageable))).thenReturn(taskPage);
        when(taskMapper.toResponse(any(Task.class), eq(true))).thenReturn(taskResponse);

        TaskPageResponse response = taskService.getTasksForUser("testuser", pageable);

        assertNotNull(response);
        assertEquals(1, response.getTotalTasks());
        assertEquals("Test Task", response.getTasks().get(0).getName());
    }

    @Test
    void getTasksForUser_ShouldReturnEmpty_WhenNoGroups() {
        when(recipientGroupRepository.findByUsers_Username("testuser")).thenReturn(Collections.emptyList());
        
        Pageable pageable = PageRequest.of(0, 20);

        TaskPageResponse response = taskService.getTasksForUser("testuser", pageable);

        assertNotNull(response);
        assertEquals(0, response.getTotalTasks());
        verify(taskRepository, never()).findByStatusAndRecipientGroupsIn(any(), any(), any());
    }

    @Test
    void getTaskForUser_ShouldReturnTask_WhenActiveAndAssigned() {
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(recipientGroupRepository.findByUsers_Username("testuser")).thenReturn(Collections.singletonList(group));
        when(taskMapper.toResponse(task, true)).thenReturn(taskResponse);

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
    void getAdminDashboard_ShouldReturnAllTasks() {
        when(taskRepository.findAll()).thenReturn(Collections.singletonList(task));
        when(taskMapper.toResponse(task, false)).thenReturn(taskResponse);

        List<TaskResponse> responses = taskService.getAdminDashboard();

        assertEquals(1, responses.size());
    }

    @Test
    void createTask_ShouldSetStatusActiveAndSave() {
        CreateTaskRequest request = new CreateTaskRequest();
        when(taskMapper.toEntity(request)).thenReturn(task);
        when(taskRepository.save(task)).thenReturn(task);
        when(taskMapper.toResponse(task, false)).thenReturn(taskResponse);

        TaskResponse response = taskService.createTask(request);

        assertNotNull(response);
        assertEquals(TaskStatus.ACTIVE, task.getStatus());
        verify(taskRepository, times(1)).save(task);
    }

    @Test
    void archiveTask_ShouldSetStatusArchived() {
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(taskMapper.toResponse(task, false)).thenReturn(taskResponse);

        taskService.archiveTask(1L);

        assertEquals(TaskStatus.ARCHIVED, task.getStatus());
    }

    @Test
    void activateTask_ShouldSetStatusActive() {
        task.setStatus(TaskStatus.PAUSED);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(taskMapper.toResponse(task, false)).thenReturn(taskResponse);

        taskService.activateTask(1L);

        assertEquals(TaskStatus.ACTIVE, task.getStatus());
    }

    @Test
    void pauseTask_ShouldSetStatusPaused() {
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(taskMapper.toResponse(task, false)).thenReturn(taskResponse);

        taskService.pauseTask(1L);

        assertEquals(TaskStatus.PAUSED, task.getStatus());
    }

    @Test
    void updateTask_ShouldUpdateFields() {
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        UpdateTaskRequest request = new UpdateTaskRequest();
        when(taskMapper.toResponse(task, false)).thenReturn(taskResponse);

        taskService.updateTask(1L, request);

        verify(taskMapper, times(1)).updateEntity(task, request);
    }
}
