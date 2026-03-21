package com.swipelab.integration.stardbi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExternalExperimentDto {
    private Long id;
    private String name;
    
    @JsonProperty("start_date")
    private String startDate;
    
    @JsonProperty("emd_date") // Matching exact contract typo
    private String emdDate;
    
    private String notes;
}
