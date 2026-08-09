package com.swipelab.classification.application.port.in;

import com.swipelab.classification.domain.core.Label;
import com.swipelab.classification.infrastructure.LabelRepository;
import com.swipelab.tasks.dto.TargetSpeciesResponse;
import com.swipelab.tasks.application.port.out.TargetSpeciesProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TargetSpeciesProviderImpl implements TargetSpeciesProvider {

    private final LabelRepository labelRepository;
    private final com.swipelab.classification.infrastructure.SpeciesReferenceImageRepository speciesReferenceImageRepository;

    @Override
    @Transactional(readOnly = true)
    public List<TargetSpeciesResponse> getSpeciesByIds(List<Long> speciesIds) {
        if (speciesIds == null || speciesIds.isEmpty()) {
            return Collections.emptyList();
        }
        return labelRepository.findAllById(speciesIds).stream()
                .map(label -> TargetSpeciesResponse.builder()
                        .name(label.getName())
                        .commonName(label.getCommonName())
                        .referenceImages(Collections.emptyList())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<Long> getOrCreateSpeciesIds(List<String> speciesNames) {
        if (speciesNames == null || speciesNames.isEmpty()) {
            return Collections.emptyList();
        }
        return speciesNames.stream().map(name -> {
            Label label = labelRepository.findByName(name)
                    .orElseGet(() -> labelRepository.save(Label.builder().name(name).build()));
            return label.getId();
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TargetSpeciesResponse> getSpeciesByIdsAndRefImages(List<Long> speciesIds, List<Long> refImageIds) {
        if (speciesIds == null || speciesIds.isEmpty()) {
            return Collections.emptyList();
        }
        
        List<Label> labels = labelRepository.findAllById(speciesIds);
        
        List<com.swipelab.classification.domain.image.SpeciesReferenceImage> allRefImages = Collections.emptyList();
        if (refImageIds != null && !refImageIds.isEmpty()) {
            allRefImages = speciesReferenceImageRepository.findAllById(refImageIds);
        }
        
        java.util.Map<Long, List<com.swipelab.classification.domain.image.SpeciesReferenceImage>> refImagesByLabelId = allRefImages.stream()
                .collect(Collectors.groupingBy(com.swipelab.classification.domain.image.SpeciesReferenceImage::getLabelId));
                
        return labels.stream()
                .map(label -> {
                    List<com.swipelab.classification.domain.image.SpeciesReferenceImage> imagesForLabel = refImagesByLabelId.getOrDefault(label.getId(), Collections.emptyList());
                    List<com.swipelab.tasks.dto.ReferenceImageResponse> refImageResponses = imagesForLabel.stream()
                            .map(img -> com.swipelab.tasks.dto.ReferenceImageResponse.builder()
                                    .imageUrl("/api/v1/species/reference-images/" + img.getId() + "/image")
                                    .caption(img.getCaption())
                                    .build())
                            .collect(Collectors.toList());
                            
                    return TargetSpeciesResponse.builder()
                            .name(label.getName())
                            .commonName(label.getCommonName())
                            .referenceImages(refImageResponses)
                            .build();
                })
                .collect(Collectors.toList());
    }
}



