package com.swipelab.tasks.domain;

import com.swipelab.dto.request.CreateTaskRequest;
import com.swipelab.dto.request.UpdateTaskRequest;
import com.swipelab.dto.response.TaskProgressResponse;
import com.swipelab.dto.response.TaskResponse;
import com.swipelab.dto.response.TargetSpeciesResponse;

import com.swipelab.tasks.application.port.out.TargetSpeciesProvider;

import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class TaskMapper {

    private final TargetSpeciesProvider targetSpeciesProvider;

    // =========================
    // REQUEST → ENTITY
    // =========================

    public Task toEntity(CreateTaskRequest request) {
        if (request == null) {
            return null;
        }

        return Task.builder()
                .title(request.getName())
                .name(request.getName() != null ? request.getName().toLowerCase().replace(" ", "_") : null)
                .description(request.getDescription())
                .minClassificationsPerImage(request.getMinClassificationsPerImage())
                .consensusThreshold(request.getConsensusThreshold())
                .experiments(
                        request.getExperiments() != null
                                ? request.getExperiments()
                                : Collections.emptyList())
                .recipientGroups(
                        request.getRecipientGroups() != null
                                ? request.getRecipientGroups()
                                : Collections.emptyList())
                .assignedUsernames(
                        request.getAssignedUsernames() != null
                                ? request.getAssignedUsernames()
                                : Collections.emptyList())
                .isPublic(request.getIsPublic() != null ? request.getIsPublic() : false)
                .sharedWithResearchers(
                        request.getSharedWithResearchers() != null
                                ? request.getSharedWithResearchers()
                                : Collections.emptyList())
                // targetSpecies resolved in service
                .build();
    }

    public void updateEntity(Task task, UpdateTaskRequest request) {
        if (task == null || request == null) {
            return;
        }

        if (request.getName() != null) {
            task.setTitle(request.getName());
            task.setName(request.getName().toLowerCase().replace(" ", "_"));
        }

        if (request.getDescription() != null) {
            task.setDescription(request.getDescription());
        }

        if (request.getExperiments() != null) {
            task.setExperiments(request.getExperiments());
        }

        if (request.getRecipientGroups() != null) {
            task.setRecipientGroups(request.getRecipientGroups());
        }

        if (request.getAssignedUsernames() != null) {
            task.setAssignedUsernames(request.getAssignedUsernames());
        }

        if (request.getIsPublic() != null) {
            task.setIsPublic(request.getIsPublic());
        }

        if (request.getSharedWithResearchers() != null) {
            task.setSharedWithResearchers(request.getSharedWithResearchers());
        }
        // targetSpecies handled in service
    }

    // =========================
    // ENTITY → RESPONSE
    // =========================

    public TaskResponse toResponse(Task task) {
        return toResponse(task, false);
    }

    public TaskResponse toResponse(Task task, boolean assignedToUser) {
        if (task == null) {
            return null;
        }

        return TaskResponse.builder()
                .taskId(task.getId())
                .status(task.getStatus().name())
                .name(task.getTitle())
                .description(task.getDescription())
                .experiments(task.getExperiments())
                .recipientGroups(task.getRecipientGroups())
                .assignedUsernames(task.getAssignedUsernames())
                .sharedWithResearchers(task.getSharedWithResearchers())
                .isPublic(task.getIsPublic())
                .targetSpecies(targetSpeciesProvider.getSpeciesByIds(task.getTargetSpeciesIds()))
                .progress(TaskProgressResponse.empty())
                // New fields
                .createdAt(task.getCreatedAt() != null
                        ? java.time.OffsetDateTime.of(task.getCreatedAt(), java.time.ZoneOffset.UTC)
                        : null)
                .deadline(task.getDeadline() != null
                        ? java.time.OffsetDateTime.of(task.getDeadline(), java.time.ZoneOffset.UTC)
                        : null)
                .minClassificationsPerImage(task.getMinClassificationsPerImage())
                .consensusThreshold(task.getConsensusThreshold())
                .assignedToUser(assignedToUser)
                .build();
    }

    public List<TaskResponse> toResponseList(List<Task> tasks) {
        if (tasks == null) {
            return Collections.emptyList();
        }

        return tasks.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
}


