# 기능 보완 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** BACKLOG 미구현/부분구현 스토리 6건(+공용 엔드포인트 1건)을 완성한다.

**Architecture:** Spring Boot(JPA) 백엔드 + React(Vite) 프론트. 백엔드는 클래스 레벨 `@Transactional(readOnly=true)` 서비스 + 파생 쿼리 리포지토리 + `ApiResponse<T>` 래핑 컨트롤러 패턴. 신규 기능은 기존 패턴을 그대로 따르고 TDD(컨트롤러 `@WebMvcTest`, 서비스 단위테스트)로 검증한다.

**Tech Stack:** Java 21, Spring Boot 3.3, Spring Data JPA, MySQL 8, React 18, Axios, Tailwind, JUnit5 + Mockito + MockMvc.

**작업 위치:** worktree `feature/feature-completion` (base 로컬 main `cc3bbf2`). 모든 경로는 이 worktree 기준.

**공통 규칙:**
- 백엔드 테스트 실행: `./gradlew test --tests "com.studentmanagement.<클래스>"`
- 전체 테스트: `./gradlew test`
- 컨트롤러 테스트는 `@WebMvcTest(XxxController.class) + @Import(SecurityConfig.class) + @TestPropertySource(jwt.*)` + `SecurityTestHelper.stubAsXxx(jwtUtil)` + 헤더 `FAKE_TOKEN`.
- 각 Task 끝에 커밋. 커밋 푸시는 하지 않는다(사용자 요청 시에만).

---

## Task 1: 공용 — 교사 목록 조회 `GET /api/users/teachers`

#2(상담 교사 필터)·#6(상담 특정교사 공유)가 공유하는 선행 엔드포인트. 교사 `{id, name}`만 반환.

**Files:**
- Create: `src/main/java/com/studentmanagement/dto/user/TeacherOptionResponse.java`
- Modify: `src/main/java/com/studentmanagement/repository/UserRepository.java`
- Modify: `src/main/java/com/studentmanagement/service/UserService.java`
- Modify: `src/main/java/com/studentmanagement/controller/UserController.java`
- Test: `src/test/java/com/studentmanagement/controller/UserControllerTest.java`
- Modify(front): `frontend/src/api/users.js`

- [ ] **Step 1: 컨트롤러 테스트 추가 (실패 확인용)**

`UserControllerTest.java`에 메서드 추가:

```java
    // ── getTeachers ───────────────────────────────────────────────────

    @Test
    void getTeachers_teacher_returns200() throws Exception {
        SecurityTestHelper.stubAsTeacher(jwtUtil);
        given(userService.getTeachers()).willReturn(List.of());

        mockMvc.perform(get("/api/users/teachers").header("Authorization", FAKE_TOKEN))
                .andExpect(status().isOk());
    }

    @Test
    void getTeachers_admin_returns200() throws Exception {
        SecurityTestHelper.stubAsAdmin(jwtUtil);
        given(userService.getTeachers()).willReturn(List.of());

        mockMvc.perform(get("/api/users/teachers").header("Authorization", FAKE_TOKEN))
                .andExpect(status().isOk());
    }

    @Test
    void getTeachers_student_returns403() throws Exception {
        SecurityTestHelper.stubAsStudent(jwtUtil);

        mockMvc.perform(get("/api/users/teachers").header("Authorization", FAKE_TOKEN))
                .andExpect(status().isForbidden());
    }
```

`UserService` 모킹에 새 메서드가 필요하다. 상단 import에 이미 `java.util.List` 있음. `TeacherOptionResponse` import는 컴파일 시점엔 service 반환타입으로만 쓰이므로 테스트에서 직접 import 불필요(`List.of()`).

- [ ] **Step 2: 테스트 실행 → 컴파일 실패 확인**

Run: `./gradlew test --tests "com.studentmanagement.controller.UserControllerTest"`
Expected: 컴파일 실패 (`getTeachers()` 메서드 없음).

- [ ] **Step 3: DTO 생성**

`TeacherOptionResponse.java`:

```java
package com.studentmanagement.dto.user;

import com.studentmanagement.domain.User;
import lombok.Getter;

@Getter
public class TeacherOptionResponse {
    private final Long id;
    private final String name;

    public TeacherOptionResponse(User user) {
        this.id = user.getId();
        this.name = user.getName();
    }
}
```

- [ ] **Step 4: 리포지토리에 파생 쿼리 추가**

`UserRepository.java`에 추가:

```java
    java.util.List<User> findByRoleOrderByNameAsc(User.Role role);
```

- [ ] **Step 5: 서비스 메서드 추가**

`UserService.java`에 추가 (import `TeacherOptionResponse`):

```java
    public List<TeacherOptionResponse> getTeachers() {
        return userRepository.findByRoleOrderByNameAsc(User.Role.TEACHER)
                .stream().map(TeacherOptionResponse::new).toList();
    }
```

- [ ] **Step 6: 컨트롤러 매핑 추가**

`UserController.java`의 `/me` 블록과 ADMIN 블록 사이에 추가:

```java
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    @GetMapping("/teachers")
    public ResponseEntity<?> getTeachers() {
        return ResponseEntity.ok(ApiResponse.ok(userService.getTeachers()));
    }
```

- [ ] **Step 7: 테스트 실행 → 통과 확인**

Run: `./gradlew test --tests "com.studentmanagement.controller.UserControllerTest"`
Expected: PASS (전체 메서드).

- [ ] **Step 8: 프론트 api 추가**

`frontend/src/api/users.js` 끝에 추가:

```javascript
/** 교사 목록 옵션 (TEACHER/ADMIN) → [{ id, name }] */
export const getTeachers = () =>
  client.get('/users/teachers').then((r) => r.data.data)
```

- [ ] **Step 9: 커밋**

```bash
git add src/main/java/com/studentmanagement/dto/user/TeacherOptionResponse.java \
        src/main/java/com/studentmanagement/repository/UserRepository.java \
        src/main/java/com/studentmanagement/service/UserService.java \
        src/main/java/com/studentmanagement/controller/UserController.java \
        src/test/java/com/studentmanagement/controller/UserControllerTest.java \
        frontend/src/api/users.js
git commit -m "[feat] 교사 목록 조회 엔드포인트 GET /api/users/teachers 추가"
```

---

## Task 2: 비밀번호 재설정 UI (US-01-05)

백엔드(`POST /api/auth/reset-password`)와 프론트 api(`resetPassword(email)`)는 이미 존재. **Login 화면에 진입점/모달만** 추가한다.

**Files:**
- Modify(front): `frontend/src/pages/Login.jsx`

- [ ] **Step 1: Login.jsx 현재 구조 확인**

Read: `frontend/src/pages/Login.jsx` 전체. 폼 상태(`email`, `password`, 에러/로딩), 제출 핸들러, 카드 내부 마크업 위치를 파악한다. `resetPassword`는 `../api/auth`에서 import.

- [ ] **Step 2: import 및 상태 추가**

상단 import에 `resetPassword` 추가:

```javascript
import { login as loginApi, resetPassword } from '../api/auth'
```

컴포넌트 상태 추가(기존 useState들 근처):

```javascript
  const [resetOpen, setResetOpen] = useState(false)
  const [resetEmail, setResetEmail] = useState('')
  const [resetMsg, setResetMsg] = useState('')
  const [resetLoading, setResetLoading] = useState(false)
```

- [ ] **Step 3: 핸들러 추가**

```javascript
  const handleReset = async (e) => {
    e.preventDefault()
    setResetLoading(true)
    setResetMsg('')
    try {
      await resetPassword(resetEmail)
    } catch {
      // 계정 존재 여부를 노출하지 않기 위해 성공/실패 동일 메시지
    } finally {
      setResetLoading(false)
      setResetMsg('해당 이메일로 가입된 계정이 있다면 임시 비밀번호를 발송했습니다.')
    }
  }
```

- [ ] **Step 4: 진입 링크 추가**

로그인 폼의 비밀번호 입력 아래(제출 버튼 부근)에 추가:

```jsx
        <button
          type="button"
          onClick={() => { setResetOpen(true); setResetMsg(''); setResetEmail('') }}
          className="text-sm text-primary-200 hover:text-white mt-3"
        >
          비밀번호를 잊으셨나요?
        </button>
```

- [ ] **Step 5: 재설정 모달 추가**

컴포넌트 return 최상위(로그인 카드와 형제)에 추가:

```jsx
      {resetOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div className="absolute inset-0 bg-black/50 backdrop-blur-sm" onClick={() => setResetOpen(false)} />
          <form onSubmit={handleReset} className="relative bg-white rounded-2xl shadow-modal w-full max-w-sm p-6 animate-slide-up">
            <h3 className="font-bold text-gray-900 text-lg mb-1">비밀번호 재설정</h3>
            <p className="text-sm text-gray-500 mb-4">가입한 이메일로 임시 비밀번호를 보내드립니다.</p>
            <input
              type="email" required value={resetEmail}
              onChange={(e) => setResetEmail(e.target.value)}
              placeholder="이메일" className="input w-full mb-3"
            />
            {resetMsg && <p className="text-sm text-green-600 mb-3">{resetMsg}</p>}
            <div className="flex gap-2 justify-end">
              <button type="button" onClick={() => setResetOpen(false)} className="btn-md btn-ghost">닫기</button>
              <button type="submit" disabled={resetLoading} className="btn-md btn-primary">
                {resetLoading ? '발송 중…' : '발송'}
              </button>
            </div>
          </form>
        </div>
      )}
```

- [ ] **Step 6: 수동 확인**

`cd frontend && npm run build` 로 빌드 오류 없음 확인. (실 서버 동작은 통합 환경에서 확인)
Expected: 빌드 성공.

- [ ] **Step 7: 커밋**

```bash
git add frontend/src/pages/Login.jsx
git commit -m "[feat] 로그인 화면에 비밀번호 재설정 모달 추가 (US-01-05)"
```

---

## Task 3: 상담 교사별/날짜 필터 (US-05-03)

백엔드 `getAll(studentId, teacherId, from, to)`는 이미 지원. 프론트 필터 UI + api 파라미터만 추가.

**Files:**
- Modify(front): `frontend/src/api/counselings.js`
- Modify(front): `frontend/src/pages/counseling/CounselingManagement.jsx`

- [ ] **Step 1: api 목록 조회에 파라미터 전달 확인/수정**

Read: `frontend/src/api/counselings.js`. 목록 조회 함수가 `params`를 `client.get('/counselings', { params })`로 넘기는지 확인. 없으면 다음 형태로 수정:

```javascript
/** 상담 목록 (필터: studentId, teacherId, from, to) */
export const getCounselings = (params = {}) =>
  client.get('/counselings', { params }).then((r) => r.data.data)
```

- [ ] **Step 2: 페이지에서 교사 목록 로드**

`CounselingManagement.jsx` 상단 import에 `getTeachers` 추가:

```javascript
import { getTeachers } from '../../api/users'
```

상태 + 로드 추가:

```javascript
  const [teachers, setTeachers] = useState([])
  const [teacherFilter, setTeacherFilter] = useState('')
  const [fromDate, setFromDate] = useState('')
  const [toDate, setToDate] = useState('')

  useEffect(() => {
    getTeachers().then(setTeachers).catch(() => setTeachers([]))
  }, [])
```

- [ ] **Step 2.5: 목록 조회 시 필터 파라미터 전달**

상담 목록을 불러오는 호출부를 다음처럼 파라미터 포함하도록 수정 (빈 값은 제외):

```javascript
  const params = {}
  if (teacherFilter) params.teacherId = teacherFilter
  if (fromDate) params.from = fromDate
  if (toDate) params.to = toDate
  const list = await getCounselings(params)
```

`teacherFilter/fromDate/toDate`를 로드 useEffect 의존성에 추가.

- [ ] **Step 3: 필터바 UI 추가**

기존 학생 이름 검색 input 옆(필터바 영역)에 추가:

```jsx
        <select value={teacherFilter} onChange={(e) => setTeacherFilter(e.target.value)} className="input w-36 h-9 py-1.5">
          <option value="">전체 교사</option>
          {teachers.map((t) => <option key={t.id} value={t.id}>{t.name}</option>)}
        </select>
        <input type="date" value={fromDate} onChange={(e) => setFromDate(e.target.value)} className="input w-40 h-9 py-1.5" />
        <input type="date" value={toDate} onChange={(e) => setToDate(e.target.value)} className="input w-40 h-9 py-1.5" />
```

- [ ] **Step 4: 빌드 확인**

Run: `cd frontend && npm run build`
Expected: 성공.

- [ ] **Step 5: 커밋**

```bash
git add frontend/src/api/counselings.js frontend/src/pages/counseling/CounselingManagement.jsx
git commit -m "[feat] 상담 목록 교사별/기간 필터 추가 (US-05-03)"
```

---

## Task 4: 성적 반/과목 일괄 조회 (G-HIGH-1 버그)

근본 원인: 반·과목 단위 조회 API 부재 → `GradeManagement`가 `getGrades(null)` 호출 → 기존 성적이 항상 빈값. 신규 엔드포인트로 해결.

**Files:**
- Modify: `src/main/java/com/studentmanagement/repository/GradeRepository.java`
- Modify: `src/main/java/com/studentmanagement/service/GradeService.java`
- Modify: `src/main/java/com/studentmanagement/controller/GradeController.java`
- Test: `src/test/java/com/studentmanagement/controller/GradeControllerTest.java`
- Modify(front): `frontend/src/api/grades.js`
- Modify(front): `frontend/src/pages/grades/GradeManagement.jsx`

- [ ] **Step 1: 기존 코드 확인**

Read: `GradeRepository.java`, `GradeService.java`(특히 `getStats`와 `getGrades` 시그니처/반환 DTO), `GradeControllerTest.java`. 성적 응답 DTO명(예: `GradeResponse`)과 student id 포함 여부를 확인한다. `getStats` JPQL을 일괄 조회 쿼리 작성의 참고로 삼는다.

- [ ] **Step 2: 컨트롤러 테스트 추가**

`GradeControllerTest.java`에 추가(기존 import/패턴 따름):

```java
    @Test
    void getGradesByClass_teacher_returns200() throws Exception {
        SecurityTestHelper.stubAsTeacher(jwtUtil);
        given(gradeService.getGradesByClass(any(), any(), any(), any(), any()))
                .willReturn(java.util.List.of());

        mockMvc.perform(get("/api/grades")
                .param("subjectId", "1").param("grade", "1").param("classNum", "2")
                .param("year", "2025").param("semester", "1")
                .header("Authorization", FAKE_TOKEN))
                .andExpect(status().isOk());
    }

    @Test
    void getGradesByClass_student_returns403() throws Exception {
        SecurityTestHelper.stubAsStudent(jwtUtil);

        mockMvc.perform(get("/api/grades").param("subjectId", "1").header("Authorization", FAKE_TOKEN))
                .andExpect(status().isForbidden());
    }
```

(메서드 시그니처는 Step 4의 서비스 시그니처와 일치시킬 것.)

- [ ] **Step 3: 테스트 실행 → 실패 확인**

Run: `./gradlew test --tests "com.studentmanagement.controller.GradeControllerTest"`
Expected: 컴파일 실패(`getGradesByClass` 없음).

- [ ] **Step 4: 리포지토리 쿼리 추가**

`GradeRepository.java`에 추가 (파라미터 null이면 무시하는 JPQL; 반환은 `Grade` 엔티티). 실제 컬럼명은 Step 1에서 확인한 엔티티 필드명에 맞춘다:

```java
    @org.springframework.data.jpa.repository.Query("""
        select g from Grade g
        where g.subject.id = :subjectId
          and (:grade is null or g.student.grade = :grade)
          and (:classNum is null or g.student.classNum = :classNum)
          and (:year is null or g.year = :year)
          and (:semester is null or g.semester = :semester)
        order by g.student.studentNum asc
    """)
    java.util.List<Grade> findByClassAndSubject(Long subjectId, Integer grade, Integer classNum,
                                                Integer year, Integer semester);
```

- [ ] **Step 5: 서비스 메서드 추가**

`GradeService.java`에 추가. 반환 DTO는 기존 단건 조회와 동일한 타입을 재사용(Step 1에서 확인한 `GradeResponse` 가정):

```java
    public List<GradeResponse> getGradesByClass(Long subjectId, Integer grade, Integer classNum,
                                                Integer year, Integer semester) {
        return gradeRepository.findByClassAndSubject(subjectId, grade, classNum, year, semester)
                .stream().map(GradeResponse::new).toList();
    }
```

(`GradeResponse` 생성자가 `Grade`를 받지 않으면, 기존 `getGrades`가 사용하는 매핑 방식을 동일하게 적용한다.)

- [ ] **Step 6: 컨트롤러 매핑 추가**

`GradeController.java`에 추가:

```java
    @Operation(summary = "반/과목별 성적 일괄 조회", description = "성적 입력 화면용. subjectId 필수, grade/classNum/year/semester 선택.")
    @GetMapping("/api/grades")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<?> getGradesByClass(
            @RequestParam Long subjectId,
            @RequestParam(required = false) Integer grade,
            @RequestParam(required = false) Integer classNum,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer semester) {
        return ResponseEntity.ok(ApiResponse.ok(
                gradeService.getGradesByClass(subjectId, grade, classNum, year, semester)));
    }
```

- [ ] **Step 7: 테스트 실행 → 통과**

Run: `./gradlew test --tests "com.studentmanagement.controller.GradeControllerTest"`
Expected: PASS.

- [ ] **Step 8: 프론트 api 추가**

`frontend/src/api/grades.js`에 추가:

```javascript
/** 반/과목별 성적 일괄 조회 (성적 입력 화면) */
export const getGradesByClass = (params = {}) =>
  client.get('/grades', { params }).then((r) => r.data.data)
```

- [ ] **Step 9: GradeManagement 연결 수정**

`frontend/src/pages/grades/GradeManagement.jsx`:
- import에 `getGradesByClass` 추가(`getGrades` 단건은 유지 또는 제거).
- `loadData`의 `getGrades(null, { year, semester, subjectId })` 호출을 교체:

```javascript
        getGradesByClass({ subjectId, grade: gradeFilter, classNum: classFilter, year, semester }).catch(() => []),
```

응답 항목의 student id 접근(`g.student?.id ?? g.studentId`)은 기존 로직 유지(반환 DTO 구조에 맞게 Step 1 확인 결과 반영).

- [ ] **Step 10: 빌드 확인 + 커밋**

```bash
cd frontend && npm run build && cd ..
git add src/main/java/com/studentmanagement/repository/GradeRepository.java \
        src/main/java/com/studentmanagement/service/GradeService.java \
        src/main/java/com/studentmanagement/controller/GradeController.java \
        src/test/java/com/studentmanagement/controller/GradeControllerTest.java \
        frontend/src/api/grades.js frontend/src/pages/grades/GradeManagement.jsx
git commit -m "[fix] 성적 입력 화면 반/과목 일괄 조회 API 추가 및 연결 (G-HIGH-1)"
```

---

## Task 5: 알림 수신 설정 (US-06-04)

`User`에 알림 타입별 boolean 3개(기본 true). `NotificationService.send()`에서 수신자 설정 확인 후 발송. 내 설정 조회/수정 엔드포인트 + Settings UI.

**Files:**
- Modify: `src/main/java/com/studentmanagement/domain/User.java`
- Create: `src/main/java/com/studentmanagement/dto/user/NotificationSettingsRequest.java`
- Create: `src/main/java/com/studentmanagement/dto/user/NotificationSettingsResponse.java`
- Modify: `src/main/java/com/studentmanagement/service/UserService.java`
- Modify: `src/main/java/com/studentmanagement/controller/UserController.java`
- Modify: `src/main/java/com/studentmanagement/service/NotificationService.java`
- Test: `src/test/java/com/studentmanagement/controller/UserControllerTest.java`
- Test: `src/test/java/com/studentmanagement/service/NotificationServiceTest.java` (없으면 생성)
- Modify(front): `frontend/src/api/users.js`, `frontend/src/pages/settings/Settings.jsx`

- [ ] **Step 1: User 엔티티에 필드 추가**

`User.java`의 `role` 필드 아래에 추가:

```java
    @Column(nullable = false, columnDefinition = "boolean default true")
    private boolean notifyGrade = true;

    @Column(nullable = false, columnDefinition = "boolean default true")
    private boolean notifyFeedback = true;

    @Column(nullable = false, columnDefinition = "boolean default true")
    private boolean notifyCounseling = true;
```

(Lombok `@Getter @Setter`가 클래스에 있으므로 접근자 자동 생성. boolean getter는 `isNotifyGrade()` 형태.)

- [ ] **Step 2: DTO 생성**

`NotificationSettingsResponse.java`:

```java
package com.studentmanagement.dto.user;

import com.studentmanagement.domain.User;
import lombok.Getter;

@Getter
public class NotificationSettingsResponse {
    private final boolean notifyGrade;
    private final boolean notifyFeedback;
    private final boolean notifyCounseling;

    public NotificationSettingsResponse(User user) {
        this.notifyGrade = user.isNotifyGrade();
        this.notifyFeedback = user.isNotifyFeedback();
        this.notifyCounseling = user.isNotifyCounseling();
    }
}
```

`NotificationSettingsRequest.java`:

```java
package com.studentmanagement.dto.user;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class NotificationSettingsRequest {
    private boolean notifyGrade = true;
    private boolean notifyFeedback = true;
    private boolean notifyCounseling = true;
}
```

- [ ] **Step 3: 컨트롤러 테스트 추가 (실패 확인)**

`UserControllerTest.java`에 추가:

```java
    @Test
    void getNotificationSettings_anyRole_returns200() throws Exception {
        SecurityTestHelper.stubAsStudent(jwtUtil);
        given(userService.getNotificationSettings(anyString()))
                .willReturn(new com.studentmanagement.dto.user.NotificationSettingsResponse(TestFixtures.teacherUser()));

        mockMvc.perform(get("/api/users/me/notification-settings").header("Authorization", FAKE_TOKEN))
                .andExpect(status().isOk());
    }

    @Test
    void updateNotificationSettings_anyRole_returns200() throws Exception {
        SecurityTestHelper.stubAsStudent(jwtUtil);
        given(userService.updateNotificationSettings(anyString(), any()))
                .willReturn(new com.studentmanagement.dto.user.NotificationSettingsResponse(TestFixtures.teacherUser()));

        mockMvc.perform(put("/api/users/me/notification-settings")
                .header("Authorization", FAKE_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"notifyGrade":true,"notifyFeedback":false,"notifyCounseling":true}
                    """))
                .andExpect(status().isOk());
    }
```

- [ ] **Step 4: 테스트 실행 → 실패 확인**

Run: `./gradlew test --tests "com.studentmanagement.controller.UserControllerTest"`
Expected: 컴파일 실패.

- [ ] **Step 5: 서비스 메서드 추가**

`UserService.java`에 추가(import 두 DTO):

```java
    public NotificationSettingsResponse getNotificationSettings(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));
        return new NotificationSettingsResponse(user);
    }

    @Transactional
    public NotificationSettingsResponse updateNotificationSettings(String email, NotificationSettingsRequest req) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));
        user.setNotifyGrade(req.isNotifyGrade());
        user.setNotifyFeedback(req.isNotifyFeedback());
        user.setNotifyCounseling(req.isNotifyCounseling());
        return new NotificationSettingsResponse(user);
    }
```

- [ ] **Step 6: 컨트롤러 매핑 추가**

`UserController.java`의 `/me` 블록 근처에 추가(import `NotificationSettingsRequest`):

```java
    @GetMapping("/me/notification-settings")
    public ResponseEntity<?> getNotificationSettings(Authentication auth) {
        return ResponseEntity.ok(ApiResponse.ok(userService.getNotificationSettings(auth.getName())));
    }

    @PutMapping("/me/notification-settings")
    public ResponseEntity<?> updateNotificationSettings(Authentication auth,
                                                        @RequestBody NotificationSettingsRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(userService.updateNotificationSettings(auth.getName(), request)));
    }
```

- [ ] **Step 7: 발송 가드 추가**

Read: `NotificationService.java`. `send(...)` 메서드의 시그니처와 `Notification.Type` 사용 방식을 확인. 수신자 `User`와 `Type`이 결정되는 지점에서, 다음 가드를 추가해 설정이 false면 저장/발송하지 않는다:

```java
    private boolean isEnabled(User user, Notification.Type type) {
        return switch (type) {
            case GRADE -> user.isNotifyGrade();
            case FEEDBACK -> user.isNotifyFeedback();
            case COUNSELING -> user.isNotifyCounseling();
        };
    }
```

`send` 본문 진입부에 `if (!isEnabled(user, type)) return;` 추가(수신자/타입 변수명은 실제 코드에 맞춤).

- [ ] **Step 8: 서비스 테스트 추가**

`NotificationServiceTest.java`(없으면 생성). `UserRepository`/`NotificationRepository`를 Mockito mock으로 주입하고, `notifyGrade=false`인 사용자에게 `GRADE` 발송 시 `notificationRepository.save`가 호출되지 않음을 검증:

```java
    @Test
    void send_disabledType_doesNotPersist() {
        User user = TestFixtures.studentUser();
        user.setNotifyGrade(false);
        // when: GRADE 알림 발송
        notificationService.send(user, Notification.Type.GRADE, "성적 입력됨");
        // then
        verify(notificationRepository, never()).save(any());
    }
```

(실제 `send` 시그니처에 맞춰 인자 조정. `TestFixtures.studentUser()`가 없으면 가장 가까운 픽스처 사용 후 `setNotifyGrade(false)`.)

- [ ] **Step 9: 테스트 실행 → 통과**

Run: `./gradlew test --tests "com.studentmanagement.controller.UserControllerTest" --tests "com.studentmanagement.service.NotificationServiceTest"`
Expected: PASS.

- [ ] **Step 10: 프론트 api 추가**

`frontend/src/api/users.js`에 추가:

```javascript
/** 내 알림 수신 설정 조회 */
export const getNotificationSettings = () =>
  client.get('/users/me/notification-settings').then((r) => r.data.data)

/** 내 알림 수신 설정 변경 */
export const updateNotificationSettings = (body) =>
  client.put('/users/me/notification-settings', body).then((r) => r.data.data)
```

- [ ] **Step 11: Settings 화면에 토글 추가**

Read: `frontend/src/pages/settings/Settings.jsx`. 알림 설정 섹션을 추가: 마운트 시 `getNotificationSettings()`로 상태 로드, 각 토글 변경 시 로컬 상태 갱신, "저장" 버튼으로 `updateNotificationSettings()` 호출 + 성공 토스트. 3개 항목: 성적/피드백/상담 알림.

핵심 스니펫:

```jsx
  const [notif, setNotif] = useState({ notifyGrade: true, notifyFeedback: true, notifyCounseling: true })
  useEffect(() => { getNotificationSettings().then(setNotif).catch(() => {}) }, [])
  const saveNotif = async () => { await updateNotificationSettings(notif); /* 토스트 */ }
  // 토글 예: <input type="checkbox" checked={notif.notifyGrade}
  //   onChange={(e) => setNotif({ ...notif, notifyGrade: e.target.checked })} />
```

- [ ] **Step 12: 빌드 확인 + 커밋**

```bash
cd frontend && npm run build && cd ..
git add src/main/java/com/studentmanagement/domain/User.java \
        src/main/java/com/studentmanagement/dto/user/NotificationSettingsRequest.java \
        src/main/java/com/studentmanagement/dto/user/NotificationSettingsResponse.java \
        src/main/java/com/studentmanagement/service/UserService.java \
        src/main/java/com/studentmanagement/controller/UserController.java \
        src/main/java/com/studentmanagement/service/NotificationService.java \
        src/test/java/com/studentmanagement/controller/UserControllerTest.java \
        src/test/java/com/studentmanagement/service/NotificationServiceTest.java \
        frontend/src/api/users.js frontend/src/pages/settings/Settings.jsx
git commit -m "[feat] 알림 수신 설정 추가 (US-06-04)"
```

---

## Task 6: 특기사항 다항목 관리 (US-03-03)

신규 1:N 엔티티 `StudentRecordNote`. 기존 단일 `specialNotes` 컬럼은 보존, 비어있지 않으면 1회 이전.

**Files:**
- Create: `src/main/java/com/studentmanagement/domain/StudentRecordNote.java`
- Create: `src/main/java/com/studentmanagement/repository/StudentRecordNoteRepository.java`
- Create: `src/main/java/com/studentmanagement/dto/record/RecordNoteRequest.java`
- Create: `src/main/java/com/studentmanagement/dto/record/RecordNoteResponse.java`
- Modify: `src/main/java/com/studentmanagement/service/StudentRecordService.java`
- Modify: `src/main/java/com/studentmanagement/controller/StudentRecordController.java`
- Test: `src/test/java/com/studentmanagement/controller/StudentRecordControllerTest.java`
- Modify(front): `frontend/src/api/records.js`, `frontend/src/pages/students/StudentDetail.jsx`

- [ ] **Step 1: 기존 학생부 코드 확인**

Read: `StudentRecord.java`(이미 파악: `specialNotes` TEXT), `StudentRecordService.java`, `StudentRecordController.java`, `StudentRecordControllerTest.java`, `frontend/src/api/records.js`. 레코드 조회/업서트 흐름과 `student_id`로 record를 찾는 방식, 응답 DTO 구조를 확인한다.

- [ ] **Step 2: 엔티티 생성**

`StudentRecordNote.java`:

```java
package com.studentmanagement.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "student_record_notes")
@Getter @Setter @NoArgsConstructor
public class StudentRecordNote {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "record_id", nullable = false)
    private StudentRecord record;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public StudentRecordNote(StudentRecord record, String content) {
        this.record = record;
        this.content = content;
    }
}
```

- [ ] **Step 3: 리포지토리 생성**

`StudentRecordNoteRepository.java`:

```java
package com.studentmanagement.repository;

import com.studentmanagement.domain.StudentRecordNote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudentRecordNoteRepository extends JpaRepository<StudentRecordNote, Long> {
    List<StudentRecordNote> findByRecordIdOrderByCreatedAtAsc(Long recordId);
}
```

- [ ] **Step 4: DTO 생성**

`RecordNoteResponse.java`:

```java
package com.studentmanagement.dto.record;

import com.studentmanagement.domain.StudentRecordNote;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
public class RecordNoteResponse {
    private final Long id;
    private final String content;
    private final LocalDateTime createdAt;

    public RecordNoteResponse(StudentRecordNote n) {
        this.id = n.getId();
        this.content = n.getContent();
        this.createdAt = n.getCreatedAt();
    }
}
```

`RecordNoteRequest.java`:

```java
package com.studentmanagement.dto.record;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class RecordNoteRequest {
    @NotBlank
    private String content;
}
```

- [ ] **Step 5: 컨트롤러 테스트 추가 (실패 확인)**

`StudentRecordControllerTest.java`에 추가(기존 패턴 따름; 서비스 메서드명은 Step 7과 일치):

```java
    @Test
    void listNotes_teacher_returns200() throws Exception {
        SecurityTestHelper.stubAsTeacher(jwtUtil);
        given(studentRecordService.listNotes(anyLong())).willReturn(java.util.List.of());

        mockMvc.perform(get("/api/students/1/record/notes").header("Authorization", FAKE_TOKEN))
                .andExpect(status().isOk());
    }

    @Test
    void addNote_teacher_returns201() throws Exception {
        SecurityTestHelper.stubAsTeacher(jwtUtil);
        given(studentRecordService.addNote(anyLong(), any()))
                .willReturn(new com.studentmanagement.dto.record.RecordNoteResponse(
                        new com.studentmanagement.domain.StudentRecordNote()));

        mockMvc.perform(post("/api/students/1/record/notes")
                .header("Authorization", FAKE_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"content":"교내 수학경시 대상"}
                    """))
                .andExpect(status().isCreated());
    }

    @Test
    void addNote_student_returns403() throws Exception {
        SecurityTestHelper.stubAsStudent(jwtUtil);

        mockMvc.perform(post("/api/students/1/record/notes")
                .header("Authorization", FAKE_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"content":"x"}
                    """))
                .andExpect(status().isForbidden());
    }
```

(`new StudentRecordNote()`는 createdAt 기본값이 있어 응답 매핑 시 NPE 없음. id/content는 null이어도 직렬화 가능.)

- [ ] **Step 6: 테스트 실행 → 실패 확인**

Run: `./gradlew test --tests "com.studentmanagement.controller.StudentRecordControllerTest"`
Expected: 컴파일 실패.

- [ ] **Step 7: 서비스 메서드 추가**

`StudentRecordService.java`에 추가(생성자에 `StudentRecordNoteRepository` 주입 추가). record 조회/생성은 기존 업서트 로직 재사용:

```java
    public List<RecordNoteResponse> listNotes(Long studentId) {
        StudentRecord record = getOrCreateRecord(studentId); // 기존 메서드명에 맞춰 조정
        return noteRepository.findByRecordIdOrderByCreatedAtAsc(record.getId())
                .stream().map(RecordNoteResponse::new).toList();
    }

    @Transactional
    public RecordNoteResponse addNote(Long studentId, RecordNoteRequest req) {
        StudentRecord record = getOrCreateRecord(studentId);
        StudentRecordNote note = noteRepository.save(new StudentRecordNote(record, req.getContent()));
        return new RecordNoteResponse(note);
    }

    @Transactional
    public RecordNoteResponse updateNote(Long noteId, RecordNoteRequest req) {
        StudentRecordNote note = noteRepository.findById(noteId)
                .orElseThrow(() -> new ResourceNotFoundException("Note not found."));
        note.setContent(req.getContent());
        return new RecordNoteResponse(note);
    }

    @Transactional
    public void deleteNote(Long noteId) {
        if (!noteRepository.existsById(noteId)) throw new ResourceNotFoundException("Note not found.");
        noteRepository.deleteById(noteId);
    }
```

기존 record 업서트 메서드명이 다르면(예: `getRecord`/`upsert`) 그에 맞춰 `getOrCreateRecord`를 대체한다. 마이그레이션: `addNote`/`listNotes` 최초 호출 시 `record.getSpecialNotes()`가 비어있지 않고 아직 note가 없으면 해당 텍스트를 첫 note로 1회 생성(헬퍼 `migrateLegacyNote(record)`로 분리).

- [ ] **Step 8: 컨트롤러 매핑 추가**

`StudentRecordController.java`에 추가:

```java
    @GetMapping("/api/students/{studentId}/record/notes")
    @PreAuthorize("hasAnyRole('TEACHER','STUDENT','PARENT')")
    public ResponseEntity<?> listNotes(@PathVariable Long studentId) {
        return ResponseEntity.ok(ApiResponse.ok(studentRecordService.listNotes(studentId)));
    }

    @PostMapping("/api/students/{studentId}/record/notes")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<?> addNote(@PathVariable Long studentId, @Valid @RequestBody RecordNoteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(studentRecordService.addNote(studentId, request)));
    }

    @PutMapping("/api/record-notes/{noteId}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<?> updateNote(@PathVariable Long noteId, @Valid @RequestBody RecordNoteRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(studentRecordService.updateNote(noteId, request)));
    }

    @DeleteMapping("/api/record-notes/{noteId}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<?> deleteNote(@PathVariable Long noteId) {
        studentRecordService.deleteNote(noteId);
        return ResponseEntity.ok(ApiResponse.ok(null, "특기사항이 삭제되었습니다."));
    }
```

- [ ] **Step 9: 테스트 실행 → 통과**

Run: `./gradlew test --tests "com.studentmanagement.controller.StudentRecordControllerTest"`
Expected: PASS.

- [ ] **Step 10: 프론트 api + UI**

`frontend/src/api/records.js`에 추가:

```javascript
export const listRecordNotes = (studentId) =>
  client.get(`/students/${studentId}/record/notes`).then((r) => r.data.data)
export const addRecordNote = (studentId, content) =>
  client.post(`/students/${studentId}/record/notes`, { content }).then((r) => r.data.data)
export const updateRecordNote = (noteId, content) =>
  client.put(`/record-notes/${noteId}`, { content }).then((r) => r.data.data)
export const deleteRecordNote = (noteId) =>
  client.delete(`/record-notes/${noteId}`).then((r) => r.data)
```

`StudentDetail.jsx` 학생부 탭: 단일 특기사항 textarea를 **항목 리스트**로 교체. 마운트/탭 진입 시 `listRecordNotes(studentId)` 로드, 각 항목 인라인 수정/삭제, 하단 "항목 추가" 입력. 교사만 편집 버튼 노출(기존 `editable`/역할 플래그 재사용).

- [ ] **Step 11: 빌드 확인 + 커밋**

```bash
cd frontend && npm run build && cd ..
git add src/main/java/com/studentmanagement/domain/StudentRecordNote.java \
        src/main/java/com/studentmanagement/repository/StudentRecordNoteRepository.java \
        src/main/java/com/studentmanagement/dto/record/RecordNoteRequest.java \
        src/main/java/com/studentmanagement/dto/record/RecordNoteResponse.java \
        src/main/java/com/studentmanagement/service/StudentRecordService.java \
        src/main/java/com/studentmanagement/controller/StudentRecordController.java \
        src/test/java/com/studentmanagement/controller/StudentRecordControllerTest.java \
        frontend/src/api/records.js frontend/src/pages/students/StudentDetail.jsx
git commit -m "[feat] 특기사항 다항목 관리 추가 (US-03-03)"
```

---

## Task 7: 상담 특정 교사 공유범위 (US-05-04)

`ShareScope`에 `SELECTED` 추가 + 상담-대상교사 M:N. `getAll`/`getById` 공유범위 접근제어.

**Files:**
- Modify: `src/main/java/com/studentmanagement/domain/Counseling.java`
- Modify: `src/main/java/com/studentmanagement/dto/counseling/CounselingRequest.java`
- Modify: `src/main/java/com/studentmanagement/dto/counseling/`(응답 DTO — 확인 후)
- Modify: `src/main/java/com/studentmanagement/service/CounselingService.java`
- Test: `src/test/java/com/studentmanagement/service/CounselingServiceTest.java`
- Modify(front): `frontend/src/api/counselings.js`, `frontend/src/pages/counseling/CounselingManagement.jsx`

- [ ] **Step 1: 기존 상담 코드 확인**

Read: `CounselingService.java`(getAll/getById/create/update, 작성자 검증 방식), `CounselingRequest.java`, 상담 응답 DTO. 현재 `getAll`이 모든 교사에게 전부 반환하는지 확인.

- [ ] **Step 2: enum + M:N 필드 추가**

`Counseling.java`:

```java
    public enum ShareScope { ALL, PRIVATE, SELECTED }
```

대상 교사 집합 추가:

```java
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "counseling_shared_teachers",
        joinColumns = @JoinColumn(name = "counseling_id"),
        inverseJoinColumns = @JoinColumn(name = "teacher_id"))
    private java.util.Set<User> sharedTeachers = new java.util.HashSet<>();
```

- [ ] **Step 3: 요청 DTO 확장**

`CounselingRequest.java`에 추가:

```java
    private java.util.List<Long> sharedTeacherIds = new java.util.ArrayList<>();
```

(getter 확인 — Lombok `@Getter`면 자동.)

- [ ] **Step 4: 서비스 테스트 추가 (실패 확인)**

`CounselingServiceTest.java`에 접근제어 테스트 추가. `SELECTED` 상담은 대상 교사만, 비대상 교사는 못 봄:

```java
    @Test
    void getAll_selectedScope_visibleOnlyToSharedTeacher() {
        // given: teacherA 작성, SELECTED, 대상 = teacherB
        // when: teacherC가 조회
        // then: 결과에 해당 상담이 없음 / teacherB 조회 시 있음
    }
```

(실제 Mockito 스텁은 repository mock 반환을 구성하고 서비스 필터 로직을 검증하도록 작성. 작성자 본인·ALL은 보이고 PRIVATE/비대상 SELECTED는 제외.)

- [ ] **Step 5: 서비스 접근제어 + 동기화 구현**

`CounselingService.java`:
- `create`/`update`: `request.getShareScope()`가 `SELECTED`면 `userRepository.findAllById(sharedTeacherIds)`로 조회해 `counseling.setSharedTeachers(...)`. 아니면 비움.
- `getAll`/`getById`: 조회자 email→User 조회 후, 다음 조건만 노출하도록 필터:
  - 작성자 본인(teacher.email == 조회자), 또는 `ALL`, 또는 (`SELECTED` ∧ `sharedTeachers`에 조회자 포함). `PRIVATE`는 작성자만.
- 필터는 repository 결과를 스트림으로 거르거나, 조회 쿼리에 반영(단순화를 위해 서비스단 필터 허용).

- [ ] **Step 6: 테스트 실행 → 통과**

Run: `./gradlew test --tests "com.studentmanagement.service.CounselingServiceTest"`
Expected: PASS.

- [ ] **Step 7: 응답 DTO에 공유 정보 노출(선택)**

상담 응답 DTO에 `shareScope`(이미 있을 가능성)와 `sharedTeacherIds`/`sharedTeacherNames`를 추가해 프론트 수정 모달이 기존 선택을 복원할 수 있게 한다.

- [ ] **Step 8: 프론트 — 작성/수정 모달 공유범위 3단계**

`CounselingManagement.jsx` 상담 작성/수정 모달:
- 공개범위 라디오 3개: 전체공개(`ALL`) / 특정 교사(`SELECTED`) / 비공개(`PRIVATE`).
- `SELECTED` 선택 시 교사 멀티셀렉트(`teachers` 상태 재사용; Task 3에서 이미 로드). 선택된 id 배열을 `sharedTeacherIds`로 전송.
- `api/counselings.js`의 create/update가 `sharedTeacherIds`를 body에 포함하도록 확인.

- [ ] **Step 9: 빌드 + 전체 테스트 + 커밋**

```bash
cd frontend && npm run build && cd ..
./gradlew test
git add src/main/java/com/studentmanagement/domain/Counseling.java \
        src/main/java/com/studentmanagement/dto/counseling/ \
        src/main/java/com/studentmanagement/service/CounselingService.java \
        src/test/java/com/studentmanagement/service/CounselingServiceTest.java \
        frontend/src/api/counselings.js frontend/src/pages/counseling/CounselingManagement.jsx
git commit -m "[feat] 상담 특정 교사 공유범위(SELECTED) 추가 (US-05-04)"
```

---

## Self-Review (작성자 점검 결과)

**Spec coverage:** 공용(teachers)=Task1, US-01-05=Task2, US-05-03=Task3, G-HIGH-1=Task4, US-06-04=Task5, US-03-03=Task6, US-05-04=Task7. 6+1 전부 매핑됨.

**Placeholder 주의:** 일부 백엔드 통합 지점(기존 메서드명: `getOrCreateRecord`, `NotificationService.send` 시그니처, 상담 응답 DTO명, `GradeResponse` 생성자)은 **실행 시 해당 파일을 Read해 실제 이름에 맞춰 조정**하라고 명시했다. 이는 cc3bbf2 시점 코드를 일부 미확인했기 때문이며, 각 Task Step 1에서 확인하도록 강제한다.

**Type consistency:** 컨트롤러 테스트의 모킹 메서드명이 서비스 메서드명과 일치(`getTeachers`, `getGradesByClass`, `getNotificationSettings`/`updateNotificationSettings`, `listNotes`/`addNote`/`updateNote`/`deleteNote`). boolean 게터는 Lombok 규칙(`isNotifyGrade`)으로 통일.

**리스크:** ddl-auto update로 컬럼/테이블 자동 추가. 특기사항 레거시 이전은 서비스 로직으로 1회 처리. 상담 접근제어 변경은 기존 동작을 바꾸므로 회귀 테스트로 보호.

---

## Execution Handoff

계획 저장 위치: `docs/superpowers/plans/2026-06-02-feature-completion.md`

실행 시 각 Task의 Step 1(기존 코드 Read)을 반드시 먼저 수행해 메서드명/DTO를 실제 코드에 맞춘 뒤 진행한다.
