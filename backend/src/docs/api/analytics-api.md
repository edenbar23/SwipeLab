# Analytics API Endpoints

Base paths: multiple (`/api/v1/...`)

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/v1/classifications/progress` | User progress stats |
| `GET` | `/api/v1/statistics/me` | User statistics overview |
| `GET` | `/api/v1/statistics/me/vs-experts` | Compare user accuracy vs experts |
| `GET` | `/api/v1/statistics/me/vs-users` | Compare user accuracy vs general users (percentile) |
| `GET` | `/api/v1/statistics/me/breakdown` | User accuracy breakdown by species |
| `GET` | `/api/v1/statistics/me/timeseries` | User accuracy timeseries trend |
| `GET` | `/api/v1/analytics/overview` | Platform-wide overview (SuperAdmin/Researcher) |
| `GET` | `/api/v1/analytics/global-stats` | Quick global dashboard stats |
| `GET` | `/api/v1/analytics/tasks/{taskId}` | Task-specific analytics and confusion matrix |
| `GET` | `/api/v1/analytics/users` | List user performance metrics for a task |
| `GET` | `/api/v1/analytics/top-performers` | List top performing users platform-wide |

## Export API
Base path: `/api/v1/admin/export`

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/classifications/csv` | Export classifications dataset |
| `GET` | `/tasks/{taskId}/summary` | Export task summary |
| `GET` | `/tasks/{taskId}/csv` | Export task data as CSV |
| `GET` | `/tasks/{taskId}/json` | Export task data as JSON |

### Export Data Schema

The CSV and JSON exports are optimized for performance and file size by omitting raw Base64 image strings. They contain the following cross-system traceability identifiers to reliably map classifications back to their source:

- `swipelab_image_id`: Internal primary key of the image in the SwipeLab database.
- `stardbi_experiment_id`: External ID mapping to the StarDBi experiment.
- `stardbi_image_id`: External ID mapping to the StarDBi parent image.
- `stardbi_crop_id`: External ID mapping to the StarDBi bounding box/crop.
