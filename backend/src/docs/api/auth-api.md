# Auth API Endpoints

> **Note:** All login and token refresh endpoints now return `accessToken` and `refreshToken` both in the JSON payload and as `HttpOnly` cookies. Web clients should rely on cookies (by setting `credentials: 'include'`) while mobile clients should store tokens from the JSON response securely.

Base path: `/api/v1/auth`

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/register` | Register a new user |
| `POST` | `/email/verify` | Verify email token |
| `GET` | `/verify-email` | HTML endpoint for email verification |
| `POST` | `/email/resend` | Resend verification email |
| `GET` | `/user` | Get current user details |
| `GET` | `/test` | Test auth endpoint |
| `POST` | `/login` | Login with username and password |
| `POST` | `/refresh` | Refresh JWT access token |
| `POST` | `/logout` | Logout |
| `GET` | `/me` | Get current user |
| `POST` | `/login/google` | Login via Google OAuth |
| `POST` | `/password/forgot` | Request password reset |
| `POST` | `/password/reset` | Reset password |
| `POST` | `/invitation/admin` | Invite a new admin user |

## External Auth
Base path: `/api/v1/auth/external`

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/stardbi/loginExternal` | Login via external Stardbi token |
