package com.swipelab.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.OffsetDateTime;
import java.util.List;

@Data
@Builder
public class TaskResponse {
    private Long taskId;
    private String status;
    private String name;
    private String description;
    private List<TargetSpeciesResponse> targetSpecies;
    private List<Long> experiments;
    private List<Long> recipientGroups;
    private TaskProgressResponse progress;

    private boolean assignedToUser;
    private OffsetDateTime createdAt;
    private OffsetDateTime deadline;
    private Integer minClassificationsPerImage;
    private Double consensusThreshold;

    private Boolean isPublic;
    private List<String> assignedUsernames;
}