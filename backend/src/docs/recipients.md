# SwipeLab Recipients Architecture

This document details the `recipients` package, which is responsible for grouping users for targeted task distribution.

## 1. Overview
Instead of assigning tasks to users one by one, researchers can organize users into `RecipientGroup` entities. These groups can then be attached to tasks to control visibility and distribution.

## 2. Entities
- **RecipientUser**: Represents a flattened reference to a SwipeLab user within the recipients context.
- **RecipientGroup**: A named group containing a set of `RecipientUser` entities. 

## 3. Flow and Logic

### Group Creation and Updates
- Handled by `RecipientGroupService`.
- When adding users to a group by username, the service uses `getOrCreateRecipientUsers`. This ensures that even if a user hasn't actively engaged with groups yet, a `RecipientUser` reference is safely created or linked.
- The `userCount` is dynamically maintained based on the size of the set of associated `RecipientUser`s.

### Integration with Tasks
- The `Task` entity holds a set of `RecipientGroup` IDs. 
- During task fetch operations in `TaskService`, a user's memberships are cross-referenced against the task's assigned group IDs.

### Integration with Users
- The package listens for global user events. For example, if a user is banned (handled by `UserService`), a `UserStatusChangedEvent` is published. The recipients module can catch this to remove or deactivate the user from active recipient lists.
