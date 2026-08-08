# SwipeLab Users Architecture

This document describes the `users` package, focusing on core user management, credibility calculation, and moderation.

## 1. Overview
The `users` package is the source of truth for user profiles, their composite credibility scores, and platform-wide moderation actions (banning/flagging).

## 2. User Profiles
- Managed by `UserService`. 
- Provides `UserProfileResponse`, which dynamically pulls in gamification data (like `currentStreak`) from the `gamification` package to present a unified frontend object.

## 3. Credibility Engine
The `CredibilityService` is a vital component that computes a user's reliability score (0-100). Researchers (`RESEARCHER` role) do not receive credibility scores.

### Score Composition
1. **Gold-Image Accuracy (40%)**: The strongest signal. Fraction of gold-standard images correctly identified.
2. **Majority Agreement (35%)**: How often the user aligns with the community consensus. Excludes image-query pairs that haven't met the minimum classifications threshold (to avoid penalizing early classifiers).
3. **Expert Agreement (25%)**: Cohen's Kappa score measuring agreement between the user and any `RESEARCHER` classifications on the same image.

*Note: Missing signals (e.g., user hasn't seen a gold image yet) automatically redistribute their weight to the available signals.*

### Calculation Flow
- Credibility is recalculated asynchronously either when consensus is reached on an image, or when an expert classifies an image.
- Results are saved back to the `User.credibilityScore` field.

## 4. Moderation and Malicious Labeling

### Automated Flagging
- During credibility calculation, the system evaluates the user's score against configurable thresholds (managed by `MaliciousLabelingConfigService`).
- If a user drops below the threshold (after completing a minimum number of classifications), they are automatically `flagged` and an `AdminNotification` is generated.
- Users who are warned count for 50% less weight (0.5) when calculating community consensus.

### Manual Actions
- SuperAdmins can ban or unban users via `UserService`. Banning sets `accountLocked = true` and `status = BANNED`, instantly severing access and emitting a `UserStatusChangedEvent` to clean up associated resources (like recipient groups).
