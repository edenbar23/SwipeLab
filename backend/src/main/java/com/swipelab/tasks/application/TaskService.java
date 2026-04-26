package com.swipelab.tasks.application;

import com.swipelab.dto.request.CreateTaskRequest;
import com.swipelab.dto.request.UpdateTaskRequest;
import com.swipelab.dto.response.TaskPageResponse;
import com.swipelab.dto.response.TaskResponse;
import com.swipelab.exception.ResourceNotFoundException;
import com.swipelab.recipients.domain.RecipientGroup;
import com.swipelab.recipients.infrastructure.RecipientGroupRepository;
import com.swipelab.tasks.domain.Task;
import com.swipelab.tasks.domain.TaskMapper;
import com.swipelab.tasks.domain.TaskStatus;
import com.swipelab.tasks.infrastructure.TaskRepository;
import com.swipelab.classification.domain.Label;
import com.swipelab.classification.infrastructure.LabelRepository;
import com.swipelab.integration.stardbi.StardbiSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final RecipientGroupRepository recipientGroupRepository;
    private final LabelRepository labelRepository;
    private final TaskMapper taskMapper;
    private final StardbiSyncService stardbiSyncService;

    // =========================
    // User Operations
    // =========================

    @Transactional(readOnly = true)
    public TaskPageResponse getTasksForUser(String username, Pageable pageable) {
        List<RecipientGroup> userGroups = recipientGroupRepository.findByUsers_Username(username);
        Set<Long> groupIds = userGroups.stream()
                .map(RecipientGroup::getId)
                .collect(Collectors.toSet());

        if (groupIds.isEmpty()) {
            groupIds = Set.of(-1L);
        }

        Page<Task> taskPage = taskRepository.findAccessibleTasksForUser(
                TaskStatus.ACTIVE,
                username,
                groupIds,
                pageable);

        List<TaskResponse> taskResponses = taskPage.getContent().stream()
                .map(task -> taskMapper.toResponse(task, true)) // assignedToUser = true
                .collect(Collectors.toList());

        return TaskPageResponse.builder()
                .page(taskPage.getNumber() + 1) // 1-indexed for response
                .pageSize(taskPage.getSize())
                .totalPages(taskPage.getTotalPages())
                .totalTasks(taskPage.getTotalElements())
                .tasks(taskResponses)
                .build();
    }

    @Transactional(readOnly = true)
    public TaskResponse getTaskForUser(Long taskId, String username) {
        Task task = getTask(taskId);

        // 1. Must be ACTIVE
        if (!task.isActive()) {
            throw new ResourceNotFoundException("Task not found or not active");
        }

        // 2. Must be assigned to user
        List<RecipientGroup> userGroups = recipientGroupRepository.findByUsers_Username(username);
        Set<Long> userGroupIds = userGroups.stream()
                .map(RecipientGroup::getId)
                .collect(Collectors.toSet());

        boolean isAssigned = false;
        
        if (Boolean.TRUE.equals(task.getIsPublic())) {
            isAssigned = true;
        } else if (task.getAssignedUsernames() != null && task.getAssignedUsernames().contains(username)) {
            isAssigned = true;
        } else if (task.getRecipientGroups() != null) {
            for (Long taskGroupId : task.getRecipientGroups()) {
                if (userGroupIds.contains(taskGroupId)) {
                    isAssigned = true;
                    break;
                }
            }
        }

        if (!isAssigned) {
            // Treat as not found for security/privacy
            throw new ResourceNotFoundException("Task not found or access denied");
        }

        return taskMapper.toResponse(task, true);
    }

    // =========================
    // Admin Operations
    // =========================

    @Transactional(readOnly = true)
    public List<TaskResponse> getAdminDashboard() {
        // Returns all tasks with full details
        return taskRepository.findAll().stream()
                .map(task -> taskMapper.toResponse(task, false))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TaskResponse getTaskDetailsAdmin(Long taskId) {
        return mapToResponse(getTask(taskId));
    }

    @Transactional
    public TaskResponse createTask(CreateTaskRequest request, String username, String stardbiAccessToken, String stardbiRefreshToken) {
        // TODO: Validate author (admin)
        Task task = taskMapper.toEntity(request);
        task.setCreatedBy(username);
        task.setStatus(TaskStatus.PROCESSING);
        
        if (request.getTargetSpecies() != null && !request.getTargetSpecies().isEmpty()) {
            List<Label> labels = new ArrayList<>();
            for (com.swipelab.dto.request.TargetSpeciesRequest tsr : request.getTargetSpecies()) {
                Label label = labelRepository.findByName(tsr.getName())
                        .orElseGet(() -> labelRepository.save(Label.builder().name(tsr.getName()).build()));
                labels.add(label);
            }
            task.setTargetSpecies(labels);
        }
        
        task = taskRepository.save(task);
        
        // Trigger a background sync using the user's Stardbi token
        final Task savedTask = task;
        CompletableFuture.runAsync(() -> stardbiSyncService.syncExperimentsForTask(savedTask, stardbiAccessToken, stardbiRefreshToken));
        
        return mapToResponse(task);
    }

    @Transactional
    public TaskResponse archiveTask(Long taskId) {
        Task task = getTask(taskId);
        task.archive();
        return mapToResponse(task);
    }

    @Transactional
    public TaskResponse activateTask(Long taskId) {
        Task task = getTask(taskId);
        task.activate();
        return mapToResponse(task);
    }

    @Transactional
    public TaskResponse pauseTask(Long taskId) {
        Task task = getTask(taskId);
        task.pause();
        return mapToResponse(task);
    }

    @Transactional
    public TaskResponse updateTask(Long taskId, UpdateTaskRequest request) {
        Task task = getTask(taskId);

        // If updating status via update payload is supported, handle it,
        // but explicit endpoints (activate/pause/archive) are preferred.
        // We focus on fields here.

        taskMapper.updateEntity(task, request);
        
        if (request.getTargetSpecies() != null) {
            List<Label> labels = new ArrayList<>();
            for (com.swipelab.dto.request.TargetSpeciesRequest tsr : request.getTargetSpecies()) {
                Label label = labelRepository.findByName(tsr.getName())
                        .orElseGet(() -> labelRepository.save(Label.builder().name(tsr.getName()).build()));
                labels.add(label);
            }
            task.setTargetSpecies(labels);
        }
        
        // Trigger a background sync in case task external experiments were added
        CompletableFuture.runAsync(stardbiSyncService::syncExperiments);
        
        return mapToResponse(task);
    }

    // =========================
    // Helpers
    // =========================

    private Task getTask(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));
    }

    private TaskResponse mapToResponse(Task task) {
        return taskMapper.toResponse(task, false);
    }

    // Legacy support if needed, but preferably use new methods
    public List<TaskResponse> getAllTasks() {
        return getAdminDashboard();
    }

    public List<TaskResponse> getActiveTasks() {
        return taskRepository.findByStatus(TaskStatus.ACTIVE).stream()
                .map(t -> taskMapper.toResponse(t, false))
                .collect(Collectors.toList());
    }

    public TaskResponse getTaskById(Long id) {
        return mapToResponse(getTask(id));
    }

    public List<TaskResponse> getTasksByUser(String username) {
        return taskRepository.findByCreatedBy(username).stream()
                .map(taskMapper::toResponse)
                .collect(Collectors.toList());
    }

}
