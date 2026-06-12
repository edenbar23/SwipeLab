# SwipeLab E2E Environment Guide

The End-to-End (E2E) environment in SwipeLab is a fully self-contained, deterministic environment designed for testing features, UI flows, and end-to-end workflows without modifying production data or depending on external services.

## 🚀 How to Run the E2E Profile

To start the backend in E2E mode, you need to activate the `e2e` Spring profile.

Run the following command from the `backend/` directory:
```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=e2e
```

When this profile is active:
- It connects to a separate PostgreSQL database (`swipelab_e2e`).
- It disables external integrations like the scheduled StarDBI synchronization.
- The `MockStardbiClient` is used to simulate responses locally.
- The database is automatically seeded with test data via the `E2eDataSeeder`.

## 👥 Seeded Users

The following users are automatically seeded and ready to use in the environment:

| Username         | Password             | Role       | Description                           |
|------------------|----------------------|------------|---------------------------------------|
| `admin_e2e`      | `superpassword123`   | RESEARCHER | Admin user for task creation/viewing. |
| `e2e_user`       | `password`           | USER       | Standard test user.                   |
| `reviewer_e2e`   | `password`           | USER       | Additional test user for reviews.     |

## 📋 Available Tasks

When you log in, you will find predefined tasks and datasets populated from local resources:

1. **E2E Identification Task** (`e2e_identification_task`)
   - **Visibility**: Private
   - **Target**: Mammals (specifically Cat/Dog)
   - **Access**: Assigned to the "E2E Testers" group (which includes `e2e_user` and `reviewer_e2e`).
   
2. **Explore E2E Task** (`explore_e2e_task`)
   - **Visibility**: Public
   - **Target**: Birds
   - **Access**: Open to all users (used for testing public task cache & assignment flow).

### 🖼️ Imagery and Data
- **Images**: Automatically loaded from `backend/src/main/resources/e2e-crops`. The seeder dynamically parses the file names (e.g., `experimentId_parentId_boxId.jpg`) to extract metadata constraints. If the folder is empty, fallback base64 images are seeded.
- **Labels**: Seeded with basic taxonomies (e.g., `CAT`, `DOG`).
- **Gamification**: Seeded with badges (First Swipe, Silver Badge, LabSwiper Legend), daily/lifetime challenges, and pre-existing user streaks and scores.
- **Analytics & Gold Images**: Pre-populated records exist for `e2e_user` to visualize dashboard metrics and simulate credibility scoring.

## ⚙️ Technical Details & Ports

The E2E profile isolates its network interactions and persists state securely.

* **Backend API (Spring Boot)**: Runs on `http://localhost:8080`.
* **Frontend (Expo/React Native)**: Typically runs on `http://localhost:8081` (Standard Expo port).
* **Database (PostgreSQL)**: Connects to `jdbc:postgresql://localhost:5432/swipelab_e2e`. Make sure your local Postgres instance has the `swipelab_e2e` database created.
* **SMTP (Mail)**: Configured to use a local SMTP stub (like MailHog) on port `1025`. This ensures emails aren't accidentally sent out during testing.
* **Mock API Endpoints**: The `MockStardbiController` simulates the external StarDBI API on `/stardbi/**` endpoints, enabling local full-stack integration without network dependencies.
