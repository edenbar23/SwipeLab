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
public class ExternalTaxonomyDto {

    @JsonProperty("specis_id") // Matching exact contract typo
    private Long speciesId;
    
    @JsonProperty("class")
    private String clazz; // 'class' is a Java keyword
    
    private String order;
    private String family;
    private String genus;
    private String species;
}
