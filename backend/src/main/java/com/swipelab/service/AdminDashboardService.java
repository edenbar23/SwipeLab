package com.swipelab.service;

import com.swipelab.dto.request.*;
import com.swipelab.dto.response.*;
import com.swipelab.exception.ResourceNotFoundException;
import com.swipelab.mapper.TaskMapper;
import com.swipelab.model.entity.RecipientGroup;
import com.swipelab.model.entity.Task;
import com.swipelab.model.entity.User;
import com.swipelab.model.enums.TaskStatus;
import com.swipelab.repository.RecipientGroupRepository;
import com.swipelab.repository.TaskRepository;

import com.swipelab.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminDashboardService {
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final RecipientGroupRepository recipientGroupRepository;
    private final TaskMapper taskMapper;

    public List<TaskResponse> getAllTasks() {
        return taskRepository.findAll()
                .stream()
                .map(taskMapper::toResponse)
                .toList();
    }

    public TaskResponse createTask(CreateTaskRequest request) {
        Task task = taskMapper.toEntity(request);
        task.setStatus(TaskStatus.ACTIVE);
        return taskMapper.toResponse(taskRepository.save(task));
    }


    public TaskResponse archiveTask(Long taskId) {
        Task task = getTask(taskId);
        task.setStatus(TaskStatus.ARCHIVED);
        return taskMapper.toResponse(task);
    }


    public TaskResponse activateTask(Long taskId) {
        Task task = getTask(taskId);
        task.activate();
        return taskMapper.toResponse(task);
    }


    public TaskResponse pauseTask(Long taskId) {
        Task task = getTask(taskId);
        task.pause();
        return taskMapper.toResponse(task);
    }

    public TaskResponse updateTask(Long taskId, UpdateTaskRequest request) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Task id:"+taskId +" not found")
                );

        taskMapper.updateEntity(task, request);

        return taskMapper.toResponse(task);
    }

//    public TaskAnalyticsResponse getTaskAnalytics(Long taskId) {
//        // later: aggregate analytics tables
//        return new TaskAnalyticsResponse();
//    }

    private Task getTask(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
    }

    public RecipientGroupResponse createRecipientGroup(CreateRecipientGroupRequest request) {

        if (recipientGroupRepository.existsByName(request.getName())) {
            throw new IllegalStateException(
                    "Recipient group with name '" + request.getName() + "' already exists"
            );
        }

        Set<User> users = userRepository.findByUsernameIn(request.getUsernames());

        RecipientGroup group = RecipientGroup.builder()
                .name(request.getName())
                .users(users)
                .build();

        recipientGroupRepository.save(group);

        return toRecipientGroupResponse(group);
    }

    public RecipientGroupResponse updateRecipientGroup(
            Long groupId,
            UpdateRecipientGroupRequest request
    ) {
        RecipientGroup group = recipientGroupRepository.findById(groupId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("RecipientGroup id:"+groupId+ " not found")
                );

        if (request.getAddUsernames() != null && !request.getAddUsernames().isEmpty()) {
            Set<User> usersToAdd =
                    userRepository.findByUsernameIn(request.getAddUsernames());
            group.addUsers(usersToAdd);
        }

        if (request.getRemoveUsernames() != null && !request.getRemoveUsernames().isEmpty()) {
            Set<User> usersToRemove =
                    userRepository.findByUsernameIn(request.getRemoveUsernames());
            group.removeUsers(usersToRemove);
        }

        return toRecipientGroupResponse(group);
    }

    public List<RecipientGroupResponse> getRecipientGroups() {
        return recipientGroupRepository.findAll()
                .stream()
                .map(this::toRecipientGroupResponse)
                .toList();
    }

    public void deleteRecipientGroup(Long groupId) {
        RecipientGroup group = recipientGroupRepository.findById(groupId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("RecipientGroup id:"+groupId+" not found")
                );

        recipientGroupRepository.delete(group);
    }



    // =========================
    // PRIVATE MAPPERS
    // =========================

    private RecipientGroupResponse toRecipientGroupResponse(RecipientGroup group) {
        return RecipientGroupResponse.builder()
                .groupId(group.getId())
                .name(group.getName())
                .userCount(group.getUserCount())
                .usernames(
                        group.getUsers()
                                .stream()
                                .map(User::getUsername)
                                .sorted()
                                .toList()
                )
                .build();
    }
}