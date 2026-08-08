# Image Storage Architecture

SwipeLab uses a fully database-backed image storage architecture, where all image payloads are stored directly in the PostgreSQL database as **Base64 encoded strings** or **Data URIs** within the `image_data` column of the `images` table.

## 1. Storage Flow

1. **Ingestion**: 
   - When a task is synced from an external provider (like Stardbi) or a Gold Image is uploaded via the admin panel, the source image is obtained.
   - If the source is an external HTTP URL, the backend downloads the image bytes in-memory.
   - If the source is a direct file upload (e.g., MultipartFile), the bytes are extracted.
2. **Encoding**:
   - The raw byte array is converted into a standard Base64 string.
   - For Gold Images and local uploads, it is prepended with a Data URI scheme (e.g., `data:image/jpeg;base64,...`).
3. **Persistence**:
   - The string is saved into the `image_data` column of the `Image` entity.
4. **Retrieval**:
   - When the frontend requests a batch of images for classification, the `ImageService` maps the `Image` entity to an `ImageResponse` DTO.
   - The `image_data` string is sent directly as the `imageUrl` property in the standard JSON response.
   - The frontend's `<img src="..." />` tag natively parses and renders the Base64/Data URI string without requiring any additional HTTP requests for binary files.

## 2. Justification for Base64 Database Storage

Storing binary files directly in a relational database as Base64 is often considered an anti-pattern for large media applications. However, in SwipeLab's specific domain, this architecture provides significant operational advantages:

### A. Small Image Footprint
The images processed by SwipeLab (specifically those synced from Stardbi) are **small, tightly cropped bounding boxes** of individual insects or animals. They are typically only a few kilobytes in size. The storage overhead of Base64 encoding (~33% increase) is negligible at this scale.

### B. Stateless Infrastructure
By removing the reliance on a local filesystem (`/uploads/`), the backend Docker container becomes completely **stateless**. 
- Containers can be destroyed, restarted, or scaled horizontally without worrying about attached persistent volumes (`volumes`) or losing uploaded Gold Images.
- We avoid the operational complexity and cost of provisioning external blob storage (like AWS S3) for a dataset that comfortably fits within standard database limits.

### C. Simplified Backups and Migrations
Because the images live alongside the classification tasks and user records, a single `pg_dump` captures the **entire state of the application**, including media. Restoring a database backup guarantees that all foreign keys (e.g., `Classifications` referencing `Images`) remain perfectly intact without risking broken file links.

### D. API Efficiency
Serving image paths (like `/api/v1/images/123/content`) requires the frontend to make an initial JSON request to get the metadata, followed by N additional HTTP requests to fetch the actual image binaries. 
By embedding the Base64 payload directly into the batch JSON response, the frontend can render an entire queue of 20 images using a single, cohesive HTTP request, significantly reducing network latency and connection overhead.
