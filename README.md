# Drumigo

<p align="center">
  <img src="docs/assets/branding/logo/logo-blue-v2.svg" alt="Drumigo Logo" width="200">
</p>

### **Status:** 🚧 In Development | **Last Updated:** December 2025

> Academic ride-hailing application developed for Software Engineering coursework at the Faculty of Technical Sciences, University of Novi Sad.

## 📋 Project Overview

Drumigo is a multi-platform ride-hailing system featuring:

- Web application (Angular)
- Mobile application (Android)
- Backend API (Spring Boot)
- Real-time tracking and notifications
- Multi-role support (passengers, drivers, administrators)

## 🏗️ Project Structure

```
/
├── docs/               # Documentation and design assets
├── frontend/           # Angular web application
├── mobile/             # Android mobile application
├── backend/            # Spring Boot API
└── tests/              # Automated tests (where applicable)
```

## 🛠️ Tech Stack (high level)

- Frontend: Angular, TypeScript, RxJS
- Mobile: Android (Java/Kotlin), MVVM
- Backend: Spring Boot, Java, MySQL
- Real-time: WebSocket
- Maps: Mapbox GL JS

(Exact versions and tools are kept in each subproject's `package.json`, `pom.xml`, or project config.)

## 🚀 Getting Started (cleaned)

### Prerequisites

- Node.js and npm (see `frontend/package.json` for recommended versions)
- Java (see `backend` build files for required JDK)
- Android Studio (for mobile development)
- PostgreSQL (if running the backend with a local DB)

### Quick Start

#### Frontend

```bash
cd frontend
npm install
npm start
# Open http://localhost:4200
```

Use the `npm` scripts so the local CLI version is used (`npm start`, `npm run build`, `npm test`).

#### Backend

```bash
cd drumigo
./mvnw spring-boot:run
# API typically runs on http://localhost:8080
```

#### Backend (MySQL setup + maintenance reset)

- Configure MySQL in `drumigo/src/main/resources/application-dev.properties` (or via env vars `MYSQL_URL`, `MYSQL_USER`, `MYSQL_PASSWORD`).
- Ensure the maintenance basic auth credentials are set (`maintenance.basic.username` / `maintenance.basic.password`).

Maintenance endpoint (drops/recreates schema from JPA + seeds data):

- Method: `POST`
- URL: `http://localhost:8080/api/maintenance/reset`
- Auth: Basic (use the maintenance credentials)
- Seed script: `drumigo/src/main/resources/db/seed/mysql-seed.sql`

#### Mobile

Open the `mobile/` project in Android Studio and run on an emulator or device.

## 📖 Documentation

- `docs/api/` — API reference (if present)
- `docs/assets/branding/` — Brand assets and usage
- `docs/sprint-planning/` — Sprint plans and retrospectives

If a document or link is missing, it is marked in the docs folder or will be added as the project progresses.

## 🧪 Testing

- Unit and integration tests are configured per-subproject (see `frontend`, `backend`, and `mobile` folders). E2E tests may not be configured by default — add explicit e2e projects/scripts if needed.

## 🔗 Links

Replace the placeholders below with actual links when available.

- Trello Board: TBD
- Figma Design: TBD
- API Docs: TBD
- Repository: TBD

## 👥 Team

- Vukan Radojevic
- Marko Sladojevic
- Milos Jovanovic
