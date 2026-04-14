# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Teacher-Student Management System (교사용 학생 성적 및 상담 관리 시스템) — a full-stack web application for managing student grades, records, feedback, and counseling sessions.

**Current status:** 풀스택 구현 완료. 백엔드 API, 프론트엔드 API 연동, Docker 설정, 보고서 기능(Excel/PDF) 모두 완성.

## Build & Run Commands

### Docker (권장)
```bash
docker compose up --build    # 전체 스택 빌드 & 실행
docker compose up            # 빌드 없이 실행 (이미지 있을 때)
docker compose down          # 종료
```
접속: `http://localhost` (프론트엔드), `http://localhost:8080/swagger-ui/index.html` (Swagger)

### Backend (Spring Boot, runs on :8080)
```bash
./gradlew bootRun          # Run application
./gradlew build            # Build JAR
./gradlew test             # Run tests
./gradlew clean            # Clean build artifacts
```

### Frontend (React/Vite, runs on :3000)
```bash
cd frontend
npm install                # Install dependencies (lock 파일 동기화용)
npm run dev                # Start dev server (proxies /api to :8080)
npm run build              # Build for production
```

## Architecture

```
React 18 (Vite) :3000 / nginx :80
      ↓ REST API (/api/**)
Spring Boot 3.3 :8080
      ↓ JPA/Hibernate
MySQL 8 :3306  (DB: student_management)
```

### Backend (`src/main/java/com/studentmanagement/`)
- `config/` — `SecurityConfig`, `JwtConfig`, `CorsConfig` (inner @ConfigurationProperties), `SwaggerConfig`
- `domain/` — User, Student, Subject, Grade, StudentRecord, Feedback, Counseling, Notification
- `repository/` — 전체 구현 (커스텀 JPQL 포함)
- `service/` — Auth, Student, Grade, StudentRecord, Feedback, Counseling, Notification, Subject, Report
- `controller/` — 전체 구현 (Auth, User, Student, Grade, StudentRecord, Feedback, Counseling, Notification, Subject, Report)
- `dto/` — ApiResponse<T>, 도메인별 Request/Response, report/ReportPreviewResponse
- `exception/` — GlobalExceptionHandler, ResourceNotFoundException, UnauthorizedException
- `util/JwtUtil` — JWT 생성/검증

Roles: `TEACHER`, `STUDENT`, `PARENT`, `ADMIN`
Auth: JWT accessToken 15분 + refreshToken 7일, stateless 필터

### Frontend (`frontend/src/`)
- `App.jsx` — React Router v6, PrivateRoute, `/register` 라우트 포함
- `store/authStore.js` — Zustand + localStorage 영속화 (login/logout/setAccessToken)
- `api/client.js` — axios baseURL `/api`, 401 자동 refresh + 큐잉
- `api/` — auth, students, grades, records, feedbacks, counselings, notifications, subjects, reports
- `pages/` — Login, Register, Dashboard, StudentList, StudentDetail, GradeManagement, FeedbackManagement, CounselingManagement, Reports (전부 API 연동 완료)
- `components/common/` — Header (실시간 알림), Sidebar, Layout

### Key Libraries
| Layer | Libraries |
|-------|-----------|
| Backend | Java 21, Spring Boot 3.3.0, jjwt 0.12.6, Spring Data JPA, poi-ooxml 5.2.5, openpdf 1.3.30, springdoc-openapi 2.5.0 |
| Frontend | React 18.3.1, React Router 6.26.0, Zustand 4.5.4, Axios 1.7.2, Recharts 2.12.7, Tailwind CSS 3.4.7 |

## Important Conventions

**YAML List 주입**: `@Value`로 List 타입 주입 금지. `CorsConfig`처럼 inner static `@ConfigurationProperties` 클래스 사용.

**Enum 매핑**:
- Feedback category: `GRADE | BEHAVIOR | ATTENDANCE | ATTITUDE | OTHER`
- Counseling shareScope: `ALL (전체공개) | PRIVATE (비공개)`

**Frontend Dockerfile**: `npm ci` 아닌 `npm install` 사용 (lock 파일 동기화 문제).

**새 npm 패키지**: `frontend/package.json`에 반드시 명시 후 Docker 빌드.

**IDE import 에러** (Swagger, POI, OpenPDF): Docker 빌드는 정상. Gradle 리로드하면 해결되나 무시해도 됨.

## Configuration

`src/main/resources/application.yml`:
- 기본: `ddl-auto: validate`
- dev 프로파일: `ddl-auto: create-drop`, show-sql: true
- prod 프로파일: `ddl-auto: update` (Docker 최초 실행 대응)
- CORS allowed origins: `http://localhost`, `http://localhost:80`, `http://localhost:3000`

## Detailed Documentation

- [docs/API.md](docs/API.md) — REST API 전체 명세 (10 domains: Auth, Users, Students, Grades, Records, Feedback, Counseling, Notifications, Reports, Subjects)
- [docs/ERD.md](docs/ERD.md) — DB 스키마 / 엔티티 관계
- [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) — 레이어 다이어그램, 패키지 구조
- [docs/DESIGN.md](docs/DESIGN.md) — UI 디자인 시스템
- [docs/WIREFRAME.md](docs/WIREFRAME.md) — 페이지별 와이어프레임
- [BACKLOG.md](BACKLOG.md) — 제품 백로그 (6 스프린트, 139 SP)

## Git Conventions

Branch naming: `feature/EP-XX-short-description` off `develop`, merge to `develop`, then to `main` for releases.

Commit prefix conventions: `[feat]`, `[fix]`, `[docs]`, `[refactor]`, `[test]`, `[chore]`
