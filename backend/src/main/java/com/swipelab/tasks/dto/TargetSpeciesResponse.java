package com.swipelab.tasks.dto;

import com.swipelab.tasks.dto.ReferenceImageResponse;
import lombok.Builder;
import com.swipelab.tasks.dto.ReferenceImageResponse;
import lombok.Data;

import com.swipelab.tasks.dto.ReferenceImageResponse;
import java.util.List;

@Data
@Builder
public class TargetSpeciesResponse {

    /**
     * Scientific name
     * Example: "Vespa mandarinia"
     */
    private String name;

    /**
     * Common name
     * Example: "Asian Giant Hornet"
     */
    private String commonName;
    /**
     * Reference images shown to users/admins
     */
    private List<ReferenceImageResponse> referenceImages;
}


