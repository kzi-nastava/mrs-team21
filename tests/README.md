# Drumigo E2E Tests

Selenium E2E tests for the Drumigo frontend.

## Prerequisites

1. **Backend** (Spring Boot) must be running and reachable at the URL configured by the frontend (e.g. `http://localhost:8080`).
2. **Frontend** (Angular) must be running at `http://localhost:4200` (or set `E2E_FRONTEND_URL`).
3. **Database** must be seeded with `drumigo/src/main/resources/db/seed/mysql-seed.sql`. All seeded users share password **`Password12345`** (SHA-256 hashed in DB).

Without the backend and seed data, login in tests will fail with "Invalid email or password".

## Running tests

```bash
mvn test
```

Optional environment variables (or `-De2e.*` system properties):

- `E2E_FRONTEND_URL` – frontend base URL (default `http://localhost:4200`)
- `E2E_HEADLESS` – `true`/`false` (default `true`)
- `E2E_ADMIN_EMAIL` / `E2E_ADMIN_PASSWORD` – admin login (default `stefan.nikolic@drumigo.com` / `Password12345`)
- `E2E_PASSENGER_EMAIL` / `E2E_PASSENGER_PASSWORD` – passenger login (default `ana.petrovic@example.com` / `Password12345`)
- `E2E_RATING_PASSENGER_EMAIL` / `E2E_RATING_PASSENGER_PASSWORD` – rating tests user (default `rating.passenger@test.local` / `Password12345`)

ChromeDriver is resolved automatically by Selenium Manager to match the installed Chrome version.
