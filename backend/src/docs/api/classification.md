# Classification API Quick Reference

This document outlines the REST API endpoints provided by the classification module.

## 1. Classifications (`/api/v1/classifications`)
Handles user interactions for swiping and fetching images to classify.

- **`GET /next-batch`**
  - Fetches the next batch of images for the authenticated user to classify.
  - Query params: `count` (default: 10), `taskId`.

- **`POST /submit`**
  - Submits a classification decision (swipe right/left) for an image.
  - Body: `SubmitClassificationRequest` (includes `imageId`, `taskId`, `decision`, `responseTimeMs`).

- **`POST /{classificationId}/submit`** *(Legacy)*
  - Legacy endpoint for submitting a classification decision using a path variable.

- **`POST /tasks/{taskId}/play`**
  - Initializes a classification session for a specific task and returns the first batch of images.
  - Query params: `count` (default: 10).

- **`POST /tasks/{taskId}/batch`** *(Legacy)*
  - Submits a batch of classifications at once.

---

## 2. Images (`/api/v1/images`)
Core endpoints for uploading and serving regular images to be classified.

- **`POST /upload`**
  - Uploads a new image.
  - Body: `ImageUploadRequest`.

- **`GET /batch`**
  - Gets a batch of images for a specific task, filtering out images the user has already classified.
  - Query param: `taskId`.

- **`GET /{id}`**
  - Gets the metadata of a specific image.

---

## 3. Gold Images (`/api/admin/gold-images`)
Admin/Researcher endpoints for managing "known-answer" images used to evaluate user credibility. Requires `RESEARCHER` or `SUPER_ADMIN` roles.

- **`GET /get-all`**
  - Retrieves all gold images across the system.

- **`POST /`**
  - Creates a gold image from metadata.

- **`POST /upload`**
  - Uploads a gold image file directly via multipart form data.
  - Params: `file`, `imageUrl`, `species`, `correctAnswer`.

- **`GET /?taskId={taskId}`**
  - Retrieves gold images associated with a specific task.

- **`GET /{id}`**
  - Gets the details of a specific gold image.

- **`PUT /{id}`**
  - Updates an existing gold image.

- **`DELETE /{id}`**
  - Deletes a gold image.

---

## 4. Species Reference Images (`/api/v1/species`)
Endpoints for managing the pool of reference images for different species. 
*(Write endpoints require `RESEARCHER` or `SUPER_ADMIN`)*

- **`POST /{speciesName}/reference-images`**
  - Uploads reference images (1-3) for a specific species.

- **`GET /{speciesName}/reference-images`**
  - Gets the reference images pool for a specific species.

- **`GET /reference-images?speciesNames=A,B,C`**
  - Batch fetches pool images for multiple species in a single request.

- **`DELETE /reference-images/{id}`**
  - Deletes an image from the reference pool.

- **`GET /reference-images/{id}/image`**
  - Streams the compressed full-resolution reference image.

- **`GET /reference-images/{id}/thumbnail`**
  - Streams a 200px thumbnail of the reference image.

---

## 5. Suspicious Activity (`/api/admin/suspicious-activity`)
Admin/Researcher endpoints for auditing user behavior and fraud detection. Requires `RESEARCHER` or `SUPER_ADMIN` roles.

- **`GET /`**
  - Retrieves the full audit log of suspicious activities (all users, all severities).

- **`GET /{username}`**
  - Retrieves the suspicious activity log filtered for a specific user.

- **`POST /{username}/reset`** *(SUPER_ADMIN only)*
  - Clears a user's strike counter and restores their status to ACTIVE. Used for false-positive remediation.
