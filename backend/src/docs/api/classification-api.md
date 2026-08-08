# Classification API Endpoints

## Suspicious Activity
Base path: `/api/admin/suspicious-activity`

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/` | List flagged users |
| `GET` | `/{username}` | Get details for flagged user |
| `POST` | `/{username}/reset` | Reset flagged status for a user |

## Species Reference Images
Base path: `/api/v1/species`

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/reference-images` | Upload a new reference image |
| `GET` | `/{speciesName}/reference-images` | Get reference images by species name |
| `GET` | `/reference-images` | List all reference images |
| `DELETE` | `/reference-images/{id}` | Delete a reference image |
| `GET` | `/reference-images/{id}/image` | Serve reference image content |
| `GET` | `/reference-images/{id}/thumbnail` | Serve reference image thumbnail |

## Images
Base path: `/api/v1/images`

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/upload` | Upload a task image |
| `GET` | `/batch` | Get a batch of images |
| `GET` | `/{id}` | Get image details |
| `GET` | `/{id}/content` | Serve raw image content |

## Gold Images
Base path: `/api/admin/gold-images`

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/get-all` | List all gold images |
| `POST` | `/` | Create a gold image |
| `POST` | `/upload` | Upload and create a gold image |
| `GET` | `/` | (Legacy list all) |
| `GET` | `/{id}` | Get gold image details |
| `PUT` | `/{id}` | Update gold image |
| `DELETE` | `/{id}` | Delete gold image |

## Classifications
Base path: `/api/v1/classifications`

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/next-batch` | Get next batch of images for classification |
| `POST` | `/submit` | Submit a classification |
| `POST` | `/{classificationId}/submit` | (Legacy endpoint for submitting) |
| `POST` | `/tasks/{taskId}/play` | Start classifying a task |
| `POST` | `/tasks/{taskId}/batch` | Get batch of images scoped to a specific task |
