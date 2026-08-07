# Classification Architecture

## Overview
The Classification module in SwipeLab manages the core business logic of users categorizing (swiping) images. It is designed using a **Hexagonal / Domain-Driven Design (DDD)** architecture. This ensures a strict separation of concerns between external interfaces, application orchestration, core business rules, and infrastructure.

## Package Structure (Layers)
The classification package is structured strictly into the following layers:

- **`api`**: Contains REST Controllers (`ClassificationController`, `GoldImageController`, etc.). Responsible for handling incoming HTTP requests, validating inputs, and delegating to the application layer.
- **`application`**: Contains application services (`ClassificationService`, `GoldImageEvaluatorService`, etc.). This layer orchestrates use cases, manages transactions, and coordinates domain services. It also defines output ports (interfaces) for external communication.
- **`domain`**: Contains the core business logic, entities, and value objects. This is the heart of the classification system and has zero dependencies on external frameworks (like Spring Web or JPA). It is further divided into sub-domains:
  - `core`: Basic classification entities and logic.
  - `threshold`: Logic for determining if an image has received enough votes to be considered "classified" (e.g., credibility-weighted threshold).
  - `distribution`: Handles distributing tasks and images to users.
  - `fraud`: Analyzes user behavior (e.g., response time) to detect bots or malicious actors, issuing warnings or bans.
  - `goldimage`: Manages "known-answer" images injected into the user's feed to dynamically evaluate and adjust their credibility score.
  - `image`: Core image entity and state management.
- **`infrastructure`**: Contains the Spring Data JPA repositories and actual database integrations (adapters).
- **`events`**: Defines domain events used for decoupled, asynchronous communication within and across modules.
- **`dto`**: Data Transfer Objects used for communication between the API layer and the Application layer, or returned in HTTP responses.

---

## Classification Flow
The primary use case is when a user submits a classification (e.g., swiping right or left on an image). The flow in `ClassificationService` is as follows:

1. **Fraud Detection**: Before processing, the `FraudDetectionService` evaluates the submission (checking response time, user role, etc.). If malicious behavior is detected, it can instantly ban the user or attach a warning.
2. **Image Retrieval**: The target `Image` is loaded from the repository.
3. **Gold Image Evaluation**: The `GoldImageEvaluatorService` checks if the submitted image was a "Gold Image" (a test image with a known answer). If so, the user's answer is evaluated for correctness.
4. **Persistence**: A `Classification` entity is constructed and saved to the database.
5. **Event Publishing**: A `ClassificationSubmittedEvent` is published. This is a crucial decoupling point. Other components listen to this event to perform asynchronous tasks, such as:
   - Adjusting the user's credibility score (`CredibilityEventListener`).
   - Evaluating if the image has reached a definitive classification result (`ThresholdEventListener`).
6. **Consensus Evaluation (Async)**: The `ThresholdEventListener` catches the event, fetches the dynamic `consensusThreshold` from the task, and checks if the combined credibility weight of all answers for a specific species meets or exceeds the threshold.
   - If consensus is reached, a `ConsensusResult` is saved to the database.
   - Images with a `ConsensusResult` for all valid task species are automatically excluded by the `TaskDistributionService` from future user feeds.
7. **Next Batch Delivery**: The service automatically fetches and returns the next batch of images for the user to classify, ensuring a seamless UI experience.

---

## Guidelines for Maintenance & Extension

When modifying or extending the classification module, adhere strictly to the following rules:

1. **Maintain Architectural Boundaries**:
   - Do not leak API concepts (like `HttpServletRequest`) into the Application or Domain layers.
   - Do not leak persistence concepts (like `@Entity` or Spring Data specific logic) into the Domain layer if possible. (Note: currently some domain entities might use JPA annotations for simplicity, but business logic should not depend on JPA).
2. **Event-Driven Side Effects**: 
   - If you need to add a new side-effect when a classification happens (e.g., notifying a webhook, awarding points), **do not** add it directly to `ClassificationService`. Instead, create a new event listener that subscribes to `ClassificationSubmittedEvent`.
3. **Keep Application Services Thin**:
   - `ClassificationService` should only coordinate. Any complex logic (like calculating thresholds, detecting fraud, or picking the next images) must be encapsulated within dedicated `domain` services.
4. **Testing Requirements**:
   - Every modification must include corresponding tests.
   - Test at least one **happy path** and one **edge case/failure path**.
5. **Security**:
   - All external inputs must be validated at the `api` layer.
   - Fraud detection relies heavily on accurate client-side timestamps. Be cautious when modifying the DTOs involving response times.
