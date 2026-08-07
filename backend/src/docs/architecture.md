# SwipeLab Architecture Overview

SwipeLab is built using a strict modular **Hexagonal / Domain-Driven Design (DDD)** architecture. This ensures that the core business logic remains isolated from external concerns like the database, web frameworks, or third-party APIs.

## 1. Technology Stack
- **Language**: Java 21
- **Framework**: Spring Boot 3.2.x
- **Database**: PostgreSQL

## 2. Layered Structure
Every distinct business context (e.g., `tasks`, `users`, `gamification`, `classification`) is isolated into its own package and adheres strictly to the following layer hierarchy:

### `domain` (The Core)
Contains the business entities, value objects, and pure logic. 
- It defines **Ports** (interfaces) for anything it needs from the outside world (like repositories or external APIs).
- **Rule**: The domain layer must never depend on any other layer. It should ideally have zero dependencies on Spring Boot or JPA annotations, though light use of JPA annotations on entities is occasionally accepted for pragmatism.

### `application` (The Orchestrator)
Contains the use cases (Services).
- It coordinates the domain objects and handles transactions (`@Transactional`).
- It implements the business flows (e.g., "Assign a task to a user", "Calculate credibility").

### `infrastructure` (The Adapters)
Contains the implementations of the Ports defined by the domain layer.
- Database repositories (Spring Data JPA).
- External HTTP clients (e.g., Stardbi integration).
- Any technology-specific wiring.

### `api` (The Presenter)
Contains the REST controllers.
- Translates incoming HTTP requests (JSON) into application commands/queries.
- Returns standard DTOs.
- **Rule**: Controllers must never contain business logic. They simply delegate to the `application` layer.

## 3. Communication Between Contexts
To maintain loose coupling between different domains (e.g., `classification` and `gamification`), SwipeLab relies heavily on an **Event-Driven Architecture**.

- Instead of the `ClassificationService` directly calling the `GamificationService` or `CollectionService`, it simply publishes a `ClassificationSubmittedEvent`.
- Other modules implement `@EventListener`s to react to these events asynchronously (`@Async`), updating their own state without blocking the main classification thread.
- This allows new features (like analytics or new gamification rules) to be added without modifying the core classification logic.
