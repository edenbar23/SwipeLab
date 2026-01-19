package com.swipelab.dto.response;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class TaskPageResponse {
    private int page;
    private int pageSize;
    private int totalPages;
    private long totalTasks;
    private List<TaskResponse> tasks;
}
