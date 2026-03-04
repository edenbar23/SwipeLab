package com.swipelab.tasks.domain;

import com.swipelab.dto.request.CreateTaskRequest;
import com.swipelab.dto.request.UpdateTaskRequest;
import com.swipelab.dto.response.TaskProgressResponse;
import com.swipelab.dto.response.TaskResponse;
import com.swipelab.dto.response.TargetSpeciesResponse;

import com.swipelab.classification.domain.Label;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class TaskMapper {

    // =========================
    // REQUEST → ENTITY
    // =========================

    public Task toEntity(CreateTaskRequest request) {
        if (request == null) {
            return null;
        }

        return Task.builder()
                .title(request.getName())
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
                // targetSpecies resolved in service
                .build();
    }

    public void updateEntity(Task task, UpdateTaskRequest request) {
        if (task == null || request == null) {
            return;
        }

        if (request.getName() != null) {
            task.setTitle(request.getName());
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
                .isPublic(task.getIsPublic())
                .targetSpecies(
                        task.getTargetSpecies() != null
                                ? task.getTargetSpecies()
                                        .stream()
                                        .map(this::toTargetSpeciesResponse)
                                        .collect(Collectors.toList())
                                : Collections.emptyList())
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

    // =========================
    // HELPERS
    // =========================

    private TargetSpeciesResponse toTargetSpeciesResponse(Label label) {
        if (label == null) {
            return null;
        }

        return TargetSpeciesResponse.builder()
                .name(label.getName())
                .commonName(label.getCommonName())
                .referenceImages(Collections.emptyList()) // filled later if needed
                .build();
    }

}
