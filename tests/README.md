# Drumigo Selenium E2E Tests

This module contains Java Selenium + JUnit 5 E2E tests for:
- **2.9.3 Filtering and sorting ride history**

## Preconditions

1. Backend is running at `http://localhost:8080`.
2. Frontend is running at `http://localhost:4200`.
3. Seed data is loaded (recommended via maintenance reset):
   - `POST /api/maintenance/reset` with Basic Auth `admin:admin123`
4. Admin login credentials are valid.

Default credentials used by tests:
- email: `stefan.nikolic@drumigo.com`
- password: `Sifra123`

If your local credentials differ, override them with env vars or JVM properties.

## Run

```bash
mvn -f tests/pom.xml test
```

## Configuration

### Env vars
- `E2E_FRONTEND_URL` (default `http://localhost:4200`)
- `E2E_ADMIN_EMAIL` (default `stefan.nikolic@drumigo.com`)
- `E2E_ADMIN_PASSWORD` (default `Sifra123`)
- `E2E_HEADLESS` (default `true`)

### JVM properties
- `-De2e.frontend.url=...`
- `-De2e.admin.email=...`
- `-De2e.admin.password=...`
- `-De2e.headless=true|false`

Example:

```bash
mvn -f tests/pom.xml test -De2e.headless=false -De2e.admin.password=YourPassword
```
