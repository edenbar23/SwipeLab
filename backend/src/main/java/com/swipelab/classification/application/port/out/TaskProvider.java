package com.swipelab.classification.application.port.out;

import java.util.List;

public interface TaskProvider {
    TaskInfo getTaskInfo(Long taskId);

    record TaskInfo(Long id, String question, String querySpecies, List<String> targetSpeciesNames, List<com.swipelab.dto.response.TargetSpeciesResponse> targetSpeciesResponses, Double consensusThreshold) {}
}
