# SwipeLab Configuration

This document outlines how SwipeLab handles runtime configurations, focusing heavily on the caching and audit mechanisms.

## 1. Runtime Configurations (Database)

System-wide parameters, particularly those related to malicious labeling and fraud detection, are stored in the database (`system_configuration` table) so they can be tuned at runtime by `SuperAdmin`s without restarting the application.

This is managed by the `MaliciousLabelingConfigService`.

### Caching and Consistency
Because the credibility evaluation runs on every classification, fetching these configs from the database would create a massive bottleneck.
- **Cache**: The config is loaded entirely into a Caffeine cache (`CACHE_MALICIOUS_LABELING_CFG`).
- **Atomic Updates**: When a SuperAdmin updates the config, the service persists the changes and uses `@CacheEvict` in the same transaction. This ensures that callers always see either the completely old config or the completely new config, preventing torn reads.

### Auditing
Every change to a configuration value is recorded in the `config_audit_log` table. This provides a historical record of exactly who changed what value and when, which is critical for understanding shifts in platform moderation behavior over time.

### Available Parameters
Examples of tunable parameters include:
- `maliciousThreshold`: The credibility score below which a user is flagged.
- `autoBanEnabled`: Whether the system should automatically lock accounts.
- `strikesForWarning1`, `strikesForWarning2`, `strikesForBan`: The thresholds for the strike system based on consecutive suspicious actions.

## 2. Spring Caching (`CacheConfig`)

SwipeLab utilizes Caffeine for local, in-memory caching to optimize read-heavy paths.

- **Per-Cache Specs**: Instead of a generic cache manager, each cache (e.g., `leaderboard`, `gamificationInfo`, `userProfile`) is registered with explicit maximum sizes and Time-To-Live (TTL) policies.
- **LRU Eviction**: For user-specific caches, the maximum size acts as a Least Recently Used (LRU) limit, ensuring the server doesn't run out of memory during traffic spikes while keeping active users fast.
- **Warm-up**: The application listens for `ApplicationReadyEvent` to proactively pre-fetch and cache slow external data (like the Stardbi taxonomy) on startup, removing the latency penalty for the first user request.
