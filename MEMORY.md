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

## Current Focus (Active GitHub Issues)
* Issue #204: Fix SwipeScreen showing mock task by default.
* Issue #205: Fix logic causing same task to appear twice in Assigned/Explore.
* Issue #206: Update UI/UX for the Gold Images Upload screen.
* Issue #201: Refactor Backend roles to include Researchers and Super Admin.
* Issue #154: Add version support in API responses for frontend caching.
* Issue #226: [Frontend] fix recipients list not deleting users.
* Issue #225: [System] fix refresh token for Researcher.
* Issue #224: [Frontend] Mobile Vs. Web compatibility enhancement.
* Issue #223: [Frontend] add a small information in the Gold Images Screen & Add Gold Images.
* Issue #222: [Frontend] fix TasksManagementScreen UI.
* Issue #221: [Frontend] adapt to changes of analytics new endpoints.
* Issue #220: [Backend] improve and add analytics new endpoints.
* Issue #219: [Frontend] Add to UsersScreen ONLY for Admin role control buttons.
* Issue #218: [Frontend] adding sort by credibility score in UsersScreen.
* Issue #217: [Frontend] adding search bar for users in the UsersScreen.

## Architecture & Design Decisions
* Backend: Strictly modular. No direct calls from API to Infrastructure. Use Application services.
* Frontend: Global state via Zustand, Server state via React Query. 
* Navigation: Custom @react-navigation stack (No Expo Router).
* Database: All schema changes must use Flyway migrations.

## Agent Guidelines
* Update this file only upon task completion or major architectural changes.
* Use LOCAL_SCRATCHPAD.md for internal logs and step-by-step execution details.