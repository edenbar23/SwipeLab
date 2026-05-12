package com.swipelab.dto.request;

import lombok.Data;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Data
public class CreateTaskRequest {
    @NotBlank(message = "Task name is required")
    @Size(min = 3, max = 100, message = "Task name must be between 3 and 100 characters")
    private String name;

    @NotBlank(message = "Task description is required")
    @Size(min = 10, max = 2000, message = "Task description must be between 10 and 2000 characters")
    private String description;
    private List<TargetSpeciesRequest> targetSpecies;
    private List<Long> experiments;
    private List<Long> recipientGroups;
    private List<String> assignedUsernames;
    private List<String> sharedWithResearchers;
    private Boolean isPublic;
    private int minClassificationsPerImage;
    private double consensusThreshold;
}
