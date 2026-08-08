# ADR 001: Event-Driven Consensus Threshold Evaluation

## Context
When a user submits a classification for an image, the system must evaluate whether the combined credibility weights of all responses for that image have reached a defined threshold (e.g., 3.0). If the threshold is reached, the image should no longer be distributed to users for that specific species. 

Initially, this logic could have been placed synchronously inside the `ClassificationService.submitClassification` method.

## Decision
We decided to decouple the threshold evaluation logic from the main classification submission flow by using a Spring `@TransactionalEventListener` wrapped in an `@Async` method (`ThresholdEventListener`).

## Consequences
### Positive
1. **Performance**: The `submitClassification` API endpoint returns immediately after persisting the user's vote and fetching the next batch, without blocking to evaluate the threshold logic against potentially hundreds of prior classifications.
2. **Decoupling**: `ClassificationService` remains unaware of the threshold domain logic, adhering strictly to the Hexagonal Architecture and keeping application services thin.
3. **Resilience**: A failure in threshold evaluation (e.g., database constraint violations or complex logic bugs) will not roll back the user's valid classification submission or crash their UI experience.

### Negative
1. **Testing Complexity**: Writing integration tests requires careful handling. Since the listener is configured for `TransactionPhase.AFTER_COMMIT`, tests cannot rely on the default `@Transactional` behavior (which rolls back and thus never fires `AFTER_COMMIT` events). Test methods must manage database cleanup manually and use utilities like `Thread.sleep` or `Awaitility` to assert asynchronous results.
2. **Race Conditions**: In highly concurrent environments, multiple users might submit the deciding vote simultaneously. We mitigate this by checking `consensusResultRepository.findByImageIdAndSpecies` both at the start of the async listener and right before persisting the result.
