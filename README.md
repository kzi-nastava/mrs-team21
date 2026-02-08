# Drumigo

<p align="center">
  <img src="docs/assets/branding/logo/logo-blue-v2.svg" alt="Drumigo Logo" width="200">
</p>

### **Status:** 🚧 In Development | **Last Updated:** February 2026

> Academic ride-hailing application developed for Software Engineering coursework at the Faculty of Technical Sciences, University of Novi Sad.

## 📋 Project Overview

Drumigo is a multi-platform ride-hailing system featuring:

- **Web Application** (Angular 21) - Passenger, driver, and admin interfaces
- **Mobile Application** (Android) - Driver ride tracking and authentication
- **Backend API** (Spring Boot 4.0.1) - RESTful API with MySQL database
- **Real-time Features** - Live vehicle tracking and ride updates
- **Multi-role Support** - Passengers, drivers, and administrators

## 🏗️ Project Structure

```
/
├── docs/                    # Documentation and design assets
│   ├── specifikacija.md     # Project specification (Serbian)
│   ├── assets/              # Brand assets and designs
│   └── designs/             # Figma exports
├── frontend/                # Angular web application
├── mobile/                  # Android mobile application
└── drumigo/                 # Spring Boot backend API
```

## ✨ Implemented Features

### 🌐 Web Application (Angular 21)

#### Authentication & User Management

- ✅ User login/logout with JWT authentication
- ✅ Customer registration with email verification
- ✅ Password reset flow (forgot password & reset with token)
- ✅ Account activation via email link
- ✅ Profile management with photo upload
- ✅ Admin-initiated driver registration

#### Passenger Features

- ✅ Landing page with active vehicle map display
- ✅ Ride estimation (route, distance, price) for unregistered users
- ✅ Order ride with multiple waypoints
- ✅ Real-time ride tracking with map visualization
- ✅ Ride history with filtering and pagination
- ✅ Rate and review completed rides
- ✅ View ride details and route replay

#### Driver Features

- ✅ Driver ride history with earnings tracking
- ✅ Profile management (changes require admin approval)
- ✅ Vehicle information display

#### Admin Features

- ✅ Driver registration and account creation
- ✅ Admin ride history with global overview
- ✅ Profile change request review and approval
- ✅ User and driver management

#### Core Components

- ✅ Mapbox integration for maps and routing
- ✅ Star rating component for reviews
- ✅ Toast notification system
- ✅ Personal info forms with validation
- ✅ Responsive navbar with role-based navigation

### 📱 Mobile Application (Android)

#### Authentication

- ✅ Login functionality
- ✅ Registration flow
- ✅ Forgot password
- ✅ Reset password

#### Driver Features

- ✅ Active vehicles map display (Mapbox)
- ✅ Real-time ride tracking
- ✅ Profile viewing

#### Architecture

- ✅ Navigation Component-based architecture
- ✅ Fragment-based UI with view binding
- ✅ REST API integration with Retrofit
- ✅ Mapbox maps integration

### 🔧 Backend API (Spring Boot 4.0.1)

#### Core Services

- ✅ **Authentication & Authorization** - JWT-based security with role-based access
- ✅ **User Management** - Passengers, drivers, and admins with profile management
- ✅ **Vehicle Management** - Vehicle types, pricing, and availability tracking
- ✅ **Ride Management** - Ride creation, assignment, tracking, and completion
- ✅ **Review System** - Rating and reviewing drivers/vehicles after rides
- ✅ **Favorite Routes** - Save and reuse frequently used routes
- ✅ **Driver Profile Changes** - Change request workflow with admin approval
- ✅ **Panic System** - Emergency panic button for passengers and drivers
- ✅ **Messaging** - Support chat functionality
- ✅ **Notifications** - User notification system
- ✅ **Reports** - Ride history reports with date range filtering
- ✅ **Map Services** - Route calculation and geocoding via Mapbox

#### API Endpoints

- `/api/auth/*` - Authentication (login, register, password reset, activation)
- `/api/users/*` - User management and profiles
- `/api/rides/*` - Ride operations and tracking
- `/api/vehicles/*` - Vehicle and vehicle type management
- `/api/reviews/*` - Rating and review system
- `/api/drivers/*` - Driver-specific operations
- `/api/passengers/*` - Passenger-specific operations
- `/api/admin/*` - Admin operations (reports, approvals, management)
- `/api/favorite-routes/*` - Favorite route management
- `/api/messages/*` - Support messaging
- `/api/panic/*` - Panic event handling
- `/api/notifications/*` - User notifications
- `/api/map/*` - Route calculation and geocoding
- `/api/maintenance/*` - Database reset and seeding (dev only)

#### Database

- ✅ MySQL database with JPA/Hibernate
- ✅ Comprehensive seed data for testing
- ✅ Database maintenance endpoint for dev environment
- ✅ Entities: Users (Admin, Driver, Passenger), Rides, Vehicles, Reviews, FavoriteRoutes, Panic Events, Messages, Notifications

#### Security & Email

- ✅ Spring Security with JWT tokens
- ✅ Email service for account activation and password reset
- ✅ Role-based access control (PASSENGER, DRIVER, ADMIN)

## 🛠️ Tech Stack

### Frontend (Web)

- **Framework:** Angular 21.1.0
- **Language:** TypeScript 5.9.2
- **Maps:** Mapbox GL JS 3.18.0 + Mapbox Search JS
- **State Management:** RxJS 7.8.0
- **Styling:** SCSS
- **Build Tool:** Angular CLI with Vite
- **Package Manager:** Bun 1.3.6
- **Linting:** ESLint + Prettier

### Mobile (Android)

- **Language:** Java 11
- **Min SDK:** 30 (Android 11)
- **Target SDK:** 36
- **Maps:** Mapbox Maps SDK
- **Networking:** Retrofit + Gson
- **UI:** Navigation Component, View Binding, Material Design
- **Architecture:** Fragment-based with Navigation Component

### Backend (API)

- **Framework:** Spring Boot 4.0.1
- **Language:** Java 17
- **Database:** MySQL with JPA/Hibernate
- **Security:** Spring Security + JWT (jjwt 0.11.5)
- **Email:** Spring Mail (SMTP)
- **Maps API:** Mapbox SDK for Services
- **Build Tool:** Maven
- **Utilities:** Lombok, Spring Validation

## 🚀 Getting Started

### Prerequisites

- **Node.js** - Version compatible with Bun 1.3.6 (for frontend)
- **Bun** - 1.3.6 or higher (frontend package manager)
- **Java JDK** - Version 17 (for backend)
- **Maven** - 3.6+ (bundled with mvnw wrapper)
- **MySQL** - 8.0+ (for backend database)
- **Android Studio** - Latest version (for mobile development)
- **Mapbox Account** - For API tokens (maps functionality)

### Environment Configuration

#### Frontend

Create `frontend/.env` or set environment variables:

```bash
# Not required - configuration is in environment.ts files
```

#### Backend

Create `drumigo/src/main/resources/application-dev.properties`:

```properties
# MySQL
spring.datasource.url=jdbc:mysql://localhost:3306/drumigo?createDatabaseIfNotExist=true
spring.datasource.username=your_mysql_user
spring.datasource.password=your_mysql_password

# Mapbox
mapbox.api.key=your_mapbox_token

# Frontend URL (for CORS and email links)
app.frontend.url=http://localhost:4200

# JWT
jwt.secret=your_secret_key_min_256_bits
jwt.expiration=86400000

# Email (Gmail SMTP example)
spring.mail.username=your_email@gmail.com
spring.mail.password=your_app_password

# Maintenance endpoint (dev only)
maintenance.basic.username=admin
maintenance.basic.password=admin123
```

#### Mobile

Create `mobile/secrets.properties`:

```properties
MAPBOX_ACCESS_TOKEN=your_mapbox_token
API_BASE_URL=http://10.0.2.2:8080/api
# Note: 10.0.2.2 is the Android emulator's localhost
```

### Quick Start

#### 1. Backend Setup

```bash
cd drumigo

# Run the application (auto-compiles)
./mvnw spring-boot:run

# Or on Windows
mvnw.cmd spring-boot:run

# API runs on http://localhost:8080
```

**Initial Database Setup:**

After first run, reset and seed the database using the maintenance endpoint:

```bash
# Using curl (with Basic Auth)
curl -X POST http://localhost:8080/api/maintenance/reset \
  -u admin:admin123

# Or using Postman/similar tool
# POST http://localhost:8080/api/maintenance/reset
# Auth: Basic Auth (username: admin, password: admin123)
```

This will:

- Drop and recreate all tables
- Run the seed script from `drumigo/src/main/resources/db/seed/mysql-seed.sql`
- Create test users (password for all: `password`):
  - Passenger: `ana.petrovic@example.com`
  - Driver: `marko.jovanovic@example.com`
  - Admin: `stefan.nikolic@drumigo.com`

#### 2. Frontend Setup

```bash
cd frontend

# Install dependencies
bun install

# Start development server
bun start
# Or
npm start

# Open http://localhost:4200
```

**Available Scripts:**

- `bun start` - Dev server with hot reload
- `bun run build` - Production build
- `bun run lint` - Run ESLint
- `bun run format` - Format code with Prettier

#### 3. Mobile Setup

1. Open the `mobile/` directory in Android Studio
2. Ensure `secrets.properties` is configured (see Environment Configuration above)
3. Sync Gradle files
4. Run on an emulator or physical device (min Android 11)

**Note:** If using an emulator, make sure the backend is accessible at `http://10.0.2.2:8080`

## 🧪 Testing

### Frontend

```bash
cd frontend
bun test
# Unit tests with Vitest
```

### Backend

```bash
cd drumigo
./mvnw test
# Unit and integration tests with Spring Boot Test
```

### Mobile

- Unit tests: Run via Android Studio
- Instrumented tests: Run on device/emulator via Android Studio

**Note:** Test coverage varies by feature. E2E tests are not configured by default.

## 📁 Database Seed Data

The backend includes comprehensive seed data for testing:

- **3 Vehicle Types**: Standard, Luxury, Van (with pricing)
- **6 Test Users**: 2 passengers, 3 drivers, 1 admin
- **5 Vehicles**: Various vehicle configurations
- **Sample Rides**: Pre-configured ride history for testing

All test accounts use password: `password`

See `drumigo/src/main/resources/db/seed/mysql-seed.sql` for details.

## 🔧 Development Scripts (H2 Legacy)

The `drumigo/h2_scripts/` directory contains SQL scripts for various testing scenarios (originally for H2, reference only):

- `active_vehicles_display/` - Active vehicle map testing
- `driver_ride_history/` - Driver ride history scenarios
- `profile_test_data/` - Profile data for testing
- `ride_completion/` - Ride completion flows
- `ride_rating/` - Rating system testing
- `ride_tracking_inconsistency/` - Edge case testing
- `sample_rides/` - Sample ride data

These are for reference only; use the MySQL seed script for actual development.

## 📚 Documentation

- [docs/specifikacija.md](docs/specifikacija.md) - Complete project specification (Serbian)
- [docs/assets/branding/](docs/assets/branding/) - Logo and brand assets
- [docs/designs/](docs/designs/) - Figma design exports

### API Documentation

The backend exposes a RESTful API at `http://localhost:8080/api`. Key endpoints:

- Authentication: `/api/auth/*`
- Users & Profiles: `/api/users/*`, `/api/profile/*`
- Rides: `/api/rides/*`
- Reviews: `/api/reviews/*`
- Admin Operations: `/api/admin/*`

For detailed API documentation, explore the controller classes in `drumigo/src/main/java/com/ftn/drumigo/controller/`.

## 🚧 Known Limitations & Future Work

### Not Yet Implemented

- Real-time WebSocket connections (ride updates are polling-based or refresh-based)
- Multi-passenger ride splitting
- Scheduled rides (future booking)
- Driver availability toggle in UI
- Driver work hours tracking (8-hour limit)
- Automatic driver assignment algorithm
- Payment processing integration
- Push notifications (mobile)
- In-app messaging (chat UI exists but needs real-time support)

### In Progress

- Admin dashboard analytics
- Driver earnings reports
- Advanced ride filtering
- Mobile passenger features

## 🎨 Design & Branding

The project uses custom branding with the Drumigo logo. Brand assets are located in `docs/assets/branding/`.

Design mockups and Figma exports are available in `docs/designs/`.

## 🤝 Contributing

This is an academic project for coursework. External contributions are not accepted, but feedback is welcome.

### Development Guidelines

- Follow existing code structure and patterns
- Use ESLint/Prettier for frontend code formatting
- Follow Spring Boot best practices for backend
- Write meaningful commit messages
- Test changes before committing

## 📝 License

This project is developed for academic purposes at the Faculty of Technical Sciences, University of Novi Sad.

## 🔗 Links

- **Repository:** TBD
- **Figma Design:** [docs/designs/driver-history-figma.html](docs/designs/driver-history-figma.html)
- **Project Specification:** [docs/specifikacija.md](docs/specifikacija.md)

## 👥 Team

### Contributors

- **Vukan Radojević** - Development
- **Marko Sladojević** - Development
- **Miloš Jovanović** - Development

### Course

Software Engineering coursework (2025/2026):

- Server-side Engineering
- Client-side Engineering
- Software Testing
- Mobile Applications
- Software Development Methodologies

**Faculty of Technical Sciences, University of Novi Sad**

---

**Last Updated:** February 7, 2026
