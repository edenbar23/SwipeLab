# Project Memory - SwipeLab

## Project Overview
SwipeLab is a gamified, human-in-the-loop image labeling platform. 
Stack: Java 21 Spring Boot (Backend) and React Native Expo (Frontend).
Architecture: Modular Hexagonal/DDD.

## Recent Milestones
* CI/CD: Fixed backend GitHub Actions workflow by adding Maven Wrapper execution files (Issue #196).
* Frontend/Backend: Fixed gold images page rendering issue by adding imageUrl to DTO and updating frontend parsing logic (Issue #177).
* Frontend: Integrated pause and archive buttons with the backend task API (PR #207).
* Backend: Decoupled gold image logic by creating GoldImagePolicy and GoldImageEvaluatorService.
* Database: Schema migrations updated (V1-V8) including gamification and challenges.

## Current Focus (Active GitHub Issues)
* Issue #204: Fix SwipeScreen showing mock task by default.
* Issue #205: Fix logic causing same task to appear twice in Assigned/Explore.
* Issue #206: Update UI/UX for the Gold Images Upload screen.
* Issue #201: Refactor Backend roles to include Researchers and Super Admin.
* Issue #196: Implement CI/CD pipeline for unit tests on Pull Requests.
* Issue #154: Add version support in API responses for frontend caching.

## Architecture & Design Decisions
* Backend: Strictly modular. No direct calls from API to Infrastructure. Use Application services.
* Frontend: Global state via Zustand, Server state via React Query. 
* Navigation: Custom @react-navigation stack (No Expo Router).
* Database: All schema changes must use Flyway migrations.

## Agent Guidelines
* Update this file only upon task completion or major architectural changes.
* Use LOCAL_SCRATCHPAD.md for internal logs and step-by-step execution details.