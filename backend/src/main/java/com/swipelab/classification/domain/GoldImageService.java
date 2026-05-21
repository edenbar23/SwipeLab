package com.swipelab.classification.domain;

import com.swipelab.classification.infrastructure.GoldImageRepository;
import com.swipelab.classification.infrastructure.ImageRepository;
import com.swipelab.dto.request.GoldImageRequest;
import com.swipelab.dto.response.GoldImageResponse;
import com.swipelab.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.web.multipart.MultipartFile;
import com.swipelab.infrastructure.FileStorageService;
import com.swipelab.classification.application.port.out.TaskProvider;

@Service
@RequiredArgsConstructor
public class GoldImageService {

    @Value("${app.base-url:http://localhost:8080}")
    private String appBaseUrl;

    private final GoldImageRepository goldImageRepository;
    private final ImageRepository imageRepository;
    private final FileStorageService fileStorageService;
    private final TaskProvider taskProvider;

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
            // Download and store locally — we never keep raw external URLs so images
            // remain accessible even if the original link is later deleted.
            srcPath = fileStorageService.storeFileFromUrl(imageUrl);
        } else {
            throw new IllegalArgumentException("Either file or imageUrl must be provided");
        }

        TaskProvider.TaskInfo taskInfo = taskProvider.getTaskInfo(taskId);

        Image image = Image.builder()
                .srcPath(srcPath)
                .taskId(taskInfo.id())
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
        return goldImageRepository.findAllByActiveTrue().stream()
                .filter(g -> g.getImage().getTaskId().equals(taskId))
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
        GoldImage goldImage = goldImageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Gold Image not found: " + id));
        // Soft-delete: preserve the row so credibility_records FK is never violated
        goldImage.setActive(false);
        goldImageRepository.save(goldImage);
    }

    private GoldImageResponse mapToResponse(GoldImage goldImage) {
        String srcPath = goldImage.getImage().getSrcPath();
        // For locally uploaded files, expose them through the dedicated image endpoint
        // so no filesystem path is ever revealed to the client.
        // External URL images (stored as full http/https URLs) are returned as-is.
        String resolvedUrl = (srcPath != null && srcPath.startsWith("/uploads/"))
                ? appBaseUrl.replaceAll("/$", "") + "/api/admin/gold-images/" + goldImage.getId() + "/image"
                : srcPath;
        return GoldImageResponse.builder()
                .id(goldImage.getId())
                .imageId(goldImage.getImage().getId())
                .species(goldImage.getSpecies())
                .correctAnswer(goldImage.getCorrectAnswer())
                .imageUrl(resolvedUrl)
                .build();
    }

    @Transactional(readOnly = true)
    public List<GoldImageResponse> getAllGoldImages() {
        return goldImageRepository.findAllByActiveTrue().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Returns a file Resource for streaming image bytes.
     * Only valid for locally-uploaded images (srcPath starts with /uploads/).
     * Throws if the image was stored as an external URL.
     */
    @Transactional(readOnly = true)
    public Resource getImageResource(Long goldImageId) {
        GoldImage goldImage = goldImageRepository.findById(goldImageId)
                .orElseThrow(() -> new ResourceNotFoundException("Gold Image not found: " + goldImageId));
        String srcPath = goldImage.getImage().getSrcPath();
        if (srcPath == null || !srcPath.startsWith("/uploads/")) {
            throw new IllegalArgumentException("Image is not a locally stored upload");
        }
        return fileStorageService.loadFile(srcPath);
    }
}
