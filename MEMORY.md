# Project Memory - SwipeLab

## Project Overview
SwipeLab is a gamified, human-in-the-loop image labeling platform. 
Stack: Java 21 Spring Boot (Backend) and React Native Expo (Frontend).
Architecture: Modular Hexagonal/DDD.

## Recent Milestones
* Architecture: Removed all unused Kafka configurations and dependencies; confirmed full decoupling via Spring ApplicationEventPublisher.
* Frontend: Fixed persistent mobile web layout and viewport scaling bugs (100dvh migration, locked root overflow, iOS input auto-zoom prevention via fontSize 16).
* CI/CD: Implemented and optimized backend GitHub Actions CI pipeline with Maven caching (`cache-dependency-path`) and wrapper execution (Issue #196).
* Frontend/Backend: Fixed gold images page rendering issue by adding imageUrl to DTO and updating frontend parsing logic (Issue #177).
* Frontend: Integrated pause and archive buttons with the backend task API (PR #207).
* Backend: Decoupled gold image logic by creating GoldImagePolicy and GoldImageEvaluatorService.
* Database: Schema migrations updated (V1-V8) including gamification and challenges.
* Frontend: Fixed frontend rendering for uploaded Gold Images by correcting the static path base parsing logic (Issue #216).
* Security/Validation: Implemented comprehensive input validation, stored XSS protection, and username normalization (Issue #227).
* Frontend/Auth: Fixed a persistent navigation bug where `isSuperAdmin` state was lost on app refresh, causing the Users screen and toolbar option to disappear for Super Admins.
* Frontend: Removed hardcoded mock task (id=1) from SwipeScreen; implemented 3-state Quick Start UI (active swipe / quick-start task picker / true empty state); aligned all task play entry points with swipeStore (Issue #204).
* Backend/Frontend: Fixed task duplication bug — added `findPublicTasksExcludingAssignedUser` JPQL query, `getExploreTasksForUser` service method, `POST /tasks/{id}/assign` endpoint with duplicate guard (DuplicateResourceException → HTTP 409), and `useAssignTask` mutation in the frontend (Issue #205).
* Frontend: Added search bar + sort-by-credibility-score toggle to UsersManagementScreen; updated mock data (Issues #217, #218).
* Frontend: Added information sections to GoldImagesManagementScreen and AddGoldImageScreen to clarify their purpose for researchers (Issue #223).
* Backend: Implemented analytics overview endpoint + implemented placeholder endpoints — added `GET /api/v1/analytics/overview` (platform-wide time-windowed stats: classifications/images/users/tasks/experiments for today/week/month, 30-day confidence trend, label distribution), added `GET /api/v1/analytics/global-stats`, implemented `getUserPerformanceMetrics` and `getTopPerformers`, added V13 Flyway migration for analytics tables, fixed missing leading `/` on admin endpoints (Issue #220).
* Frontend: Redesigned AnalyticsScreen with two-tab layout (Overview / Tasks). Overview tab consumes new `/api/v1/analytics/overview` endpoint and renders: TimeWindowCards (Today/Week/Month activity), platform totals, ConfidenceTrendChart (30-day credibility sparkline), LabelDistributionBar (YES/NO/DONT_KNOW/TRASH), and top performers. Tasks tab lists all researcher tasks with expandable per-task analytics panel. Added analyticsTypes.ts, 3 new components, 2 new query hooks, mock routes (Issue #221).
* Backend/Frontend: Completed integration of the Fraud Detection System banning flow (added global 403 `ACCOUNT_BANNED` interception in frontend `apiFetch`, implemented backend `BannedUserFilter` to secure API access, and updated `UserService` manual bans to mirror the auto-ban state).

## Current Focus (Active GitHub Issues)
* Issue #201: Refactor Backend roles to include Researchers and Super Admin.
* Issue #154: Add version support in API responses for frontend caching.
* Issue #226: [Frontend] fix recipients list not deleting users.
* Issue #225: [System] fix refresh token for Researcher.
* Issue #224: [Frontend] Mobile Vs. Web compatibility enhancement.
* Issue #222: [Frontend] fix TasksManagementScreen UI.
* Issue #219: [Frontend] Add to UsersScreen ONLY for Admin role control buttons.

## Architecture & Design Decisions
* Backend: Strictly modular. No direct calls from API to Infrastructure. Use Application services.
* Frontend: Global state via Zustand, Server state via React Query. 
* Navigation: Custom @react-navigation stack (No Expo Router).
* Database: All schema changes must use Flyway migrations.

## Agent Guidelines
* Update this file only upon task completion or major architectural changes.
* Use LOCAL_SCRATCHPAD.md for internal logs and step-by-step execution details.