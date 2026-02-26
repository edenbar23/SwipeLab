package com.swipelab.classification.domain;

import com.swipelab.classification.infrastructure.GoldImageRepository;
import com.swipelab.classification.infrastructure.ImageRepository;
import com.swipelab.dto.request.GoldImageRequest;
import com.swipelab.dto.response.GoldImageResponse;
import com.swipelab.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GoldImageService {

    private final GoldImageRepository goldImageRepository;
    private final ImageRepository imageRepository;

    @Transactional
    public GoldImageResponse createGoldImage(GoldImageRequest request) {
        Image image = imageRepository.findById(request.getImageId())
                .orElseThrow(() -> new ResourceNotFoundException("Image not found: " + request.getImageId()));

        GoldImage goldImage = GoldImage.builder()
                .image(image)
                .species(request.getSpecies())
                .correctAnswer(request.getCorrectAnswer())
                .build();

        GoldImage saved = goldImageRepository.save(goldImage);
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<GoldImageResponse> getGoldImagesByTask(Long taskId) {
        // Assuming we want all gold images for images belonging to a task
        return goldImageRepository.findAll().stream()
                .filter(g -> g.getImage().getTask().getId().equals(taskId))
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public GoldImageResponse getGoldImageById(Long id) {
        GoldImage goldImage = goldImageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Gold Image not found: " + id));
        return mapToResponse(goldImage);
    }

    @Transactional
    public GoldImageResponse updateGoldImage(Long id, GoldImageRequest request) {
        GoldImage goldImage = goldImageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Gold Image not found: " + id));

        goldImage.setSpecies(request.getSpecies());
        goldImage.setCorrectAnswer(request.getCorrectAnswer());

        GoldImage updated = goldImageRepository.save(goldImage);
        return mapToResponse(updated);
    }

    @Transactional
    public void deleteGoldImage(Long id) {
        goldImageRepository.deleteById(id);
    }

    private GoldImageResponse mapToResponse(GoldImage goldImage) {
        return GoldImageResponse.builder()
                .id(goldImage.getId())
                .imageId(goldImage.getImage().getId())
                .species(goldImage.getSpecies())
                .correctAnswer(goldImage.getCorrectAnswer())
                .build();
    }

    @Transactional(readOnly = true)
    public List<GoldImageResponse> getAllGoldImages() {
        return goldImageRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
}
