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
public class ExternalClassificationDto {

    @JsonProperty("user_id")
    private String userId; // Using string as contract says "integer or string", string is safer
    
    @JsonProperty("user_classification_grade")
    private Integer userClassificationGrade;
    
    @JsonProperty("image_id")
    private Long imageId; // The bounding box internal ID
    
    @JsonProperty("classification_id")
    private Long classificationId; // the species_id FK
}
