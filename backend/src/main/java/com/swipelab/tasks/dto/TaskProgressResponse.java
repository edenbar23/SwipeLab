package com.swipelab.tasks.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class TaskProgressResponse {

    private int totalImages;
    private int imagesClassified;

    public static TaskProgressResponse empty() {
        return new TaskProgressResponse(0, 0);
    }
}



