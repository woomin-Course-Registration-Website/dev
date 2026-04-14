# API 설계 명세서

## 공통 규칙

- Base URL: `/api`
- 인증: `Authorization: Bearer {accessToken}` (로그인 API 제외)
- Content-Type: `application/json`
- 응답 공통 형식:
```json
{
  "success": true,
  "data": { ... },
  "message": "string"
}
```
- 에러 응답:
```json
{
  "success": false,
  "error": "에러 메시지",
  "code": "ERROR_CODE"
}
```

---

## 1. 인증 (Auth)

### POST /api/auth/register
회원가입

> `ADMIN` 계정은 회원가입으로 생성 불가. 기존 ADMIN이 `POST /api/users`로 생성해야 합니다.

**Request Body:**
```json
{
  "email": "teacher@school.kr",
  "password": "password123",
  "name": "김교사",
  "role": "TEACHER"
}
```

**role 값:** `TEACHER` / `STUDENT` / `PARENT`

**Response (201):**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "email": "teacher@school.kr",
    "name": "김교사",
    "role": "TEACHER",
    "createdAt": "2025-03-14T10:00:00"
  },
  "message": "회원가입이 완료되었습니다."
}
```

---

### POST /api/auth/login
로그인

**Request Body:**
```json
{
  "email": "teacher@school.kr",
  "password": "password123"
}
```

**Response:**
```json
{
  "accessToken": "eyJ...",
  "refreshToken": "eyJ...",
  "user": {
    "id": 1,
    "name": "김교사",
    "role": "TEACHER"
  }
}
```

---

### POST /api/auth/logout
로그아웃 (Refresh Token 무효화)

---

### POST /api/auth/refresh
Access Token 재발급

**Request Body:**
```json
{ "refreshToken": "eyJ..." }
```

---

### POST /api/auth/reset-password
비밀번호 재설정 이메일 발송

**Request Body:**
```json
{ "email": "teacher@school.kr" }
```

---

## 2. 사용자 관리 (Admin)

| Method | URL | 설명 | 권한 |
|--------|-----|------|------|
| GET | /api/users | 사용자 목록 | ADMIN |
| POST | /api/users | 사용자 생성 | ADMIN |
| PUT | /api/users/{id} | 사용자 수정 | ADMIN |
| DELETE | /api/users/{id} | 사용자 삭제 | ADMIN |

---

## 3. 학생 관리

| Method | URL | 설명 | 권한 |
|--------|-----|------|------|
| GET | /api/students | 학생 목록 | TEACHER |
| GET | /api/students/{id} | 학생 상세 | TEACHER, STUDENT(본인), PARENT(자녀) |
| POST | /api/students | 학생 등록 | TEACHER |
| PUT | /api/students/{id} | 학생 정보 수정 | TEACHER |

**GET /api/students Query Params:**
- `grade`: 학년 필터
- `classNum`: 반 필터
- `keyword`: 이름 검색

---

## 4. 성적 관리

| Method | URL | 설명 | 권한 |
|--------|-----|------|------|
| GET | /api/students/{id}/grades | 성적 목록 | TEACHER, STUDENT(본인), PARENT(자녀) |
| POST | /api/students/{id}/grades | 성적 입력 | TEACHER |
| PUT | /api/grades/{id} | 성적 수정 | TEACHER |
| DELETE | /api/grades/{id} | 성적 삭제 | TEACHER |

**GET /api/students/{id}/grades Query Params:**
- `year`: 연도 필터
- `semester`: 학기 필터 (1 or 2)
- `subjectId`: 과목 필터

**POST /api/students/{id}/grades Request Body:**
```json
{
  "subjectId": 1,
  "year": 2025,
  "semester": 1,
  "score": 92.5
}
```

**Response:** (성적 + 자동 계산값 포함)
```json
{
  "id": 1,
  "subject": { "id": 1, "name": "수학" },
  "year": 2025,
  "semester": 1,
  "score": 92.5,
  "gradeRank": "A",
  "average": 85.3,
  "total": 512.0
}
```

---

## 5. 학생부 관리

| Method | URL | 설명 | 권한 |
|--------|-----|------|------|
| GET | /api/students/{id}/records | 학생부 조회 | TEACHER, STUDENT(본인), PARENT(자녀) |
| PUT | /api/students/{id}/records | 학생부 수정 | TEACHER |

**PUT Request Body:**
```json
{
  "attendance": {
    "present": 180,
    "absent": 2,
    "late": 3
  },
  "specialNotes": "수학 경시대회 수상"
}
```

---

## 6. 피드백 관리

| Method | URL | 설명 | 권한 |
|--------|-----|------|------|
| GET | /api/students/{id}/feedbacks | 피드백 목록 | TEACHER, STUDENT/PARENT(공개분) |
| POST | /api/students/{id}/feedbacks | 피드백 작성 | TEACHER |
| PUT | /api/feedbacks/{id} | 피드백 수정 | TEACHER(작성자) |
| DELETE | /api/feedbacks/{id} | 피드백 삭제 | TEACHER(작성자) |

**POST Request Body:**
```json
{
  "category": "GRADE",
  "content": "수학 성적이 많이 향상되었습니다.",
  "isPublic": true
}
```

---

## 7. 상담 내역 관리

| Method | URL | 설명 | 권한 |
|--------|-----|------|------|
| GET | /api/counselings | 상담 목록 | TEACHER |
| GET | /api/counselings/{id} | 상담 상세 | TEACHER |
| POST | /api/counselings | 상담 등록 | TEACHER |
| PUT | /api/counselings/{id} | 상담 수정 | TEACHER(작성자) |
| DELETE | /api/counselings/{id} | 상담 삭제 | TEACHER(작성자) |

**GET /api/counselings Query Params:**
- `studentId`: 학생 필터
- `teacherId`: 교사 필터
- `from`: 시작 날짜 (yyyy-MM-dd)
- `to`: 종료 날짜 (yyyy-MM-dd)

**POST Request Body:**
```json
{
  "studentId": 1,
  "date": "2025-03-14",
  "content": "진로 상담 진행. 이공계 희망.",
  "nextPlan": "2주 후 심화 상담 예정",
  "shareScope": "ALL"
}
```

---

## 8. 알림

| Method | URL | 설명 | 권한 |
|--------|-----|------|------|
| GET | /api/notifications | 알림 목록 | 본인 |
| PUT | /api/notifications/{id}/read | 알림 읽음 처리 | 본인 |
| PUT | /api/notifications/read-all | 전체 읽음 처리 | 본인 |

---

## 9. 보고서 (Reports)

> 권한: `TEACHER`, `ADMIN`

| Method | URL | 설명 |
|--------|-----|------|
| GET | /api/reports/preview  | 보고서 미리보기 데이터 (JSON) |
| GET | /api/reports/download | Excel / PDF 파일 다운로드 |

**보고서 종류 (`type` 파라미터):**
| 값 | 이름 |
|----|------|
| `grade-summary`     | 성적 종합 보고서 |
| `student-record`    | 학생부 보고서 |
| `feedback-report`   | 피드백 현황 보고서 |
| `counseling-report` | 상담 이력 보고서 |

**GET /api/reports/preview Query Params:**
- `type`: 보고서 종류 (필수)
- `grade`: 학년 필터 (선택)
- `classNum`: 반 필터 (선택)
- `year`: 연도 (grade-summary에서 사용)
- `semester`: 학기 (grade-summary에서 사용, 1 또는 2)

**Preview Response:**
```json
{
  "success": true,
  "data": {
    "columns": [
      { "key": "name", "label": "이름", "align": "left" },
      { "key": "avg",  "label": "평균점수", "align": "right" }
    ],
    "rows": [
      { "name": "홍길동", "avg": "85.4", "gradeRank": "B" }
    ],
    "totalCount": 32,
    "generatedAt": "2025-03-22"
  }
}
```

**GET /api/reports/download Query Params:**
- 위 preview 파라미터 동일
- `format`: `excel`(기본값) 또는 `pdf`

**Download Response:**
- Excel: `Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`
- PDF: `Content-Type: application/pdf`
- `Content-Disposition: attachment; filename="grade-summary_2025-03-22.xlsx"`

---

## 10. 과목 (Subject)

| Method | URL | 설명 | 권한 |
|--------|-----|------|------|
| GET | /api/subjects | 과목 목록 | 전체 |
| POST | /api/subjects | 과목 추가 | ADMIN |

---

## HTTP 상태 코드

| 코드 | 의미 |
|------|------|
| 200 | OK |
| 201 | Created |
| 400 | Bad Request (입력값 오류) |
| 401 | Unauthorized (미인증) |
| 403 | Forbidden (권한 없음) |
| 404 | Not Found |
| 500 | Internal Server Error |
