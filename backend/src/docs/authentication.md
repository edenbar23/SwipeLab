# SwipeLab Authentication Architecture

This document details the various authentication flows supported by the SwipeLab backend, covering native users, external Stardbi researchers, and Google OAuth users.

## Overview

SwipeLab issues its own internal **JWT (JSON Web Tokens)** for securing API endpoints. Regardless of which authentication method a user employs to log in, the final output of a successful authentication is always a SwipeLab Access Token and a Refresh Token.

The system utilizes `AuthenticationService` for local credentials and delegates to `StardbiAuthService` and `OAuth2Service` for external providers.

---

## 1. Native SwipeLab Users (Local Authentication)

The primary flow for standard users logging in directly via SwipeLab credentials.

### Registration Flow
1. Users submit a registration request containing their email, username, and password.
2. The `AuthenticationService` creates a local `User` entity with `AuthProvider.LOCAL` and standard role `USER`.
3. A unique email verification token is generated, and a verification email is dispatched (handled by `EmailService`).
4. *Dev Environment Exception:* If `app.auto-verify-emails` is enabled, the account is immediately verified and tokens are returned instantly.

### Login Flow
1. The user provides their credentials to the `/auth/login` endpoint.
2. The `AuthenticationService` validates the password hash, verifies that the account is active, not locked, and that the email has been verified.
3. Upon success, internal JWT Access and Refresh tokens are generated using the `JwtService` and returned to the client.

---

## 2. Stardbi Researchers (External Authentication)

SwipeLab integrates with Stardbi to allow existing external researchers to authenticate seamlessly and act on SwipeLab tasks.

### Flow
1. The client sends an `ExternalLoginRequest` to the backend containing a Stardbi access token.
2. The `StardbiAuthService` intercepts this and validates the token directly against the Stardbi platform via `StardbiAuthProvider`.
3. **Auto-Provisioning**:
   - If the Stardbi token is valid, the service checks if a local SwipeLab user exists for that Stardbi username.
   - If no user exists, a new SwipeLab `User` is provisioned automatically with `AuthProvider.STARDBI`.
   - **Role Assignment**: Stardbi users are strictly assigned the **`RESEARCHER`** role.
4. **Token Caching (BFF Proxying)**:
   - The Stardbi access and refresh tokens are securely cached in memory (`CACHE_STARDBI_TOKENS`).
   - This allows SwipeLab to execute actions on behalf of the researcher against the Stardbi API later.
   - The backend handles automatic token refreshing against Stardbi if an API call yields a 401 Unauthorized.
5. Finally, native SwipeLab JWT tokens are issued to the client for subsequent SwipeLab API access.

---

## 3. Google OAuth

Google Authentication allows for frictionless onboarding for new standard users and can be linked to existing accounts.

### Flow
1. The client performs Google authentication natively (e.g., via Expo AuthSession on mobile or standard web flows).
2. The client sends either a Google **ID Token (JWT)** or a Google **Access Token** to the SwipeLab backend.
3. The `OAuth2Service` validates the token:
   - *ID Tokens* are verified cryptographically via `GoogleIdTokenVerifier`.
   - *Access Tokens* are verified by calling Google's `userinfo` endpoint.
4. **User Resolution**:
   - The service checks if a user already exists with the provided Google email.
   - **Existing User**: The account's profile image and display name are updated if necessary. If it was a `LOCAL` account, the `providerId` may be linked.
   - **New User**: A new `User` is created with `AuthProvider.GOOGLE`.
5. **Role Assignment**:
   - New Google OAuth users default to the **`USER`** role.
   - *Exception*: If the Google email matches the system's defined SuperAdmin, they are granted the **`RESEARCHER`** role immediately.
6. Native SwipeLab JWT tokens are generated and returned to the client.
