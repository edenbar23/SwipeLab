package com.swipelab.dto.response;

import lombok.Builder;
import lombok.Data;

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
