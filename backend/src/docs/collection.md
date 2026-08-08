# SwipeLab Collection Architecture

This document outlines the `collection` package, which manages users' personal collections of identified species.

## 1. Overview
SwipeLab includes a "Pokédex"-style feature where users can view all the images they have positively identified (swiped "YES"). This is managed by the `CollectionService`.

## 2. Flow and Logic

### Recording Entries
- When a user classifies an image with a `YES` response, a `UserCollectionEntry` is generated.
- **No Deduplication**: Each distinct `YES` swipe produces its own entry in the user's collection, even if they have identified the same species previously. This acts as a historical timeline of their positive findings.

### Fetching and Caching
- **Listing**: The collection is fetched ordered by `taggedAt` descending (newest first).
- **Stats**: Total collection counts are heavily cached using Spring Cache (`@Cacheable` with `CACHE_COLLECTION_STATS`). This ensures the UI can quickly render collection badges and counts without repeatedly querying the `UserCollectionEntry` table.

## 3. Event Handling
Similar to gamification, the collection is populated asynchronously. The `CollectionEventListener` listens for relevant domain events (like classifications) and delegates to `CollectionService.recordYesTag()` if the condition (a `YES` response) is met.
