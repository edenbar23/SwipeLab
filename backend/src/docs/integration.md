# SwipeLab Integration Architecture

This document describes the `integration` package within the SwipeLab backend. This package is designed to handle communication and synchronization with external systems. 

Currently, the only supported external integration is **Stardbi**, a third-party platform for biological data management and experiment tracking.

## Architecture Overview

The integration layer follows a Hexagonal (Ports and Adapters) architectural style to ensure that external dependencies do not bleed into the core domain logic.

- **Ports**: Interfaces that define the required operations for an external system.
- **Adapters**: Implementations of these ports that handle the actual HTTP/network communication.
- **Services**: Application services that orchestrate the synchronization of data between the external system and SwipeLab's internal database.

---

## Stardbi Integration

The Stardbi integration handles bidirectional synchronization: pulling down experiments and images (crops) for classification, and pushing back user classifications (labels).

### 1. Client and Communication
- **`StardbiClientPort`**: The interface defining all allowed external API calls to Stardbi (e.g., `getExperiments`, `getCrops`, `downloadCropsZip`, `postLabel`, `getTaxonomy`).
- **`StardbiClient`**: The primary implementation of the port using `RestTemplate` or similar HTTP clients to make actual network calls to Stardbi.
- **`MockStardbiClient`**: A mock implementation used for local development and testing to simulate Stardbi API responses without needing a live connection.
- **`StardbiTokensDto`**: A data transfer object used to store cached access and refresh tokens. Authentication is handled by `StardbiAuthService` (located in the `auth` package) which acts as a BFF (Backend-For-Frontend) proxy to cache tokens for external API calls.

### 2. Inbound Data Sync (Pull)
Data is pulled from Stardbi into SwipeLab via the **`StardbiSyncService`**.

- **Manual Trigger**: The `SyncController` exposes an endpoint (restricted to `SuperAdmin` users) to manually trigger the sync process.
- **Process**:
  1. The service fetches a list of experiments using the `StardbiClient`.
  2. For each experiment, it fetches the list of image crops (bounding boxes).
  3. It downloads the actual image data (often streamed in a ZIP file) and extracts it.
  4. It provisions SwipeLab `Task` and `Image` entities in the local database for these crops so that users can classify them natively.

### 3. Outbound Data Sync (Push)
When a SwipeLab user classifies an image that originated from Stardbi, that classification must be pushed back to the Stardbi platform. This is handled asynchronously to prevent blocking the user's classification flow.

- **`StardbiClassificationEventListener`**: Listens for the `ClassificationSubmittedEvent`.
- **Flow**:
  1. When a user submits a classification, the core domain publishes an event.
  2. The listener catches this event and checks if the `Task` originated from `"STARDBI"`.
  3. It fetches the parent image details and the external box ID.
  4. It resolves the string species name to a numeric `species_id` using an in-memory `ConcurrentHashMap` cache populated from Stardbi's Taxonomy endpoint.
  5. It constructs an `ExternalLabelDto` and pushes it via `StardbiClient.postLabel()`.
- **Resilience**: The listener uses Spring Retry (`@Retryable`). If the Stardbi API is temporarily down, the push will automatically be retried with an exponential backoff. If it completely fails, it falls back to a `@Recover` method (which logs a severe error for potential manual Dead-Letter Queue processing).

---

## Adding Future Integrations

To add a new integration (e.g., a different database or platform):
1. Create a new sub-package under `com.swipelab.integration` (e.g., `com.swipelab.integration.newdb`).
2. Define a Client Port interface specific to that system.
3. Implement the Client Adapter to handle the specific API dialect (REST, GraphQL, gRPC).
4. Create a Sync Service to handle pulling data and mapping it to SwipeLab's internal domain models (`Task`, `Image`).
5. Use Event Listeners to asynchronously push data back to the external system when internal events (like `ClassificationSubmittedEvent`) occur.
