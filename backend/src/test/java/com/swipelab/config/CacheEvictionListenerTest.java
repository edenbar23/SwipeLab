package com.swipelab.config;

import com.swipelab.classification.domain.core.Classification.UserResponse;
import com.swipelab.classification.events.ClassificationSubmittedEvent;
import com.swipelab.gamification.events.GamificationUpdatedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CacheEvictionListenerTest {

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache gamificationCache;

    @Mock
    private Cache collectionStatsCache;

    @Mock
    private Cache userProfileCache;

    @Mock
    private Cache leaderboardCache;

    @Mock
    private Cache platformOverviewCache;

    private CacheEvictionListener listener;

    @BeforeEach
    void setUp() {
        listener = new CacheEvictionListener(cacheManager);

        when(cacheManager.getCache(CacheConfig.CACHE_GAMIFICATION)).thenReturn(gamificationCache);
        when(cacheManager.getCache(CacheConfig.CACHE_COLLECTION_STATS)).thenReturn(collectionStatsCache);
        when(cacheManager.getCache(CacheConfig.CACHE_USER_PROFILE)).thenReturn(userProfileCache);
        when(cacheManager.getCache(CacheConfig.CACHE_LEADERBOARD)).thenReturn(leaderboardCache);
        when(cacheManager.getCache(CacheConfig.CACHE_PLATFORM_OVERVIEW)).thenReturn(platformOverviewCache);
    }

    // ── ClassificationSubmittedEvent ───────────────────────────────────────────

    @Test
    void onClassificationSubmitted_ShouldEvictUserSpecificAndSharedCaches() {
        ClassificationSubmittedEvent event = ClassificationSubmittedEvent.builder()
                .username("alice")
                .imageId(1L)
                .taskId(1L)
                .userResponse(UserResponse.YES)
                .build();

        listener.onClassificationSubmitted(event);

        // Per-user evictions
        verify(gamificationCache).evict("alice");
        verify(collectionStatsCache).evict("alice");
        verify(userProfileCache).evict("alice");

        // Shared cache clears
        verify(leaderboardCache).clear();
        verify(platformOverviewCache).clear();
    }

    @Test
    void onClassificationSubmitted_WithNullUsername_ShouldNotThrow() {
        ClassificationSubmittedEvent event = ClassificationSubmittedEvent.builder()
                .username(null)
                .build();

        // Must be a no-op — no NPE, no cache interaction
        listener.onClassificationSubmitted(event);

        verifyNoInteractions(gamificationCache, collectionStatsCache, userProfileCache,
                leaderboardCache, platformOverviewCache);
    }

    // ── GamificationUpdatedEvent ───────────────────────────────────────────────

    @Test
    void onGamificationUpdated_ShouldEvictGamificationAndUserProfile() {
        GamificationUpdatedEvent event = GamificationUpdatedEvent.builder()
                .username("bob")
                .score(1500L)
                .build();

        listener.onGamificationUpdated(event);

        verify(gamificationCache).evict("bob");
        verify(userProfileCache).evict("bob");

        // Leaderboard and platform overview NOT touched by this event
        verifyNoInteractions(leaderboardCache, platformOverviewCache);
    }

    @Test
    void onGamificationUpdated_WithNullUsername_ShouldNotThrow() {
        GamificationUpdatedEvent event = GamificationUpdatedEvent.builder()
                .username(null)
                .build();

        listener.onGamificationUpdated(event);

        verifyNoInteractions(gamificationCache, userProfileCache);
    }
}
