package com.swipelab.common.dto.response;

import com.swipelab.tasks.dto.TargetSpeciesResponse;
import lombok.Data;
import com.swipelab.tasks.dto.TargetSpeciesResponse;
import java.util.List;

@Data
public class MyTaskListResponse {
    private Integer page;
    private Integer pageSize;
    private Integer totalPages;
    private Integer totalTasks;
    private List<UserTaskSummary> tasks;

    @Data
    public static class UserTaskSummary {
        private Long taskId;
        private String name;
        private String description;
        private Integer totalImages;
        private Integer imagesClassified;
        private List<TargetSpeciesResponse> species; // Reuse existing or create specific
    }
}

