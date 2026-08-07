package com.swipelab.collection.application;

import com.swipelab.classification.domain.core.Classification.UserResponse;
import com.swipelab.classification.events.ClassificationSubmittedEvent;
import com.swipelab.classification.infrastructure.ImageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Listens to classification events and records YES-tagged images into the user's collection.
 * Runs asynchronously so it does not block the classification response flow.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CollectionEventListener {

    private final CollectionService collectionService;
    private final ImageRepository imageRepository;

    @Async
    @EventListener
    @Transactional
    public void onClassificationSubmitted(ClassificationSubmittedEvent event) {
        if (event.getUserResponse() != UserResponse.YES) {
            return; // only YES swipes go into the collection
        }

        // Resolve imageUrl from the image record (srcPath holds the URL or base64).
        String imageUrl = imageRepository.findById(event.getImageId())
                .map(img -> img.getSrcPath())
                .orElse(null);

        collectionService.recordYesTag(
                event.getUsername(),
                event.getImageId(),
                event.getSpecies(),
                imageUrl,
                event.getTaskId()
        );
    }
}
