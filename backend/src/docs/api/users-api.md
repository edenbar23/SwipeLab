# Users API Endpoints

Base path: `/api/v1/users`

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/me` | Get current user profile |
| `GET` | `/{username}` | Get profile for specific username |
| `PUT` | `/me` | Update current user profile |
| `GET` | `/get-all` | List all users |
| `GET` | `/roles/{role}` | List users by role |
| `POST` | `/ban/{username}` | Ban a user (SuperAdmin only) |
| `POST` | `/unban/{username}` | Unban a user (SuperAdmin only) |

## Admin Notifications
Base path: `/api/admin/notifications`

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/` | List admin notifications |
| `GET` | `/unread-count` | Get count of unread notifications |
| `PATCH` | `/{id}/read` | Mark specific notification as read |
| `PATCH` | `/read-all` | Mark all notifications as read |
