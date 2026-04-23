package com.swipelab.tasks.api;

import com.swipelab.integration.stardbi.StardbiClient;
import com.swipelab.integration.stardbi.dto.ExternalTaxonomyDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v1/metadata")
@RequiredArgsConstructor
public class MetadataController {

    private final StardbiClient stardbiClient;

    @GetMapping("/species")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getSpecies() {
        try {
            // Retrieves target species taxonomy directly from Stardbi
            List<ExternalTaxonomyDto> taxonomy = stardbiClient.getTaxonomy();
            if (taxonomy != null && !taxonomy.isEmpty()) {
                log.info("Sample Species DTO from STARdbi: {}", taxonomy.get(0));
            }

            // Map to frontend-expected format { "id": ..., "label": ... }
            List<Map<String, Object>> options = taxonomy.stream()
                .map(tax -> {
                    Map<String, Object> option = new HashMap<>();
                    // Use speciesId if available, fallback to the species name, then genus
                    Object id = tax.getSpeciesId() != null ? tax.getSpeciesId() : 
                                (tax.getSpecies() != null ? tax.getSpecies() : tax.getGenus());
                    String label = tax.getSpecies() != null ? tax.getSpecies() : tax.getGenus();
                    
                    option.put("id", id);
                    option.put("label", label);
                    option.put("searchTerms", String.join(" ",
                        tax.getClazz() != null ? tax.getClazz() : "",
                        tax.getOrder() != null ? tax.getOrder() : "",
                        tax.getFamily() != null ? tax.getFamily() : "",
                        tax.getGenus() != null ? tax.getGenus() : "",
                        tax.getSpecies() != null ? tax.getSpecies() : ""
                    ).toLowerCase());
                    return option;
                })
                .collect(Collectors.toList());

            return ResponseEntity.ok(options);
        } catch (Exception e) {
            log.error("SEVERE: Failed to fetch species metadata", e);
            throw e; 
        }
    }
}
