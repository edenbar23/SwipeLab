package com.swipelab.collection.application;

import com.swipelab.collection.domain.UserCollectionEntry;
import com.swipelab.collection.dto.CollectionEntryResponse;
import com.swipelab.collection.dto.CollectionStatsResponse;
import com.swipelab.collection.infrastructure.UserCollectionRepository;
import com.swipelab.config.CacheConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CollectionService {

    private final UserCollectionRepository collectionRepository;

    /**
     * Records a YES-tagged image into the user's collection.
     * No dedup: each distinct YES swipe produces its own entry.
     */
    @Transactional
    public CollectionEntryResponse recordYesTag(String username, Long imageId,
                                                String species, String imageUrl, Long taskId) {
        UserCollectionEntry entry = UserCollectionEntry.builder()
                .username(username)
                .imageId(imageId)
                .species(species)
                .imageUrl(imageUrl)
                .taskId(taskId)
                .build();

        UserCollectionEntry saved = collectionRepository.save(entry);
        log.info("Recorded YES tag for user={} image={} species={}", username, imageId, species);
        return toResponse(saved);
    }

    /** Returns all YES-tagged entries for the user, newest first. */
    @Transactional(readOnly = true)
    public List<CollectionEntryResponse> getCollection(String username) {
        return collectionRepository
                .findByUsernameOrderByTaggedAtDesc(username)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /** Returns aggregate stats for the user's collection. */
    @Cacheable(value = CacheConfig.CACHE_COLLECTION_STATS, key = "#username")
    @Transactional(readOnly = true)
    public CollectionStatsResponse getStats(String username) {
        return CollectionStatsResponse.builder()
                .total(collectionRepository.countByUsername(username))
                .build();
    }

    private CollectionEntryResponse toResponse(UserCollectionEntry entry) {
        return CollectionEntryResponse.builder()
                .id(entry.getId())
                .imageId(entry.getImageId())
                .species(entry.getSpecies())
                .imageUrl(entry.getImageUrl())
                .taskId(entry.getTaskId())
                .taggedAt(entry.getTaggedAt())
                .build();
    }
}
