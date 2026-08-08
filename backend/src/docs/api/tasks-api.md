# Tasks API Endpoints

Base path: `/api/v1/tasks`

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/my-tasks` | Get tasks assigned to the current user |
| `GET` | `/available-tasks` | Get public tasks available to explore |
| `GET` | `/my-tasks/{taskId}` | Get details of a specific assigned task |
| `POST` | `/{taskId}/assign` | Self-assign an available public task |
| `GET` | `/dashboard` | Get researcher task dashboard |
| `GET` | `/dashboard/{taskId}` | Get researcher task details |
| `GET` | `/dashboard/experiments` | List external experiments (Stardbi) |
| `POST` | `/create` | Create a new task |
| `POST` | `/{taskId}/archive` | Archive a task |
| `PUT` | `/{taskId}` | Update a task |
| `POST` | `/{taskId}/activate` | Activate a task |
| `POST` | `/{taskId}/pause` | Pause a task |

## Metadata
Base path: `/api/v1/metadata`

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/species` | Get all target species metadata |
