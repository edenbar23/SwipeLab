package com.swipelab.service;

import com.swipelab.dto.request.CreateTaskRequest;
import com.swipelab.dto.response.TaskResponse;
import com.swipelab.exception.ResourceNotFoundException;
import com.swipelab.model.entity.Task;
import com.swipelab.model.entity.User;
import com.swipelab.model.enums.TaskStatus;
import com.swipelab.repository.TaskRepository;
import com.swipelab.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    @Transactional
    public TaskResponse createTask(String username, CreateTaskRequest request) {
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

        Task task = Task.builder()
                .title(request.getName())
                .description(request.getDescription())
                .status(TaskStatus.ACTIVE) // Defaulting to ACTIVE per project requirements
                .createdBy(user)
                .build();

        Task savedTask = taskRepository.save(task);
        return mapToResponse(savedTask);
    }

    public List<TaskResponse> getAllTasks() {
        return taskRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<TaskResponse> getActiveTasks() {
        return taskRepository.findByStatus(TaskStatus.ACTIVE).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public TaskResponse getTaskById(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));
        return mapToResponse(task);
    }

    private TaskResponse mapToResponse(Task task) {
        return TaskResponse.builder()
                .taskId(task.getId()) // DTO uses taskId, Entity uses id
                .name(task.getTitle()) // DTO uses name, Entity uses title
                .description(task.getDescription())
                .status(task.getStatus() != null ? task.getStatus().name() : null) // Convert Enum to String
                // For now, these can be empty lists to allow compilation until the full logic is added
                .targetSpecies(java.util.Collections.emptyList())
                .experiments(java.util.Collections.emptyList())
                .recipientGroups(java.util.Collections.emptyList())
                .progress(null)
                .build();
    }
}
