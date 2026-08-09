package com.swipelab.tasks.dto;

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

    @jakarta.validation.constraints.Min(value = 3, message = "Consensus threshold must be at least 3")
    @jakarta.validation.constraints.Max(value = 20, message = "Consensus threshold must be at most 20")
    private double consensusThreshold;

    /**
     * Map of Species Name -> List of SpeciesReferenceImage IDs
     * Selected from the species image pool for this specific task.
     */
    private java.util.Map<String, List<Long>> speciesReferenceImageIds;
}



