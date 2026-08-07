# SwipeLab Tasks Architecture

This document outlines the architecture, flow, and logic for managing classification tasks within SwipeLab.

## 1. Overview
The `tasks` package is responsible for defining the units of work (tasks) that users will classify. Tasks aggregate images and target specific species.

## 2. Task Lifecycle and Flow

### Creation
- When a `RESEARCHER` or `SuperAdmin` creates a task via `TaskService`, it is saved in the database with a `PROCESSING` status.
- **Background Sync**: An asynchronous process (`stardbiSyncService.syncExperimentsForTask()`) is kicked off immediately to pull the corresponding image crops from Stardbi if the task utilizes external data. 
- Once crops are fully ingested, the task becomes ready for classification.

### Modification and States
- Tasks can transition between several states: `ACTIVE`, `PAUSED`, and `ARCHIVED`.
- Only `ACTIVE` tasks are served to regular users.

## 3. Assignment and Visibility

Task visibility is highly controlled to ensure data goes to the right users.

- **Public Tasks**: If `isPublic` is true, the task can be explored and assigned by any user.
- **Assigned Users**: Tasks can explicitly list usernames in `assignedUsernames`.
- **Recipient Groups**: Tasks can be assigned to specific `RecipientGroups` (managed by the `recipients` package). `TaskService` checks the requesting user's group memberships to filter accessible tasks.

## 4. Serving Tasks to Users
When a user requests their dashboard:
1. `TaskService` retrieves the `RecipientGroups` the user belongs to.
2. It queries the `TaskRepository` for `ACTIVE` tasks that are either public, explicitly assigned to the user, or assigned to one of their groups.
3. The response is mapped, calculating the real-time progress (images classified vs total images).
