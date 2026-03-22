# ERD 및 DB 스키마 설계

## 엔티티 관계도

```
┌──────────┐       ┌───────────┐       ┌──────────┐
│   User   │ 1   1 │  Student  │ N   N │  Subject │
│──────────│───────│───────────│───────│──────────│
│ id (PK)  │       │ id (PK)   │       │ id (PK)  │
│ email    │       │ user_id   │       │ name     │
│ password │       │ grade     │       └──────────┘
│ name     │       │ class_num │            │
│ role     │       │ student   │            │ (Grade 테이블로 연결)
│ created  │       │   _num    │            │
└──────────┘       └───────────┘            │
     │                   │                  │
     │ 1                 │ 1                │
     │                   │                  │
     │ N                 │ N                │
     ▼                   ▼                  │
┌──────────┐       ┌───────────┐            │
│ Feedback │       │   Grade   │◄───────────┘
│──────────│       │───────────│
│ id (PK)  │       │ id (PK)   │
│ teacher  │       │ student_id│
│   _id    │       │ subject_id│
│ student  │       │ semester  │
│   _id    │       │ year      │
│ category │       │ score     │
│ content  │       │ grade_rank│
│ is_public│       └───────────┘
│ created  │
└──────────┘
     │
     │ (User)
     │
┌──────────────┐   ┌──────────────┐   ┌──────────────┐
│  Counseling  │   │StudentRecord │   │ Notification │
│──────────────│   │──────────────│   │──────────────│
│ id (PK)      │   │ id (PK)      │   │ id (PK)      │
│ teacher_id   │   │ student_id   │   │ user_id      │
│ student_id   │   │ attendance   │   │ type         │
│ date         │   │ special_notes│   │ message      │
│ content      │   │ updated_at   │   │ is_read      │
│ next_plan    │   └──────────────┘   │ created_at   │
│ share_scope  │                      └──────────────┘
│ created_at   │
└──────────────┘
```

---

## 테이블 스키마

### users
```sql
CREATE TABLE users (
    id          BIGINT          NOT NULL AUTO_INCREMENT,
    email       VARCHAR(100)    NOT NULL UNIQUE,
    password    VARCHAR(255)    NOT NULL,
    name        VARCHAR(50)     NOT NULL,
    role        ENUM('TEACHER', 'STUDENT', 'PARENT', 'ADMIN') NOT NULL,
    created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);
```

### students
```sql
CREATE TABLE students (
    id          BIGINT          NOT NULL AUTO_INCREMENT,
    user_id     BIGINT          NULL,           -- 학생 계정 연동 (없을 수도 있음)
    name        VARCHAR(50)     NOT NULL,
    grade       INT             NOT NULL,       -- 학년 (1~3)
    class_num   INT             NOT NULL,       -- 반
    student_num INT             NOT NULL,       -- 번호
    created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
);
```

### subjects
```sql
CREATE TABLE subjects (
    id      BIGINT          NOT NULL AUTO_INCREMENT,
    name    VARCHAR(50)     NOT NULL UNIQUE,
    PRIMARY KEY (id)
);
```

### grades
```sql
CREATE TABLE grades (
    id          BIGINT          NOT NULL AUTO_INCREMENT,
    student_id  BIGINT          NOT NULL,
    subject_id  BIGINT          NOT NULL,
    year        INT             NOT NULL,       -- 연도 (예: 2025)
    semester    INT             NOT NULL,       -- 학기 (1 or 2)
    score       DECIMAL(5,2)    NULL,
    grade_rank  VARCHAR(2)      NULL,           -- 등급 (A+, A, B+ ...)
    created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
    FOREIGN KEY (subject_id) REFERENCES subjects(id),
    UNIQUE KEY uq_grade (student_id, subject_id, year, semester)
);
```

### student_records
```sql
CREATE TABLE student_records (
    id              BIGINT  NOT NULL AUTO_INCREMENT,
    student_id      BIGINT  NOT NULL UNIQUE,
    attendance      JSON    NULL,              -- {"present": 180, "absent": 2, "late": 3}
    special_notes   TEXT    NULL,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE
);
```

### feedbacks
```sql
CREATE TABLE feedbacks (
    id          BIGINT          NOT NULL AUTO_INCREMENT,
    teacher_id  BIGINT          NOT NULL,
    student_id  BIGINT          NOT NULL,
    category    ENUM('GRADE', 'BEHAVIOR', 'ATTENDANCE', 'ATTITUDE', 'OTHER') NOT NULL,
    content     TEXT            NOT NULL,
    is_public   BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    FOREIGN KEY (teacher_id) REFERENCES users(id),
    FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE
);
```

### counselings
```sql
CREATE TABLE counselings (
    id          BIGINT          NOT NULL AUTO_INCREMENT,
    teacher_id  BIGINT          NOT NULL,
    student_id  BIGINT          NOT NULL,
    date        DATE            NOT NULL,
    content     TEXT            NOT NULL,
    next_plan   TEXT            NULL,
    share_scope ENUM('ALL', 'PRIVATE') NOT NULL DEFAULT 'ALL',
    created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    FOREIGN KEY (teacher_id) REFERENCES users(id),
    FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE
);
```

### notifications
```sql
CREATE TABLE notifications (
    id          BIGINT          NOT NULL AUTO_INCREMENT,
    user_id     BIGINT          NOT NULL,
    type        ENUM('GRADE', 'FEEDBACK', 'COUNSELING') NOT NULL,
    message     VARCHAR(500)    NOT NULL,
    is_read     BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
```

---

## 인덱스 전략

```sql
-- 성적 조회 성능
CREATE INDEX idx_grades_student_year ON grades(student_id, year, semester);

-- 피드백 조회
CREATE INDEX idx_feedbacks_student ON feedbacks(student_id, created_at DESC);
CREATE INDEX idx_feedbacks_teacher ON feedbacks(teacher_id, created_at DESC);

-- 상담 조회/검색
CREATE INDEX idx_counselings_student ON counselings(student_id, date DESC);
CREATE INDEX idx_counselings_teacher ON counselings(teacher_id, date DESC);

-- 알림 조회
CREATE INDEX idx_notifications_user ON notifications(user_id, is_read, created_at DESC);

-- 학생 목록 조회
CREATE INDEX idx_students_grade_class ON students(grade, class_num, student_num);
```
