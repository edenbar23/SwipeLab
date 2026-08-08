# SwipeLab Gold Images Architecture

This document outlines the architecture, policy, and impact of "Gold Images" within the SwipeLab classification ecosystem. 

Gold Images are pre-labeled, known-truth images injected into a user's classification flow to test their accuracy. They serve as the strongest signal (hard ground truth) for evaluating a user's credibility.

---

## 1. Creation and Management

Gold Images are managed by users with the `RESEARCHER` or `SuperAdmin` role via the `GoldImageController`.

- **Upload/Creation**: A gold image can be created by uploading a new image file or providing an external URL alongside a known `species` and the `correctAnswer` (e.g., `YES`, `NO`, `UNSURE`). This logic is handled by `GoldImageService`.
- **Storage Implementation**: 
  - All gold images are stored inline within the database as **Base64 encoded Data URIs**. 
  - If a file is uploaded directly, it is converted to a Base64 string. 
  - If an external URL is provided, the backend downloads the image over HTTP, converts it to Base64, and stores the resulting string. 
  - A standard `Image` entity is created with `taskId = null` (to distinguish it from normal task images) and its `imageData` contains the Base64 string. The `GoldImage` entity then holds a foreign key reference to this `Image` alongside the correct answer.
- **Association**: Gold Images are tied to specific species. When tasks are generated, the system maps available Gold Images that match the task's target species.
- **Soft Deletion**: When a Gold Image is deleted, it is only soft-deleted (`active = false`). This prevents violating foreign key constraints for historical credibility records where users have previously classified that specific image.

## 2. Serving Policy

To prevent users from realizing they are being tested, Gold Images are seamlessly blended into standard classification batches. 

The decision of when to serve a Gold Image is governed by the `GoldImagePolicy` interface, specifically implemented by **`FrequencyBasedGoldImagePolicy`**.

- **Frequency Rule**: By default, the policy dictates serving **1 gold image per 15 regular classifications** (`GOLD_IMAGE_FREQUENCY = 15`).
- **Mechanism**: When a user requests a batch of tasks, the system calculates their total classifications for that task (`countByUsernameAndTaskId`). If adding the new batch size pushes their count across a multiple of 15, a Gold Image is injected into their queue.

## 3. Credibility Impact

Gold Images are the most heavily weighted component of a user's composite credibility score, as they represent incontrovertible ground truth (unlike community consensus which can occasionally be wrong).

- **Weighting**: Gold-image accuracy constitutes **40%** of a user's total credibility score (calculated by `CredibilityService`).
- **Calculation**: Accuracy is computed as the fraction of gold classifications where the user's submitted response exactly matches the `correctAnswer` stored on the `GoldImage` entity.
- **Cold Start**: If a user has not yet encountered any Gold Images, they are not penalized. The 40% weight is redistributed among other available signals (like majority agreement or expert kappa) until they build a gold-image history.
- **Record Keeping**: The results of Gold Image classifications are tracked on the `User` entity (`correctGoldClassifications` and `totalGoldClassifications`) for rapid dashboard display.

## 4. Flow Summary

1. **Setup**: A `RESEARCHER` creates a Gold Image for species X.
2. **Batch Request**: A `USER` requests a batch of tasks for a task targeting species X.
3. **Policy Evaluation**: The `FrequencyBasedGoldImagePolicy` determines the user is due for a test.
4. **Injection**: The Gold Image is disguised as a normal classification task and added to the user's batch.
5. **Submission**: The user submits their classification.
6. **Evaluation**: The system recognizes the image as a Gold Image, compares the user's response to the stored `correctAnswer`, and triggers `CredibilityService.updateUserCredibility()`.
7. **Score Update**: The user's composite credibility score is recalculated based on their updated Gold Image accuracy.
