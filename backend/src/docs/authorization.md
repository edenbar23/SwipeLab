# SwipeLab Authorization Architecture

This document outlines the authorization flow, available roles, their permissions, and how authorization is implemented within the SwipeLab backend.

## Authorization Flow

SwipeLab uses **Method-Level Security** in Spring Boot, driven primarily by `@PreAuthorize` annotations on controller endpoints.

1. **Token Processing**: When a request arrives, the `JwtAuthenticationFilter` intercepts it and extracts the JWT token from the `Authorization` header.
2. **Context Establishment**: The JWT token is validated. The user's role, embedded within the token's claims, is extracted and used to build an `Authentication` object, which is then placed in the Spring Security context.
3. **Endpoint Access Control**: When a request reaches a controller method, Spring Security evaluates the `@PreAuthorize` annotation against the current `Authentication` context to determine if the user has the required authorities or meets specific dynamic conditions.

## Available Roles and Permissions

The system defines two primary roles within the `UserRole` enum (`USER` and `RESEARCHER`), alongside a special dynamic `SuperAdmin` designation.

### 1. USER
The standard role assigned to native SwipeLab users and general Google Auth users.
- **Permissions**:
  - Manage their own profile and authentication credentials.
  - Fetch classification tasks and submit classifications.
  - Participate in gamification and view leaderboards/ranks.
  - Access public collections.

### 2. RESEARCHER
An elevated role assigned to Stardbi researchers and explicitly invited administrative users.
- **Permissions**:
  - **All `USER` permissions.**
  - **Task Management**: Create and manage tasks, and manage task metadata.
  - **Classification Management**: Access and manage Gold Images and Species Reference Images.
  - **Recipient Groups**: Create and manage groups for task distribution.
  - **Analytics**: Access analytics dashboards and export data.
  - **Monitoring**: View suspicious activity logs.

### 3. SuperAdmin
The SuperAdmin is not a distinct enum role, but rather a dynamic privilege level verified via `@securityAuthorizationService.isSuperAdmin(authentication.name)`. The SuperAdmin's identity is defined via environment variables (`app.security.super-admin.username`) and initialized on application startup via `SuperAdminRoleInitializer` (which ensures they are granted the `RESEARCHER` role).
- **Permissions**:
  - **All `RESEARCHER` permissions.**
  - **System Configuration**: Access configuration endpoints (e.g., `MaliciousLabelingConfigController`).
  - **Integration Sync**: Trigger manual synchronizations with external systems (e.g., `SyncController` for Stardbi).
  - **Admin Notifications**: Manage system-wide administrative notifications.
  - **User Management**: Perform privileged user management actions.

## Implementation Details

Authorization is enforced using SpEL (Spring Expression Language) within `@PreAuthorize` annotations.

**Common Patterns:**
- **User or Researcher Access**:
  ```java
  @PreAuthorize("hasAnyRole('USER', 'RESEARCHER') or @securityAuthorizationService.isSuperAdmin(authentication.name)")
  ```
- **Researcher Only Access**:
  ```java
  @PreAuthorize("hasRole('RESEARCHER') or @securityAuthorizationService.isSuperAdmin(authentication.name)")
  ```
- **SuperAdmin Only Access**:
  ```java
  @PreAuthorize("@securityAuthorizationService.isSuperAdmin(authentication.name)")
  ```

*Note: The SuperAdmin check is frequently paired with role checks to serve as a fallback or override, ensuring the SuperAdmin retains access to all endpoints even if standard roles are modified.*
