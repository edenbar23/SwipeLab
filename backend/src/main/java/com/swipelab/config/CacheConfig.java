package com.swipelab.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.swipelab.integration.stardbi.StardbiClientPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

import java.util.concurrent.TimeUnit;

/**
 * Central Caffeine cache configuration.
 *
 * Named caches with per-cache TTL and size caps. Per-user caches (gamificationInfo,
 * collectionStats, userProfile) use username as the key; maximumSize=500 means the
 * 500 most recently accessed users are retained (LRU for the rest).
 *
 * Cache warm-up: taxonomy data is pre-loaded on startup to avoid first-request
 * latency spikes against the external Stardbi API.
 */
@Slf4j
@Configuration
@EnableCaching
@RequiredArgsConstructor
public class CacheConfig {

    private final StardbiClientPort stardbiClient;

    // ── Cache names ────────────────────────────────────────────────────────────

    public static final String CACHE_LEADERBOARD      = "leaderboard";
    public static final String CACHE_TAXONOMY         = "taxonomy";
    public static final String CACHE_COLLECTION_STATS = "collectionStats";
    public static final String CACHE_GAMIFICATION     = "gamificationInfo";
    public static final String CACHE_ACTIVE_TASKS     = "activeTasks";
    public static final String CACHE_TASK_DETAILS     = "taskDetails";
    public static final String CACHE_USER_PROFILE     = "userProfile";
    public static final String CACHE_PLATFORM_OVERVIEW = "platformOverview";

    // ── CacheManager ──────────────────────────────────────────────────────────

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager();

        // Each cache gets its own Caffeine spec via individual registration.
        // Spring Boot's CaffeineCacheManager supports per-cache specs when
        // caches are registered explicitly before any @Cacheable call.
        manager.registerCustomCache(CACHE_LEADERBOARD,
                Caffeine.newBuilder()
                        .maximumSize(10)
                        .expireAfterWrite(60, TimeUnit.SECONDS)
                        .build());

        manager.registerCustomCache(CACHE_TAXONOMY,
                Caffeine.newBuilder()
                        .maximumSize(1)
                        .expireAfterWrite(10, TimeUnit.MINUTES)
                        .build());

        manager.registerCustomCache(CACHE_COLLECTION_STATS,
                Caffeine.newBuilder()
                        .maximumSize(500)
                        .expireAfterWrite(60, TimeUnit.SECONDS)
                        .build());

        manager.registerCustomCache(CACHE_GAMIFICATION,
                Caffeine.newBuilder()
                        .maximumSize(500)
                        .expireAfterWrite(30, TimeUnit.SECONDS)
                        .build());

        manager.registerCustomCache(CACHE_ACTIVE_TASKS,
                Caffeine.newBuilder()
                        .maximumSize(50)
                        .expireAfterWrite(2, TimeUnit.MINUTES)
                        .build());

        manager.registerCustomCache(CACHE_TASK_DETAILS,
                Caffeine.newBuilder()
                        .maximumSize(100)
                        .expireAfterWrite(2, TimeUnit.MINUTES)
                        .build());

        manager.registerCustomCache(CACHE_USER_PROFILE,
                Caffeine.newBuilder()
                        .maximumSize(500)
                        .expireAfterWrite(60, TimeUnit.SECONDS)
                        .build());

        manager.registerCustomCache(CACHE_PLATFORM_OVERVIEW,
                Caffeine.newBuilder()
                        .maximumSize(1)
                        .expireAfterWrite(2, TimeUnit.MINUTES)
                        .build());

        return manager;
    }

    // ── Taxonomy warm-up ───────────────────────────────────────────────────────

    /**
     * Pre-populates the taxonomy cache once the application is fully started.
     * Eliminates first-request latency against the external Stardbi API.
     * If Stardbi is unavailable at startup, the cache populates lazily instead.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void warmTaxonomyCache() {
        try {
            log.info("Warming taxonomy cache on startup...");
            stardbiClient.getTaxonomy();
            log.info("Taxonomy cache warmed successfully.");
        } catch (Exception e) {
            log.warn("Taxonomy cache warm-up failed — will populate lazily on first request. Reason: {}", e.getMessage());
        }
    }
}
