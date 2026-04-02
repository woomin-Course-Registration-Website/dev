# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Teacher-Student Management System (교사용 학생 성적 및 상담 관리 시스템) — a full-stack web application for managing student grades, records, feedback, and counseling sessions.

**Current status:** Sprint 0 complete. Architecture, design, and scaffolding are done. Most controllers/services/repositories/entities and frontend API modules are not yet implemented.

## Build & Run Commands

### Backend (Spring Boot, runs on :8080)
```bash
./gradlew bootRun          # Run application
./gradlew build            # Build JAR
./gradlew test             # Run tests
./gradlew test --tests "com.studentmanagement.SomeTest"  # Run single test
./gradlew clean            # Clean build artifacts
```

### Frontend (React/Vite, runs on :3000)
```bash
cd frontend
npm install                # Install dependencies
npm run dev                # Start dev server (proxies /api to :8080)
npm run build              # Build for production
```

### Full local dev
Start backend (`./gradlew bootRun`) and frontend (`cd frontend && npm run dev`) in separate terminals. Frontend dev server proxies `/api/**` to `http://localhost:8080`.

## Architecture

```
React 18 (Vite) :3000
      ↓ REST API (/api/**)
Spring Boot 3.3 :8080
      ↓ JPA/Hibernate
MySQL 8 :3306  (DB: student_management)
```

### Backend (`src/main/java/com/studentmanagement/`)
- `config/` — `SecurityConfig` (JWT filter chain), `JwtConfig` (@ConfigurationProperties), `CorsConfig`
- `exception/` — `GlobalExceptionHandler`, `ResourceNotFoundException`, `UnauthorizedException`
- `util/JwtUtil` — JWT generation and validation

Domain layers to be implemented follow: `domain/` → `repository/` → `service/` → `controller/` per the package structure in [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

Roles: `TEACHER`, `STUDENT`, `PARENT`, `ADMIN`. Auth uses JWT access tokens (15 min) + refresh tokens (7 days) via Spring Security stateless filter.

### Frontend (`frontend/src/`)
- `App.jsx` — React Router v6 with `PrivateRoute` guard
- `store/authStore.js` — Zustand global auth state
- `utils/tokenUtils.js` — JWT token handling
- `pages/` — Login, Dashboard, StudentList, StudentDetail, GradeManagement, FeedbackManagement, CounselingManagement, Reports (all scaffolded, most empty)
- `api/` — Empty; axios client and API modules to be implemented here
- `components/common/` — Header, Sidebar, Layout

### Key Libraries
| Layer | Libraries |
|-------|-----------|
| Backend | Java 21, Spring Boot 3.3.0, jjwt 0.12.6, Spring Data JPA |
| Frontend | React 18.3.1, React Router 6.26.0, Zustand 4.5.4, Recharts 2.12.7, Tailwind CSS 3.4.7 |

## Configuration

`src/main/resources/application.yml` — database URL, JWT secrets, Gmail SMTP settings, CORS origin. Uses Spring profiles: `dev` (ddl-auto: create-drop, show-sql: true) and `prod` (ddl-auto: validate).

## Detailed Documentation

All design decisions are captured in `docs/`:
- [docs/API.md](docs/API.md) — Full REST API spec (9 domains: Auth, Users, Students, Grades, Records, Feedback, Counseling, Notifications, Subjects)
- [docs/ERD.md](docs/ERD.md) — Database schema / entity relationships
- [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) — Layer diagrams, package structure
- [docs/DESIGN.md](docs/DESIGN.md) — UI design system (Pretendard font, color palette, breakpoints)
- [docs/WIREFRAME.md](docs/WIREFRAME.md) — Page-level wireframes
- [BACKLOG.md](BACKLOG.md) — Product backlog with story points (6 sprints, 139 SP total)

## Git Conventions

Branch naming: `feature/EP-XX-short-description` off `develop`, merge to `develop`, then to `main` for releases.

Commit prefix conventions: `[feat]`, `[fix]`, `[docs]`, `[refactor]`, `[test]`, `[chore]`
