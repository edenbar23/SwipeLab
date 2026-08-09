package com.swipelab.tasks.dto;

import com.swipelab.tasks.dto.ReferenceImageRequest;
import lombok.Data;
import com.swipelab.tasks.dto.ReferenceImageRequest;
import java.util.List;

@Data
public class TargetSpeciesRequest {

    /**
     * Scientific name (e.g. "Vespa mandarinia")
     */
    private String name;

    /**
     * Reference images for admin task creation
     */
    private List<ReferenceImageRequest> referenceImages;
}


