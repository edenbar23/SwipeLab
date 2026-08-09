package com.swipelab.tasks.dto;

import lombok.Data;

import java.util.List;

@Data
public class UpdateTaskRequest {
    private String name;
    private String description;
    private List<TargetSpeciesRequest> targetSpecies;
    private List<Long> experiments;
    private List<Long> recipientGroups;
    private List<String> assignedUsernames;
    private List<String> sharedWithResearchers;
    private Boolean isPublic;
    private Double consensusThreshold;

    /**
     * Map of Species Name -> List of SpeciesReferenceImage IDs
     * Selected from the species image pool for this specific task.
     */
    private java.util.Map<String, List<Long>> speciesReferenceImageIds;
}



