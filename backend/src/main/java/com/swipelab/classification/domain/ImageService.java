package com.swipelab.classification.domain;

import com.swipelab.classification.dto.api.*;
import com.swipelab.classification.infrastructure.GoldImageRepository;
import com.swipelab.dto.request.ImageUploadRequest;
import com.swipelab.dto.response.ImageBatchResponse;
import com.swipelab.dto.response.ImageResponse;
import com.swipelab.exception.ResourceNotFoundException;

import com.swipelab.tasks.domain.Task;
import com.swipelab.classification.infrastructure.ImageRepository;
import com.swipelab.classification.infrastructure.LabelRepository;
import com.swipelab.tasks.infrastructure.TaskRepository;
import com.swipelab.classification.infrastructure.ClassificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ImageService {

        private final ImageRepository imageRepository;
        private final TaskRepository taskRepository;
        private final LabelRepository labelRepository;
        private final ClassificationRepository classificationRepository;
        private final GoldImageRepository goldImageRepository;
        private final TaskDistributionService taskDistributionService;

        @Transactional(readOnly = true)
        public NextBatchResponse getNextBatchForApi(Long taskId, String username, int count) {
                Task task = taskRepository.findById(taskId)
                                .orElseThrow(() -> new ResourceNotFoundException("Task not found: " + taskId));

                List<BatchImageDto> batchImages = new ArrayList<>();

                // Retry mechanism to find available images properly
                int attempt = 0;
                int found = 0;

                // Very basic loop. In production, we'd bulk fetch.
                // Re-using taskDistributionService but it's designed for single fetch with
                // state.
                // We will loop. State update in taskDist might be redundant or messy if we call
                // it multiple times.
                // But let's use it for now as it handles logic.

                while (found < count && attempt < count * 2) {
                        Optional<Image> imageOpt = taskDistributionService.getNextImageForUser(username, taskId);
                        if (imageOpt.isEmpty()) {
                                break;
                        }
                        Image image = imageOpt.get();

                        // Avoid duplicates in this batch if any (TaskDist might give different ones,
                        // hopefully)
                        if (batchImages.stream().noneMatch(b -> b.getImageId().equals(image.getId()))) {
                                batchImages.add(mapToBatchDto(image, task));
                                found++;
                        }
                        attempt++;
                }

                return NextBatchResponse.builder().images(batchImages).build();
        }

        private BatchImageDto mapToBatchDto(Image image, Task task) {
                return BatchImageDto.builder()
                                .imageId(image.getId())
                                .taskId(task.getId())
                                .question(task.getQuestion() != null ? task.getQuestion() : "Classify this image")
                                .image(ImageDataDto.builder()
                                                .contentType("image/jpeg")
                                                .data(mockBase64(image.getSrcPath()))
                                                .build())
                                .referenceImages(List.of()) // Placeholder
                                .build();
        }

        private String mockBase64(String path) {
                return "base64_mock_data_for_" + path;
        }

        @Transactional
        public ImageResponse uploadImage(ImageUploadRequest request) {
                Task task = taskRepository.findById(request.getTaskId())
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Task not found with id: " + request.getTaskId()));

                Image image = Image.builder()
                                .srcPath(request.getImageUrl())
                                .caption(request.getCaption())
                                .task(task)
                                .priority(request.getPriority())
                                .build();

                Image savedImage = imageRepository.save(image);

                if (Boolean.TRUE.equals(request.getIsGoldStandard())) {
                        if (request.getCorrectLabelId() != null) {
                                Label label = labelRepository.findById(request.getCorrectLabelId())
                                                .orElseThrow(() -> new ResourceNotFoundException(
                                                                "Label not found: " + request.getCorrectLabelId()));

                                GoldImage goldImage = GoldImage.builder()
                                                .image(savedImage)
                                                .species(label.getName())
                                                .correctAnswer(GoldImage.UserResponse.YES)
                                                .build();
                                goldImageRepository.save(goldImage);
                        }
                }

                return mapToResponse(savedImage);
        }

        @Transactional(readOnly = true)
        public ImageBatchResponse getImageBatch(Long taskId, String username) {
                if (taskId == null) {
                        throw new IllegalArgumentException("Task ID cannot be null");
                }

                List<Image> allImages = imageRepository.findByTaskId(taskId);
                List<Image> unclassifiedImages;

                if (username != null && !username.isEmpty()) {
                        unclassifiedImages = allImages.stream()
                                        .filter(image -> !classificationRepository
                                                        .existsByUsernameAndImageId(username, image.getId()))
                                        .collect(Collectors.toList());
                } else {
                        unclassifiedImages = allImages;
                }

                Collections.shuffle(unclassifiedImages);

                List<ImageResponse> batch = unclassifiedImages.stream()
                                .limit(20)
                                .map(this::mapToResponse)
                                .collect(Collectors.toList());

                return ImageBatchResponse.builder()
                                .images(batch)
                                .build();
        }

        public ImageResponse getImageById(Long id) {
                Image image = imageRepository.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException("Image not found with id: " + id));
                return mapToResponse(image);
        }

        private ImageResponse mapToResponse(Image image) {
                boolean isGold = goldImageRepository.existsByImageId(image.getId());
                return ImageResponse.builder()
                                .id(image.getId())
                                .imageUrl(image.getSrcPath())
                                .thumbnailUrl(image.getThumbnailUrl())
                                .caption(image.getCaption())
                                .taskId(image.getTask().getId())
                                .priority(image.getPriority())
                                .isGoldStandard(isGold)
                                .build();
        }
}
