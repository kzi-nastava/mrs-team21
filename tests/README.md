# Drumigo Selenium E2E Tests

This module contains Java Selenium + JUnit 5 E2E tests for:
- **2.9.3 Filtering and sorting ride history**
- **2.4.3 Order from favorite routes** (select favorite on order page → pre-fill; add/remove favorite in passenger history)

## Preconditions

1. Backend is running at `http://localhost:8080`.
2. Frontend is running at `http://localhost:4200`.
3. Seed data is loaded (recommended via maintenance reset):
   - `POST /api/maintenance/reset` with Basic Auth `admin:admin123`
4. For admin tests: admin login credentials are valid.
5. For 2.4.3 favorite-route tests: passenger must have at least one completed ride in history (seed data).

Default credentials used by tests (must match seed data; seed uses `Password12345` for all users):
- **Admin**: email `stefan.nikolic@drumigo.com`, password `Password12345`
- **Passenger** (for 2.4.3): email `ana.petrovic@example.com`, password `Password12345`

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
- `E2E_PASSENGER_EMAIL` (default `ana.petrovic@example.com`) – for 2.4.3 favorite-route tests
- `E2E_PASSENGER_PASSWORD` (default `password`)
- `E2E_HEADLESS` (default `true`)

### JVM properties
- `-De2e.frontend.url=...`
- `-De2e.admin.email=...`
- `-De2e.admin.password=...`
- `-De2e.passenger.email=...`
- `-De2e.passenger.password=...`
- `-De2e.headless=true|false`

Example:

```bash
mvn -f tests/pom.xml test -De2e.headless=false -De2e.admin.password=YourPassword
```
