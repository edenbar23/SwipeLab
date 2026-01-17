package com.swipelab.classification.application;

import com.swipelab.dto.request.GoldImageRequest;
import com.swipelab.dto.response.GoldImageResponse;
import com.swipelab.exception.ResourceNotFoundException;
import com.swipelab.classification.domain.GoldImage;
import com.swipelab.classification.domain.Image;
import com.swipelab.classification.domain.Label;
import com.swipelab.tasks.domain.Task;
import com.swipelab.repository.GoldImageRepository;
import com.swipelab.classification.infrastructure.ImageRepository;
import com.swipelab.classification.infrastructure.LabelRepository;
import com.swipelab.tasks.infrastructure.TaskRepository;
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
    private final TaskRepository taskRepository;
    private final LabelRepository labelRepository;

    @Transactional
    public GoldImageResponse createGoldImage(GoldImageRequest request) {
        Task task = taskRepository.findById(request.getTaskId())
                .orElseThrow(() -> new ResourceNotFoundException("Task not found: " + request.getTaskId()));

        Label correctLabel = labelRepository.findById(request.getCorrectLabelId())
                .orElseThrow(() -> new ResourceNotFoundException("Label not found: " + request.getCorrectLabelId()));

        // Create the base Image entity
        Image image = Image.builder()
                .imageUrl(request.getImageUrl())
                .caption(request.getCaption())
                .task(task)
                .isGoldStandard(true)
                .correctLabel(correctLabel)
                .priority(0)
                .build();

        Image savedImage = imageRepository.save(image);

        // Create the GoldImage entity with additional details
        GoldImage goldImage = GoldImage.builder()
                .image(savedImage)
                .difficultyLevel(request.getDifficultyLevel() != null ? request.getDifficultyLevel() : "MEDIUM")
                .explanation(request.getExplanation())
                .build();

        GoldImage savedGoldImage = goldImageRepository.save(goldImage);
        return mapToResponse(savedGoldImage);
    }

    @Transactional(readOnly = true)
    public List<GoldImageResponse> getGoldImagesByTask(Long taskId) {
        return goldImageRepository.findByImage_Task_Id(taskId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public GoldImageResponse getGoldImageById(Long id) {
        GoldImage goldImage = goldImageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Gold image not found: " + id));
        return mapToResponse(goldImage);
    }

    @Transactional
    public GoldImageResponse updateGoldImage(Long id, GoldImageRequest request) {
        GoldImage goldImage = goldImageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Gold image not found: " + id));

        if (request.getDifficultyLevel() != null) {
            goldImage.setDifficultyLevel(request.getDifficultyLevel());
        }
        if (request.getExplanation() != null) {
            goldImage.setExplanation(request.getExplanation());
        }
        if (request.getCorrectLabelId() != null) {
            Label newLabel = labelRepository.findById(request.getCorrectLabelId())
                    .orElseThrow(
                            () -> new ResourceNotFoundException("Label not found: " + request.getCorrectLabelId()));
            goldImage.getImage().setCorrectLabel(newLabel);
        }

        return mapToResponse(goldImageRepository.save(goldImage));
    }

    @Transactional
    public void deleteGoldImage(Long id) {
        GoldImage goldImage = goldImageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Gold image not found: " + id));

        // Remove gold status from the image
        Image image = goldImage.getImage();
        image.setIsGoldStandard(false);
        image.setCorrectLabel(null);
        imageRepository.save(image);

        goldImageRepository.delete(goldImage);
    }

    private GoldImageResponse mapToResponse(GoldImage goldImage) {
        Image image = goldImage.getImage();
        return GoldImageResponse.builder()
                .id(goldImage.getId())
                .imageId(image.getId())
                .imageUrl(image.getImageUrl())
                .caption(image.getCaption())
                .taskId(image.getTask().getId())
                .correctLabelId(image.getCorrectLabel() != null ? image.getCorrectLabel().getId() : null)
                .correctLabelName(image.getCorrectLabel() != null ? image.getCorrectLabel().getName() : null)
                .difficultyLevel(goldImage.getDifficultyLevel())
                .explanation(goldImage.getExplanation())
                .createdAt(goldImage.getCreatedAt())
                .build();
    }
}
