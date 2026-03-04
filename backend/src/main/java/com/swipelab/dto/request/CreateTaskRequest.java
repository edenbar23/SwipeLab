package com.swipelab.dto.request;

import lombok.Data;
import java.util.List;

@Data
public class CreateTaskRequest {
    private String name;
    private String description;
    private List<TargetSpeciesRequest> targetSpecies;
    private List<Long> experiments;
    private List<Long> recipientGroups;
    private List<String> assignedUsernames;
    private Boolean isPublic;
    private int minClassificationsPerImage;
    private double consensusThreshold;
}
