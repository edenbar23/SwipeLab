# SwipeLab Analytics Architecture

This document details the `analytics` package, which provides rich data insights for both individual users and platform researchers.

## 1. Overview
The `AnalyticsService` synthesizes data from classifications, users, and tasks to provide leaderboards, performance metrics, confusion matrices, and time-series trends.

## 2. Data Sources and Aggregation
Analytics relies heavily on specialized, highly-indexed repositories rather than raw entity scans:
- **`ClassificationFactRepository`**: A denormalized or optimized view for querying accuracy, user performance aggregations, and species breakdowns.
- **`UserDailyStatsRepository` & `TaskDailyStatsRepository`**: Tables containing pre-aggregated statistics to quickly render time-series charts (e.g., "accuracy over the last 30 days").
- **`UserRankingRepository`**: Maintains percentile data and absolute ranks.

## 3. User-Scoped Analytics
Users can view their own performance:
- **Progress & Stats**: Fetches historical accuracy, total classifications, and streaks.
- **Trend**: Returns a daily breakdown of accuracy.
- **Comparisons**: Evaluates the user's accuracy against the global expert accuracy (`getUserVsExperts`) and calculates their percentile against the general user base (`getUserVsUsers`).
- **Breakdown**: Groups classifications by species to show category-specific accuracy.

## 4. Task-Scoped Analytics (Researchers)
Researchers monitor the health of their tasks:
- **Task Progress**: Calculates completion percentage by comparing classified images vs total images in the task.
- **Species Analytics**: Generates a **Confusion Matrix** (True/False Positives/Negatives) and agreement rates for every target species in the task, helping researchers identify difficult or ambiguous images.

## 5. Platform Overview (SuperAdmin)
A heavily cached, expensive query that provides a comprehensive platform snapshot:
- **Time Windows**: Shows activity (classifications, unique users, tasks) for "Today", "This Week", and "This Month".
- **Confidence Trend**: Tracks average user credibility scores over a 30-day window.
- **Label Distribution**: Shows the split of YES / NO / UNSURE labels over time.
