package com.swipelab.classification.domain;

import com.swipelab.classification.infrastructure.ClassificationRepository;
import com.swipelab.classification.infrastructure.ImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TaskDistributionService {

    private final ImageRepository imageRepository;
    private final ClassificationRepository classificationRepository;
    private final com.swipelab.classification.infrastructure.GoldImageRepository goldImageRepository;

    /**
     * Holds the selected image and the species to query for that image.
     */
    public record ImageSpeciesPair(Image image, String species) {}



    /**
     * Get next gold standard image+species pair that user hasn't classified yet for that species.
     */
    public Optional<ImageSpeciesPair> getNextGoldImagePair(String username, Long taskId, List<String> taskSpecies) {
        if (taskSpecies == null || taskSpecies.isEmpty()) return Optional.empty();
        
        List<Image> goldImages = imageRepository.findUnclassifiedGoldImages(username, taskSpecies);
        if (goldImages.isEmpty()) return Optional.empty();

        for (Image image : goldImages) {
            Optional<com.swipelab.classification.domain.GoldImage> goldOpt = goldImageRepository.findByImageIdAndActiveTrue(image.getId());
            if (goldOpt.isPresent()) {
                return Optional.of(new ImageSpeciesPair(image, goldOpt.get().getSpecies()));
            }
        }
        return Optional.empty();
    }

    /**
     * Get next regular image+species pair, prioritising images with fewer total classifications.
     * Candidates are images the user hasn't classified for EVERY species yet.
     */
    public Optional<ImageSpeciesPair> getNextRegularImagePair(String username, Long taskId, List<String> taskSpecies) {
        int speciesCount = taskSpecies == null || taskSpecies.isEmpty() ? 1 : taskSpecies.size();
        List<Image> candidates = imageRepository.findRegularImageCandidatesForUser(
                username, taskId, speciesCount, PageRequest.of(0, 20));

        // Shuffle to avoid always picking the same image when multiple are available
        Collections.shuffle(candidates);

        for (Image image : candidates) {
            String species = pickUnqueriedSpecies(username, image.getId(), taskSpecies);
            if (species != null) return Optional.of(new ImageSpeciesPair(image, species));
        }
        return Optional.empty();
    }

    /**
     * From the task species list, randomly pick one the user hasn't queried for this image yet.
     */
    private String pickUnqueriedSpecies(String username, Long imageId, List<String> taskSpecies) {
        if (taskSpecies == null || taskSpecies.isEmpty()) return null;

        List<String> alreadyQueried = classificationRepository
                .findQueriedSpeciesByUsernameAndImageId(username, imageId);

        List<String> remaining = taskSpecies.stream()
                .filter(s -> !alreadyQueried.contains(s))
                .collect(java.util.stream.Collectors.toList());

        if (remaining.isEmpty()) return null;

        Collections.shuffle(remaining);
        return remaining.get(0);
    }

    public void resetUserSession(String username, Long taskId) {
        // No-op — state is computed from DB
    }
}
