# Drumigo

<p align="center">
  <img src="docs/assets/branding/logo/logo-blue-v2.svg" alt="Drumigo Logo" width="200">
</p>

Academic ride-hailing application developed for Software Engineering coursework at the Faculty of Technical Sciences, University of Novi Sad.

---

## Purpose

Drumigo is a multi-platform ride-hailing system that enables:

- **Passengers** to order rides, track drivers in real time, view history, rate drivers, and save favorite routes
- **Drivers** to accept or reject ride requests, manage profile (with admin approval for changes), and track earnings
- **Administrators** to register drivers, approve profile changes, manage users, view reports, and handle panic events

The system consists of a REST API backend, an Angular web application (passenger, driver, and admin interfaces), and an Android mobile app (driver-focused). Maps and routing use Mapbox across all components.

---

## Architecture Overview

### High-level structure

```
mrs-team21/
├── drumigo/     # Spring Boot backend (REST API, MySQL, JWT)
├── frontend/    # Angular web application
├── mobile/      # Android application
└── docs/        # Specification and design assets
```

### Components

| Component | Stack | Role |
|-----------|--------|------|
| **Backend** | Spring Boot 4.0.1, Java 17, Maven, MySQL, JPA/Hibernate, Spring Security, JWT | REST API, business logic, auth, email, Mapbox server-side (routing/geocoding) |
| **Frontend** | Angular 21, TypeScript, SCSS, Bun, Mapbox GL JS | Web UI for passengers, drivers, and admins |
| **Mobile** | Android (Java 11), Retrofit, Mapbox Maps SDK, Material Design | Driver app: login, ride tracking, profile, maps |

### Domain model (summary)

- **Users:** Abstract `User` with subclasses `Admin`, `Driver`, `Passenger`; roles `PASSENGER`, `DRIVER`, `ADMIN`
- **Rides:** Lifecycle `PENDING` → `ACCEPTED` → `STARTED` → `FINISHED` or `CANCELED`
- **Vehicles:** Linked to drivers; `VehicleType` (STANDARD, LUXURY, VAN) with pricing
- **Other:** `Review`, `PanicEvent`, `Message`, `Notification`, `FavoriteRoute`, `DriverProfileChangeRequest`

### Backend layout

```
drumigo/src/main/java/com/ftn/drumigo/
├── controller/   # REST controllers
├── domain/       # JPA entities
├── repository/   # Spring Data JPA repositories
├── service/      # Business logic
├── dto/          # Request/response DTOs
├── mapper/       # Entity–DTO mappers
├── config/       # Security, CORS, web config
├── security/     # JWT filter, user details
└── util/         # JWT, password utilities
```

### API surface

| Prefix | Purpose |
|--------|---------|
| `/api/auth` | Login, register, password reset, activation |
| `/api/users` | User management |
| `/api/profile` | Profile and profile picture |
| `/api/rides` | Rides, estimation, tracking, reviews, map/estimate |
| `/api/vehicles` | Vehicles and active vehicles |
| `/api/vehicle-types` | Vehicle types and pricing |
| `/api/drivers` | Driver operations, profile change requests |
| `/api/passengers` | Passenger operations, favorite routes |
| `/api/admin` | Admin operations, reports, approvals |
| `/api/support` | Support messaging |
| `/api/panic-events` | Panic events |
| `/api/users/{id}/notifications` | Notifications |
| `/api/activation` | Account activation |
| `/api/maintenance` | Database reset/seed (dev, Basic Auth) |

Profile pictures are stored by URL in the database; files live under `uploads/profile/`. Registration: `POST /api/auth/profile-picture` (no auth), then send returned URL as `profilePictureUrl` in registration payload. Profile update: `POST /api/profile/picture`, then `PUT /api/profile` with the returned URL.

---

## Setup Instructions

### Prerequisites

- **Node.js** – compatible with Bun (for frontend)
- **Bun** – 1.3.6 or higher (frontend package manager)
- **Java JDK 17** – backend
- **Maven** – 3.6+ (use project `mvnw` wrapper)
- **MySQL** – 8.0+
- **Android Studio** – for mobile (latest stable)
- **ngrok** – to expose backend to mobile (emulator and device)
- **Mapbox account** – for API tokens (all three components)

### 1. Backend

1. Create `drumigo/src/main/resources/application-dev.properties` (this file is gitignored). Example:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/drumigo?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
spring.datasource.username=root
spring.datasource.password=YOUR_MYSQL_PASSWORD

mapbox.api.key=YOUR_MAPBOX_TOKEN
app.frontend.url=http://localhost:4200

jwt.secret=YOUR_SECRET_MIN_32_CHARS
jwt.expiration=86400000

spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=YOUR_EMAIL
spring.mail.password=YOUR_APP_PASSWORD

maintenance.basic.username=admin
maintenance.basic.password=admin123
```

Set your MySQL credentials, Mapbox token, JWT secret, and mail settings. Optional: `app.driver-activation.link` for driver activation links (e.g. ngrok URL or `drumigo://activate-driver`).

2. Start MySQL and ensure a database can be created (e.g. `drumigo`).

3. Run the backend:

```bash
cd drumigo
./mvnw spring-boot:run
# Windows: mvnw.cmd spring-boot:run
```

API base: `http://localhost:8080`. Default profile is `dev`, so `application-dev.properties` is loaded.

4. Seed the database (first run or reset):

```bash
curl -X POST http://localhost:8080/api/maintenance/reset -u admin:admin123
```

Use the same Basic Auth credentials as in `maintenance.basic.username` and `maintenance.basic.password`. Seed script: `drumigo/src/main/resources/db/seed/mysql-seed.sql`.

**Test accounts (password: `password`):**

- Passenger: `ana.petrovic@example.com`
- Driver: `marko.jovanovic@example.com`
- Admin: `stefan.nikolic@drumigo.com`

### 2. Frontend

1. Install dependencies and run:

```bash
cd frontend
bun install
bun start
```

2. Open `http://localhost:4200`. Development build uses `environment.dev.ts` (API base `http://localhost:8080/api` and Mapbox token).

3. If you do not have `environment.dev.ts`, copy from `environment.ts` and set `mapboxToken` and `apiBaseUrl`.

### 3. Mobile

1. Create `mobile/secrets.properties` with at least:
   - `MAPBOX_ACCESS_TOKEN` (or `MAPBOX_PUBLIC_TOKEN`) for maps
   - `MAPBOX_DOWNLOADS_TOKEN` or `MAPBOX_SECRET_KEY` for Gradle (Mapbox SDK)
   - `API_BASE_URL` – backend API base (e.g. ngrok URL + `/api`)

2. Expose backend for the device/emulator (e.g. ngrok):

```bash
ngrok http 8080
```

Set `API_BASE_URL=https://YOUR_NGROK_HTTPS_URL/api` in `secrets.properties` (no trailing slash).

3. Open `mobile/` in Android Studio, sync Gradle, run on emulator or device (`./gradlew installDebug` from `mobile/` if preferred).

**Troubleshooting:** If the device is "UNAUTHORIZED," allow USB debugging when prompted. For "No online devices," ensure the device/emulator is connected and recognized.

---

## Configuration Options

### Backend (`application.properties` + `application-dev.properties`)

| Property | Description |
|----------|-------------|
| `spring.datasource.url` | JDBC URL (e.g. `jdbc:mysql://localhost:3306/drumigo?createDatabaseIfNotExist=true`) |
| `spring.datasource.username` / `password` | MySQL credentials |
| `mapbox.api.key` | Mapbox token for server-side routing/geocoding |
| `app.frontend.url` | Frontend origin (CORS, email links) |
| `app.driver-activation.link` | Base URL for driver activation link (e.g. ngrok or `drumigo://activate-driver`) |
| `jwt.secret` | Secret for JWT (min 256 bits for HS256) |
| `jwt.expiration` | Token validity in milliseconds |
| `spring.mail.*` | SMTP (e.g. Gmail) for activation and password reset |
| `maintenance.basic.username` / `password` | Basic Auth for `/api/maintenance/reset` |
| `app.maintenance.seed-script` | Path to seed SQL (default: `classpath:db/seed/mysql-seed.sql`) |
| `app.uploads.profile-dir` | Profile picture upload directory (default: `uploads/profile`) |
| `server.port` | HTTP port (default: 8080) |
| `server.address` | Bind address (default: 0.0.0.0) |

Secrets and local overrides belong in `application-dev.properties` (gitignored).

### Frontend

- **environment.ts** – default/placeholder; `mapboxToken`, `apiBaseUrl`, optional `useMockVehicles`, `driverLocationPingIntervalMs`
- **environment.dev.ts** – used by `ng serve`; set real `mapboxToken` and `apiBaseUrl` (e.g. `http://localhost:8080/api`)

Production build can use a different environment (e.g. `environment.prod.ts`) via Angular configuration.

### Mobile

- **secrets.properties** (project root, gitignored):
  - `MAPBOX_ACCESS_TOKEN` or `MAPBOX_PUBLIC_TOKEN` or `MAPBOX_SECRET_KEY` – map display
  - `MAPBOX_DOWNLOADS_TOKEN` or `MAPBOX_SECRET_KEY` – Gradle Mapbox SDK resolution
  - `API_BASE_URL` – backend API base (e.g. ngrok HTTPS URL + `/api`)

Without `MAPBOX_DOWNLOADS_TOKEN`/`MAPBOX_SECRET_KEY`, Gradle may fail resolving Mapbox. For emulator-only you can use `http://10.0.2.2:8080/api`; for device on same network use the host machine’s LAN IP.

---

## Common Development Tasks

### Backend

- **Run:** `./mvnw spring-boot:run` (from `drumigo/`)
- **Tests:** `./mvnw test`
- **Reset DB and reseed:** `POST http://localhost:8080/api/maintenance/reset` with Basic Auth

### Frontend

- **Run:** `bun start` (from `frontend/`)
- **Build:** `bun run build`
- **Lint:** `bun run lint`; fix: `bun run lint:fix`
- **Format:** `bun run format`; check: `bun run format:check`
- **Tests:** `bun test` (e.g. Vitest/Karma per project config)

### Mobile

- **Build/install debug:** `./gradlew installDebug` (from `mobile/`)
- **Unit/instrumented tests:** Run via Android Studio

### Cross-cutting

- **Price formula:** `vehicle_type_price + distance_km * 120` (backend)
- **Conventions:** Code, commits, and comments in English; frontend uses Bun; backend uses Maven wrapper
- **Mapbox:** GL JS (frontend), Maps SDK (mobile), Mapbox SDK for Services (backend)

---

## Documentation and References

- **Specification:** [docs/specifikacija.md](docs/specifikacija.md) (Serbian)
- **Branding:** [docs/assets/branding/](docs/assets/branding/)
- **Designs:** [docs/designs/](docs/designs/)

For detailed API behavior, see the controller classes under `drumigo/src/main/java/com/ftn/drumigo/controller/`.


---

## Team and License

Contributors: Vukan Radojević, Marko Sladojević, Miloš Jovanović.

Developed for academic purposes at the Faculty of Technical Sciences, University of Novi Sad (Software Engineering coursework 2025/2026). External contributions are not accepted; feedback is welcome.
