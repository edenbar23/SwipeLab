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
import org.springframework.web.multipart.MultipartFile;
import com.swipelab.infrastructure.FileStorageService;
import com.swipelab.tasks.infrastructure.TaskRepository;
import com.swipelab.tasks.domain.Task;

@Service
@RequiredArgsConstructor
public class GoldImageService {

    private final GoldImageRepository goldImageRepository;
    private final ImageRepository imageRepository;
    private final FileStorageService fileStorageService;
    private final TaskRepository taskRepository;

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

    @Transactional
    public GoldImageResponse uploadGoldImage(MultipartFile file, String imageUrl, Long taskId, String species, String correctAnswerStr) {
        String srcPath;
        if (file != null && !file.isEmpty()) {
            srcPath = fileStorageService.storeFile(file);
        } else if (imageUrl != null && !imageUrl.isEmpty()) {
            srcPath = imageUrl;
        } else {
            throw new IllegalArgumentException("Either file or imageUrl must be provided");
        }

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found: " + taskId));

        Image image = Image.builder()
                .srcPath(srcPath)
                .task(task)
                .build();
        Image savedImage = imageRepository.save(image);

        GoldImage.UserResponse correctAnswer;
        try {
            correctAnswer = GoldImage.UserResponse.valueOf(correctAnswerStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            correctAnswer = GoldImage.UserResponse.YES;
        }

        GoldImage goldImage = GoldImage.builder()
                .image(savedImage)
                .species(species)
                .correctAnswer(correctAnswer)
                .build();

        GoldImage savedGoldImage = goldImageRepository.save(goldImage);
        return mapToResponse(savedGoldImage);
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
