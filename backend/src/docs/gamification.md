# SwipeLab Gamification Architecture

This document describes the event-driven gamification system that incentivizes user participation.

## 1. Overview
The `gamification` package calculates points, streaks, ranks, badges, and challenge progress based on user actions. It is decoupled from the core classification logic and operates entirely via domain events.

## 2. Event-Driven Architecture

The core driver is the `GamificationOrchestratorService`, which listens for the `ClassificationSubmittedEvent`. 

When an event fires asynchronously, the orchestrator triggers a pipeline of updates:

### Streaks
- `StreakService.updateStreak(username)` updates the user's consecutive day activity.

### Points
- Base points (e.g., 10 pts) are awarded for the classification via `PointsService`.
- If the classification was on a Gold Image AND the user got it correct, a massive bonus (e.g., 50 pts) is awarded.

### Rank and YES Tags
- If the user swiped `YES` on an image, their `yesTagCount` is incremented.
- The `RankService` computes a new `RankTier` (e.g., BRONZE, SILVER, GOLD, PLATINUM) based strictly on their `yesTagCount`.

### Badges and Challenges
- `BadgeService` checks milestone thresholds (e.g., "100 Classifications") and awards badges.
- `ChallengeEngine` processes the latest streak and points to update progress for active time-boxed challenges.

## 3. Storage
Gamification state is primarily stored on the `Gamification` entity, separate from the primary `User` entity, keeping the auth/profile footprint lightweight. The `UserService` hydrates gamification stats (like `currentStreak`) onto the user profile response dynamically.
