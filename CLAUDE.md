# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 프로젝트 개요

교사용 학생 성적 및 상담 관리 시스템 — 학생 성적, 생활기록부, 피드백, 상담 이력을 관리하는 풀스택 웹 애플리케이션.

## 빌드 & 실행 명령어

### Docker (권장)
```bash
docker compose up --build    # 전체 스택 빌드 & 실행
docker compose up            # 빌드 없이 실행 (이미지 있을 때)
docker compose down          # 종료
```
접속: `http://localhost:3000` (프론트엔드), `http://localhost:8080/swagger-ui/index.html` (Swagger)

### 백엔드 (Spring Boot, 포트 :8080)
```bash
./gradlew bootRun          # 애플리케이션 실행
./gradlew build            # JAR 빌드
./gradlew test             # 전체 테스트 실행
./gradlew test --tests "com.studentmanagement.ClassName"         # 단일 테스트 클래스
./gradlew test --tests "com.studentmanagement.ClassName.method"  # 단일 테스트 메서드
./gradlew clean            # 빌드 아티팩트 정리
```

**로컬 개발 환경 변수** (application.yml 기본값 덮어쓰기):
```
DB_USERNAME=root  DB_PASSWORD=password
JWT_SECRET=local-dev-secret-key-must-be-at-least-256-bits-long
MAIL_USERNAME=  MAIL_PASSWORD=   # 선택 사항; 미설정 시 이메일 기능 비활성
```

**개발용 더미 데이터 시딩** (서버 실행 후):
```bash
curl -X POST http://localhost:8080/api/dev/seed
```
`/api/dev/**`는 인증 없이 접근 가능. 이미 존재하는 레코드는 건너뜀.

### 프론트엔드 (React/Vite, 포트 :3000)
```bash
cd frontend
npm install                # 의존성 설치 (lock 파일 동기화용)
npm run dev                # 개발 서버 실행 (/api 요청을 :8080으로 프록시)
npm run build              # 프로덕션 빌드
```

## 아키텍처

```
React 18 (Vite) :3000 / nginx :80
      ↓ REST API (/api/**)
Spring Boot 3.3 :8080
      ↓ JPA/Hibernate
MySQL 8 :3306  (DB: student_management)
```

### 백엔드 (`src/main/java/com/studentmanagement/`)
- `config/` — `SecurityConfig`, `JwtConfig`, `CorsConfig` (inner @ConfigurationProperties), `SwaggerConfig`
- `domain/` — User, Student, Subject, Grade, StudentRecord, Feedback, Counseling, Notification
- `repository/` — 전체 구현 (커스텀 JPQL 포함)
- `service/` — Auth, Student, Grade, StudentRecord, Feedback, Counseling, Notification, Subject, Report
- `controller/` — 전체 구현 (Auth, User, Student, Grade, StudentRecord, Feedback, Counseling, Notification, Subject, Report)
- `dto/` — `ApiResponse<T>`, 도메인별 Request/Response, report/ReportPreviewResponse
- `exception/` — GlobalExceptionHandler, ResourceNotFoundException, UnauthorizedException
- `util/JwtUtil` — JWT 생성/검증

역할: `TEACHER`, `STUDENT`, `PARENT`, `ADMIN`
인증: JWT accessToken 15분 + refreshToken 7일, stateless 필터

### 프론트엔드 (`frontend/src/`)
- `App.jsx` — React Router v6, PrivateRoute, `/register` 라우트 포함
- `store/authStore.js` — Zustand + localStorage 영속화 (login/logout/setAccessToken)
- `api/client.js` — axios baseURL `/api`, 401 자동 refresh + 큐잉
- `api/` — auth, students, grades, records, feedbacks, counselings, notifications, subjects, reports
- `pages/` — Login, Register, Dashboard, StudentList, StudentDetail, GradeManagement, FeedbackManagement, CounselingManagement, Reports (전부 API 연동 완료)
- `components/common/` — Header (실시간 알림), Sidebar, Layout

### 주요 라이브러리
| 레이어 | 라이브러리 |
|--------|-----------|
| 백엔드 | Java 21, Spring Boot 3.3.0, jjwt 0.12.6, Spring Data JPA, poi-ooxml 5.2.5, openpdf 1.3.30, springdoc-openapi 2.5.0 |
| 프론트엔드 | React 18.3.1, React Router 6.26.0, Zustand 4.5.4, Axios 1.7.2, Recharts 2.12.7, Tailwind CSS 3.4.7 |

## API 응답 형식

모든 엔드포인트는 `ApiResponse<T>` — `{ success: boolean, data: T, message: string }` 형식으로 응답.

팩토리 메서드: `ApiResponse.ok(data)` / `ApiResponse.ok(data, message)`. 에러 응답은 `GlobalExceptionHandler`가 직접 생성하며 `ResourceNotFoundException` (404), `UnauthorizedException` (403), `MethodArgumentNotValidException` (400), 일반 500 에러를 처리.

## 인가 패턴

`SecurityConfig`는 stateless JWT 필터 사용 (Spring 세션 없음). 세밀한 접근 제어는 서비스 또는 컨트롤러 메서드의 `@PreAuthorize`로 강제 (`@EnableMethodSecurity` 활성화). Spring Security 역할 문자열은 `ROLE_TEACHER`, `ROLE_STUDENT` 등이지만, `@PreAuthorize("hasRole('TEACHER')")`는 접두사를 자동으로 제거.

공개 엔드포인트: `/api/auth/**`, `/api/health`, `/api/dev/**`, Swagger UI.

## 중요 컨벤션

**YAML List 주입**: `@Value`로 List 타입 주입 금지. `CorsConfig`처럼 inner static `@ConfigurationProperties` 클래스 사용.

**Enum 매핑**:
- Feedback category: `GRADE | BEHAVIOR | ATTENDANCE | ATTITUDE | OTHER`
- Counseling shareScope: `ALL (전체공개) | PRIVATE (비공개)`

**프론트엔드 Dockerfile**: `npm ci` 아닌 `npm install` 사용 (lock 파일 동기화 문제).

**새 npm 패키지**: `frontend/package.json`에 반드시 명시 후 Docker 빌드.

**IDE import 에러** (Swagger, POI, OpenPDF): Docker 빌드는 정상. Gradle 리로드하면 해결되나 무시해도 됨.

## 설정

`src/main/resources/application.yml`:
- 기본: `ddl-auto: validate`
- dev 프로파일: `ddl-auto: create-drop`, show-sql: true
- prod 프로파일: `ddl-auto: update` (Docker 최초 실행 대응)
- CORS: `allowed-origins: "*"` (개발용 와일드카드; 프로덕션에서 환경변수로 좁혀야 함)

## 테스트 패턴

**Controller 테스트**: `@WebMvcTest` + `@TestPropertySource`로 JWT 속성 주입. `SecurityTestHelper`로 역할 스텁 — Bearer 토큰: `"Bearer fake.test.token"`.

```java
// 사용 가능한 스텁 메서드
SecurityTestHelper.stubAsTeacher(jwtUtil);
SecurityTestHelper.stubAsStudent(jwtUtil);
SecurityTestHelper.stubAsParent(jwtUtil);
SecurityTestHelper.stubAsAdmin(jwtUtil);
SecurityTestHelper.stubAsInvalid(jwtUtil);
```

**픽스처**: `TestFixtures` 클래스의 정적 빌더 메서드로 엔티티 생성 (`teacherUser()`, `student(linkedUser)` 등). 리플렉션으로 ID 주입.

**Service 트랜잭션 패턴**: 클래스 레벨 `@Transactional(readOnly = true)` + 쓰기 메서드에 `@Transactional` 개별 오버라이드.

**소유권 검증**: 서비스 계층에서 `email` 기반으로 생성자 본인 여부 확인 후 수정/삭제 허용 (e.g. CounselingService).

**알림 연동**: 서비스에서 도메인 이벤트 발생 시 `NotificationService.send()`로 직접 호출 (e.g. 성적 입력 후 학생/학부모에게 알림).

**역할별 응답 필터링**: 서비스에서 요청자 역할(TEACHER vs STUDENT/PARENT)에 따라 반환 데이터 차등 처리 (e.g. FeedbackService).

## CI/CD

- **CI** (`.github/workflows/ci.yml`): `main`/`develop` 브랜치 push/PR 시 실행. 실제 MySQL 컨테이너로 `SPRING_PROFILES_ACTIVE: dev`에서 테스트 → JAR 빌드.
- **백엔드 CD** (`.github/workflows/cd.yml`): `main` push 시(`k8s/**`·`docs/**`·`frontend/**` 제외) OIDC로 AWS 인증 → **ECR**에 backend 이미지 빌드/푸시(불변 태그 `github.sha`) → `kustomize edit set image`로 `k8s/overlays/dev`의 이미지 태그를 갱신·커밋(write-back). **CI에 클러스터 자격증명 없음.**
- **프론트엔드 CD**: **Amplify Console**이 GitHub webhook으로 자동 빌드/배포(`amplify.yml`). branch=`develop`→`dev.<domain>`, `staging`→`staging.<domain>`, `main`→`<domain>`(루트). GitHub Actions 워크플로우 불필요.
- **GitOps 배포**: **Argo CD**(EKS `argocd` 네임스페이스)가 git을 watch하여 `k8s/overlays/{env}`를 각 `student-mgmt-{env}` 네임스페이스에 동기화. dev는 자동, staging/prod는 `promote.yml`(`workflow_dispatch`) PR 머지로 승격. Argo CD·ApplicationSet·AppProject는 Terraform `helm_release`(`infra/terraform/eks.tf` + `k8s/bootstrap/` 로컬 차트)로 부트스트랩.
- **시크릿**: 운영자가 `kubeseal`로 암호화한 `SealedSecret`을 `k8s/overlays/{env}/sealed-secrets/`에 커밋 → Argo가 sync → 클러스터 내 sealed-secrets controller가 복호화하여 Secret 생성 → Pod `envFrom`.
- **진입 토폴로지**: 브라우저 → Amplify(SPA, public) / 브라우저 → API Gateway HTTP API(public, 환경별 3개) → VPC Link → private ALB → EKS backend.
- **인프라**: **Terraform**(`infra/terraform/`)으로 VPC·EKS·RDS·ECR·ACM·Route53·Amplify·API Gateway·Sealed Secrets controller를 관리. **Kustomize**(`k8s/base` + `k8s/overlays/{dev,staging,prod}`)로 Deployment·Service·HPA·PDB·Ingress·NetworkPolicy·SealedSecret·ServiceMonitor 구성.

## 상세 문서

- [docs/API.md](docs/API.md) — REST API 전체 명세 (10개 도메인: Auth, Users, Students, Grades, Records, Feedback, Counseling, Notifications, Reports, Subjects)
- [docs/ERD.md](docs/ERD.md) — DB 스키마 / 엔티티 관계
- [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) — 백엔드/프론트 코드 레이어·패키지 구조
- [docs/SYSTEM_ARCHITECTURE.md](docs/SYSTEM_ARCHITECTURE.md) — 런타임 토폴로지·환경 분리·요청/배포 흐름·보안 경계
- [docs/INFRASTRUCTURE.md](docs/INFRASTRUCTURE.md) — AWS 리소스 카탈로그·EKS 애드온·IAM/IRSA·Terraform 부트스트랩
- [docs/DESIGN.md](docs/DESIGN.md) — UI 디자인 시스템
- [docs/WIREFRAME.md](docs/WIREFRAME.md) — 페이지별 와이어프레임
- [BACKLOG.md](BACKLOG.md) — 제품 백로그 (8 스프린트, 181 SP)

## Git 컨벤션

브랜치 명명: `develop`에서 `feature/EP-XX-간단한-설명` 생성, `develop`으로 병합, 릴리즈 시 `main`으로 병합.

커밋 접두사 컨벤션: `[feat]`, `[fix]`, `[docs]`, `[refactor]`, `[test]`, `[chore]`
