package com.swipelab.tasks.application;

import com.swipelab.dto.request.CreateTaskRequest;
import com.swipelab.dto.request.UpdateTaskRequest;
import com.swipelab.dto.response.TaskResponse;
import com.swipelab.exception.ResourceNotFoundException;
import com.swipelab.tasks.domain.Task;
import com.swipelab.tasks.domain.TaskMapper;
import com.swipelab.tasks.domain.TaskStatus;
import com.swipelab.tasks.infrastructure.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskMapper taskMapper;

    // --- User/Shared Operations ---

    public List<TaskResponse> getAllTasks() {
        return taskRepository.findAll().stream()
                .map(taskMapper::toResponse)
                .collect(Collectors.toList());
    }

    public List<TaskResponse> getActiveTasks() {
        return taskRepository.findByStatus(TaskStatus.ACTIVE).stream()
                .map(taskMapper::toResponse)
                .collect(Collectors.toList());
    }

    public TaskResponse getTaskById(Long id) {
        return mapToResponse(getTask(id));
    }

    @Transactional
    public TaskResponse createTask(String username, CreateTaskRequest request) {
        // VALIDATE USER THROUGH AUTH WITH EVENT DRIVEN MAYBE

        Task task = taskMapper.toEntity(request);
        task.setStatus(TaskStatus.ACTIVE);
        // task.setCreatedBy(user); // If Task has createdBy field, uncomment

        return mapToResponse(taskRepository.save(task));
    }

    // --- Admin Operations ---

    @Transactional
    public TaskResponse createTask(CreateTaskRequest request) {
        Task task = taskMapper.toEntity(request);
        task.setStatus(TaskStatus.ACTIVE);
        return mapToResponse(taskRepository.save(task));
    }

    @Transactional
    public TaskResponse archiveTask(Long taskId) {
        Task task = getTask(taskId);
        task.setStatus(TaskStatus.ARCHIVED);
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
        taskMapper.updateEntity(task, request);
        return mapToResponse(task);
    }

    // --- Helpers ---

    private Task getTask(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));
    }

    private TaskResponse mapToResponse(Task task) {
        return taskMapper.toResponse(task);
    }

    public List<TaskResponse> getTasksByUser(String username) {
        return taskRepository.findByCreatedBy_Username(username).stream()
                .map(taskMapper::toResponse)
                .collect(Collectors.toList());
    }
}
