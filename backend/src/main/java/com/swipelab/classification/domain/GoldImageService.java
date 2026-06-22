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
    public GoldImageResponse uploadGoldImage(MultipartFile file, String imageUrl, String species, String correctAnswerStr) {
        String srcPath;
        if (file != null && !file.isEmpty()) {
            try {
                byte[] bytes = file.getBytes();
                String base64 = java.util.Base64.getEncoder().encodeToString(bytes);
                String contentType = file.getContentType() != null ? file.getContentType() : "image/jpeg";
                srcPath = "data:" + contentType + ";base64," + base64;
            } catch (java.io.IOException e) {
                throw new RuntimeException("Could not read uploaded file", e);
            }
        } else if (imageUrl != null && !imageUrl.isEmpty()) {
            srcPath = downloadUrlAsBase64(imageUrl);
        } else {
            throw new IllegalArgumentException("Either file or imageUrl must be provided");
        }

        Image image = Image.builder()
                .srcPath(srcPath)
                .taskId(null)
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
        TaskProvider.TaskInfo taskInfo = taskProvider.getTaskInfo(taskId);
        List<String> taskSpecies = taskInfo.targetSpeciesNames();
        return goldImageRepository.findAllByActiveTrue().stream()
                .filter(g -> taskSpecies != null && taskSpecies.contains(g.getSpecies()))
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
        return GoldImageResponse.builder()
                .id(goldImage.getId())
                .imageId(goldImage.getImage().getId())
                .species(goldImage.getSpecies())
                .correctAnswer(goldImage.getCorrectAnswer())
                .imageUrl(srcPath)
                .build();
    }

    @Transactional(readOnly = true)
    public List<GoldImageResponse> getAllGoldImages() {
        return goldImageRepository.findAllByActiveTrue().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private String downloadUrlAsBase64(String imageUrl) {
        java.net.HttpURLConnection connection = null;
        try {
            java.net.URI uri = java.net.URI.create(imageUrl);
            connection = (java.net.HttpURLConnection) uri.toURL().openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(15000);
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");
            connection.connect();

            int status = connection.getResponseCode();
            if (status < 200 || status >= 300) {
                throw new RuntimeException("Remote URL returned HTTP " + status);
            }

            String contentType = connection.getContentType();
            if (contentType == null) contentType = "image/jpeg";

            try (java.io.InputStream in = connection.getInputStream();
                 java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
                String base64 = java.util.Base64.getEncoder().encodeToString(out.toByteArray());
                return "data:" + contentType + ";base64," + base64;
            }
        } catch (java.io.IOException ex) {
            throw new RuntimeException("Could not download image from URL: " + imageUrl, ex);
        } finally {
            if (connection != null) connection.disconnect();
        }
    }
}
