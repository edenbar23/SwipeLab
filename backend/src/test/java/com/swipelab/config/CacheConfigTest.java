package com.swipelab.config;

import com.swipelab.integration.stardbi.StardbiClientPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CacheConfigTest {

    @Mock
    private StardbiClientPort stardbiClient;

    @InjectMocks
    private CacheConfig cacheConfig;

    // ── Cache registration ─────────────────────────────────────────────────────

    @Test
    void cacheManager_ShouldRegisterAllExpectedCaches() {
        CacheManager manager = cacheConfig.cacheManager();

        assertThat(manager.getCache(CacheConfig.CACHE_LEADERBOARD)).isNotNull();
        assertThat(manager.getCache(CacheConfig.CACHE_TAXONOMY)).isNotNull();
        assertThat(manager.getCache(CacheConfig.CACHE_COLLECTION_STATS)).isNotNull();
        assertThat(manager.getCache(CacheConfig.CACHE_GAMIFICATION)).isNotNull();
        assertThat(manager.getCache(CacheConfig.CACHE_ACTIVE_TASKS)).isNotNull();
        assertThat(manager.getCache(CacheConfig.CACHE_TASK_DETAILS)).isNotNull();
        assertThat(manager.getCache(CacheConfig.CACHE_USER_PROFILE)).isNotNull();
        assertThat(manager.getCache(CacheConfig.CACHE_PLATFORM_OVERVIEW)).isNotNull();
    }

    @Test
    void cache_ShouldReturnSameValueOnRepeatedGet() {
        CacheManager manager = cacheConfig.cacheManager();
        Cache leaderboard = manager.getCache(CacheConfig.CACHE_LEADERBOARD);

        leaderboard.put("key1", "value1");

        Cache.ValueWrapper hit = leaderboard.get("key1");
        assertThat(hit).isNotNull();
        assertThat(hit.get()).isEqualTo("value1");
    }

    @Test
    void cache_ShouldReturnNullAfterEviction() {
        CacheManager manager = cacheConfig.cacheManager();
        Cache cache = manager.getCache(CacheConfig.CACHE_GAMIFICATION);

        cache.put("alice", "gamification-data");
        cache.evict("alice");

        assertThat(cache.get("alice")).isNull();
    }

    @Test
    void cache_ShouldBeEmptyAfterClear() {
        CacheManager manager = cacheConfig.cacheManager();
        Cache cache = manager.getCache(CacheConfig.CACHE_LEADERBOARD);

        cache.put(10, "top10");
        cache.put(5, "top5");
        cache.clear();

        assertThat(cache.get(10)).isNull();
        assertThat(cache.get(5)).isNull();
    }

    // ── Taxonomy warm-up ───────────────────────────────────────────────────────

    @Test
    void warmTaxonomyCache_ShouldCallGetTaxonomy() {
        cacheConfig.warmTaxonomyCache();

        verify(stardbiClient, times(1)).getTaxonomy();
    }

    @Test
    void warmTaxonomyCache_ShouldNotPropagateException_WhenStardbiIsDown() {
        doThrow(new RuntimeException("Stardbi unavailable")).when(stardbiClient).getTaxonomy();

        // Must NOT throw — warm-up failure is non-fatal
        cacheConfig.warmTaxonomyCache();

        verify(stardbiClient, times(1)).getTaxonomy();
    }
}
