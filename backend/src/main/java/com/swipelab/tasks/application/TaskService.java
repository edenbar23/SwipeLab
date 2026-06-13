package com.swipelab.tasks.application;

import com.swipelab.dto.request.CreateTaskRequest;
import com.swipelab.dto.request.UpdateTaskRequest;
import com.swipelab.dto.response.TaskPageResponse;
import com.swipelab.dto.response.TaskResponse;
import com.swipelab.exception.DuplicateResourceException;
import com.swipelab.exception.ResourceNotFoundException;
import com.swipelab.recipients.domain.RecipientGroup;
import com.swipelab.recipients.infrastructure.RecipientGroupRepository;
import com.swipelab.tasks.domain.Task;
import com.swipelab.tasks.domain.TaskMapper;
import com.swipelab.tasks.domain.TaskStatus;
import com.swipelab.tasks.infrastructure.TaskRepository;
import com.swipelab.tasks.application.port.out.TargetSpeciesProvider;
import com.swipelab.integration.stardbi.StardbiSyncService;
import com.swipelab.classification.infrastructure.ImageRepository;
import com.swipelab.classification.infrastructure.ClassificationRepository;
import com.swipelab.dto.response.TaskProgressResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;
import com.swipelab.auth.application.SecurityAuthorizationService;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final RecipientGroupRepository recipientGroupRepository;
    private final TargetSpeciesProvider targetSpeciesProvider;
    private final TaskMapper taskMapper;
    private final StardbiSyncService stardbiSyncService;
    private final SecurityAuthorizationService securityAuthorizationService;
    private final ImageRepository imageRepository;
    private final ClassificationRepository classificationRepository;


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

    @Transactional(readOnly = true)
    public TaskPageResponse getExploreTasksForUser(String username, Pageable pageable) {
        List<RecipientGroup> userGroups = recipientGroupRepository.findByUsers_Username(username);
        Set<Long> groupIds = userGroups.stream()
                .map(RecipientGroup::getId)
                .collect(Collectors.toSet());

        if (groupIds.isEmpty()) {
            groupIds = Set.of(-1L);
        }

        Page<Task> taskPage = taskRepository.findPublicTasksExcludingAssignedUser(
                TaskStatus.ACTIVE,
                username,
                groupIds,
                pageable);

        List<TaskResponse> taskResponses = taskPage.getContent().stream()
                .map(task -> taskMapper.toResponse(task, false))
                .collect(Collectors.toList());

        return TaskPageResponse.builder()
                .page(taskPage.getNumber() + 1)
                .pageSize(taskPage.getSize())
                .totalPages(taskPage.getTotalPages())
                .totalTasks(taskPage.getTotalElements())
                .tasks(taskResponses)
                .build();
    }

    @Transactional
    public TaskResponse assignTaskToUser(Long taskId, String username) {
        Task task = getTask(taskId);

        if (!task.isActive()) {
            throw new ResourceNotFoundException("Task not found or not active");
        }

        if (!Boolean.TRUE.equals(task.getIsPublic())) {
            throw new ResourceNotFoundException("Task not found or access denied");
        }

        // Idempotency guard — prevents duplicate rows in task_assigned_users
        if (taskRepository.existsByIdAndUsernameInAssignedUsers(taskId, username)) {
            throw new DuplicateResourceException("User '" + username + "' is already assigned to task " + taskId);
        }

        task.getAssignedUsernames().add(username);
        return taskMapper.toResponse(task, true);
    }

    // =========================
    // Researcher Operations
    // =========================

    @Transactional(readOnly = true)
    public List<TaskResponse> getResearcherDashboard(String username) {
        List<Task> tasks;
        if (securityAuthorizationService.isSuperAdmin(username)) {
            tasks = taskRepository.findAll();
        } else {
            tasks = taskRepository.findAll().stream()
                .filter(task -> task.getCreatedBy().equals(username) || 
                                (task.getSharedWithResearchers() != null && task.getSharedWithResearchers().contains(username)))
                .collect(Collectors.toList());
        }
        return tasks.stream()
                .map(task -> taskMapper.toResponse(task, false, buildProgress(task.getId())))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TaskResponse getTaskDetailsResearcher(Long taskId, String username) {
        Task task = getTask(taskId);
        verifyTaskAccess(task, username);
        return taskMapper.toResponse(task, false, buildProgress(taskId));
    }

    @Transactional
    public TaskResponse createTask(CreateTaskRequest request, String username, String stardbiAccessToken, String stardbiRefreshToken) {
        // Validate uniqueness
        if (taskRepository.existsByCreatedByAndName(username, request.getName().trim())) {
            throw new IllegalStateException("Task name already exists for your account");
        }



        // Sanitize Description (Stored XSS prevention)
        String sanitizedDescription = org.springframework.web.util.HtmlUtils.htmlEscape(request.getDescription().trim());

        // TODO: Validate author (admin)
        Task task = taskMapper.toEntity(request);
        task.setName(request.getName().trim());
        task.setDescription(sanitizedDescription);
        task.setCreatedBy(username);
        task.setStatus(TaskStatus.PROCESSING);
        
        if (request.getTargetSpecies() != null && !request.getTargetSpecies().isEmpty()) {
            List<String> speciesNames = request.getTargetSpecies().stream()
                    .map(com.swipelab.dto.request.TargetSpeciesRequest::getName)
                    .collect(Collectors.toList());
            List<Long> speciesIds = targetSpeciesProvider.getOrCreateSpeciesIds(speciesNames);
            task.setTargetSpeciesIds(speciesIds);
        }

        if (request.getSpeciesReferenceImageIds() != null) {
            List<Long> refImageIds = request.getSpeciesReferenceImageIds().values().stream()
                    .flatMap(List::stream)
                    .collect(Collectors.toList());
            task.setSpeciesReferenceImageIds(refImageIds);
        }
        
        task = taskRepository.save(task);
        
        // Run async so the API call returns immediately while images are ingested in the background.
        // exceptionally() ensures uncaught errors surface in the log even if the sync method's own
        // catch block somehow rethrows.
        final Task savedTask = task;
        CompletableFuture
                .runAsync(() -> stardbiSyncService.syncExperimentsForTask(savedTask, stardbiAccessToken, stardbiRefreshToken))
                .exceptionally(ex -> {
                    log.error("[TaskService] Async crop sync failed for task {}. Task may remain in PROCESSING state.", savedTask.getId(), ex);
                    return null;
                });
        
        return mapToResponse(task);
    }

    @Transactional
    public TaskResponse archiveTask(Long taskId, String username) {
        Task task = getTask(taskId);
        verifyTaskAccess(task, username);
        task.archive();
        return mapToResponse(task);
    }

    @Transactional
    public TaskResponse activateTask(Long taskId, String username) {
        Task task = getTask(taskId);
        verifyTaskAccess(task, username);
        task.activate();
        return mapToResponse(task);
    }

    @Transactional
    public TaskResponse pauseTask(Long taskId, String username) {
        Task task = getTask(taskId);
        verifyTaskAccess(task, username);
        task.pause();
        return mapToResponse(task);
    }

    @Transactional
    public TaskResponse updateTask(Long taskId, UpdateTaskRequest request, String username) {
        Task task = getTask(taskId);
        verifyTaskAccess(task, username);

        // If updating status via update payload is supported, handle it,
        // but explicit endpoints (activate/pause/archive) are preferred.
        // We focus on fields here.

        taskMapper.updateEntity(task, request);
        
        if (request.getTargetSpecies() != null) {
            List<String> speciesNames = request.getTargetSpecies().stream()
                    .map(com.swipelab.dto.request.TargetSpeciesRequest::getName)
                    .collect(Collectors.toList());
            List<Long> speciesIds = targetSpeciesProvider.getOrCreateSpeciesIds(speciesNames);
            task.setTargetSpeciesIds(speciesIds);
        }

        if (request.getSpeciesReferenceImageIds() != null) {
            List<Long> refImageIds = request.getSpeciesReferenceImageIds().values().stream()
                    .flatMap(List::stream)
                    .collect(Collectors.toList());
            task.setSpeciesReferenceImageIds(refImageIds);
        }
        
        // Trigger a background sync in case task external experiments were added
        CompletableFuture.runAsync(stardbiSyncService::syncExperiments);
        
        return mapToResponse(task);
    }

    // =========================
    // Helpers
    // =========================

    /**
     * Computes real image progress for a task by querying the images and
     * classifications tables. imagesClassified is a simple "touched" count
     * (any classification exists) until the threshold-gated logic is built.
     */
    private TaskProgressResponse buildProgress(Long taskId) {
        long total = imageRepository.countByTaskId(taskId);
        long classified = classificationRepository.countDistinctImagesByTaskId(taskId);
        return new TaskProgressResponse((int) total, (int) classified);
    }

    private Task getTask(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));
    }

    private TaskResponse mapToResponse(Task task) {
        return taskMapper.toResponse(task, false, buildProgress(task.getId()));
    }

    // Legacy support if needed, but preferably use new methods
    public List<TaskResponse> getAllTasks(String username) {
        return getResearcherDashboard(username);
    }

    private void verifyTaskAccess(Task task, String username) {
        if (securityAuthorizationService.isSuperAdmin(username)) {
            return;
        }
        if (task.getCreatedBy().equals(username)) {
            return;
        }
        if (task.getSharedWithResearchers() != null && task.getSharedWithResearchers().contains(username)) {
            return;
        }
        throw new AccessDeniedException("You do not have permission to access or modify this task");
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
