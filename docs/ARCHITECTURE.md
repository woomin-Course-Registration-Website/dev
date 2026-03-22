# 시스템 아키텍처 설계

```text
           ┌───────────────────────────────┐
           │         사용자 (교사/학생)     │
           └───────────────┬───────────────┘
                           │ HTTPS
                           ▼
┌──────────────────────────────────────────────────────┐
│     프론트엔드 (React + Vite) - 웹 브라우저          │
│  - 로그인/대시보드/학생·성적·상담 관리 화면         │
└───────────────────────┬──────────────────────────────┘
                        │ HTTPS / JSON (REST API)
                        ▼
┌──────────────────────────────────────────────────────┐
│             백엔드 (Spring Boot / Java 21)           │
│ ┌──────────────────────────────────────────────────┐ │
│ │ Controller 레이어 (REST API)                     │ │
│ │  - AuthController                                │ │
│ │  - StudentController / GradeController ...       │ │
│ └──────────────────────────────────────────────────┘ │
│ ┌──────────────────────────────────────────────────┐ │
│ │ Service 레이어 (비즈니스 로직)                   │ │
│ │  - AuthService                                   │ │
│ │  - StudentService / GradeService ...             │ │
│ └──────────────────────────────────────────────────┘ │
│ ┌──────────────────────────────────────────────────┐ │
│ │ Repository 레이어 (JPA)                          │ │
│ │  - UserRepository / StudentRepository ...        │ │
│ └──────────────────────────────────────────────────┘ │
│                                                      │
│  [보안/인증]                                         │
│   - Spring Security                                  │
│   - JWT 필터 (Access / Refresh Token)               │
│   - JwtUtil                                         │
└───────────────┬──────────────────────────────────────┘
                │ JPA / Hibernate
                ▼
        ┌───────────────────────────────┐
        │     데이터베이스 (MySQL 8)     │
        │  - 학생 / 성적 / 상담 / 피드백 │
        └───────────────────────────────┘

        ┌───────────────────────────────┐
        │  외부 알림 서비스 (선택)      │
        │  - 이메일 / 푸시 알림 등      │
        └───────────────────────────────┘
                  ▲
                  │ 이벤트/알림 요청
                  └ 백엔드에서 연동
```

> 위 다이어그램은 프론트엔드, 백엔드 레이어 구조, 인증(JWT), 데이터베이스, 외부 알림 서비스를 포함한 전체 시스템 구성을 텍스트로 표현한 것입니다.

## 기술 스택


| 영역       | 기술              | 버전     |
| -------- | --------------- | ------ |
| Backend  | Java            | 21     |
| Backend  | Spring Boot     | 3.x    |
| Backend  | Spring Security | 6.x    |
| Backend  | Spring Data JPA | 3.x    |
| ORM      | Hibernate       | 6.x    |
| 인증       | JWT (jjwt)      | 0.12.x |
| Database | MySQL           | 8.x    |
| Frontend | React           | 18.x   |
| Frontend | Vite            | 5.x    |
| Build    | Gradle          | 8.x    |


---

## 레이어 구조

```
┌─────────────────────────────────────┐
│         Frontend (React + Vite)     │  :3000
├─────────────────────────────────────┤
│         REST API (HTTP/JSON)        │
├─────────────────────────────────────┤
│   Presentation Layer (Controller)   │
│   Business Layer     (Service)      │  :8080
│   Persistence Layer  (Repository)   │
├─────────────────────────────────────┤
│         Database (MySQL)            │  :3306
└─────────────────────────────────────┘
```

---

## 백엔드 패키지 구조

```
src/main/java/com/studentmanagement/
├── StudentManagementApplication.java   # 메인 클래스
│
├── config/
│   ├── SecurityConfig.java             # Spring Security 설정
│   ├── JwtConfig.java                  # JWT 설정
│   └── CorsConfig.java                 # CORS 설정
│
├── controller/
│   ├── AuthController.java
│   ├── StudentController.java
│   ├── GradeController.java
│   ├── StudentRecordController.java
│   ├── FeedbackController.java
│   ├── CounselingController.java
│   └── NotificationController.java
│
├── service/
│   ├── AuthService.java
│   ├── StudentService.java
│   ├── GradeService.java
│   ├── StudentRecordService.java
│   ├── FeedbackService.java
│   ├── CounselingService.java
│   └── NotificationService.java
│
├── repository/
│   ├── UserRepository.java
│   ├── StudentRepository.java
│   ├── SubjectRepository.java
│   ├── GradeRepository.java
│   ├── StudentRecordRepository.java
│   ├── FeedbackRepository.java
│   ├── CounselingRepository.java
│   └── NotificationRepository.java
│
├── domain/
│   ├── User.java
│   ├── Student.java
│   ├── Subject.java
│   ├── Grade.java
│   ├── StudentRecord.java
│   ├── Feedback.java
│   ├── Counseling.java
│   └── Notification.java
│
├── dto/
│   ├── request/
│   │   ├── LoginRequest.java
│   │   ├── GradeRequest.java
│   │   ├── FeedbackRequest.java
│   │   └── CounselingRequest.java
│   └── response/
│       ├── LoginResponse.java
│       ├── GradeResponse.java
│       ├── FeedbackResponse.java
│       └── CounselingResponse.java
│
├── exception/
│   ├── GlobalExceptionHandler.java
│   ├── ResourceNotFoundException.java
│   └── UnauthorizedException.java
│
└── util/
    └── JwtUtil.java
```

---

## 프론트엔드 구조

```
frontend/
├── index.html
├── vite.config.js
├── package.json
└── src/
    ├── main.jsx
    ├── App.jsx
    │
    ├── api/
    │   ├── axios.js                    # Axios 인스턴스 (인터셉터 포함)
    │   ├── authApi.js
    │   ├── studentApi.js
    │   ├── gradeApi.js
    │   ├── feedbackApi.js
    │   └── counselingApi.js
    │
    ├── components/
    │   ├── common/
    │   │   ├── Header.jsx
    │   │   ├── Sidebar.jsx
    │   │   └── Layout.jsx
    │   └── charts/
    │       └── RadarChart.jsx          # 성적 레이더 차트
    │
    ├── pages/
    │   ├── Login.jsx
    │   ├── Dashboard.jsx
    │   ├── students/
    │   │   ├── StudentList.jsx
    │   │   └── StudentDetail.jsx
    │   ├── grades/
    │   │   └── GradeManagement.jsx
    │   ├── feedback/
    │   │   └── FeedbackManagement.jsx
    │   └── counseling/
    │       └── CounselingManagement.jsx
    │
    ├── store/
    │   └── authStore.js                # Zustand 인증 상태
    │
    └── utils/
        └── tokenUtils.js               # JWT 토큰 관리
```

---

## 인증 흐름

```
1. 로그인 요청 (POST /api/auth/login)
2. Spring Security 인증 처리
3. Access Token (15분) + Refresh Token (7일) 발급
4. 클라이언트: Access Token → Authorization Header
5. Access Token 만료 시 Refresh Token으로 재발급 (POST /api/auth/refresh)
6. Refresh Token 만료 시 재로그인
```

---

## CORS 설정

- 허용 Origin: `http://localhost:3000` (개발), 배포 URL (운영)
- 허용 Method: GET, POST, PUT, DELETE, OPTIONS
- 허용 Header: Authorization, Content-Type

