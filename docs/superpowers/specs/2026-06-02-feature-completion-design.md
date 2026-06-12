# 기능 보완 설계 — BACKLOG 미구현 스토리 완성

- 작성일: 2026-06-02
- 대상 브랜치: `feature/feature-completion` (base: 로컬 `main` = cc3bbf2)
- 목표: 코드 기준 감사에서 발견된 BACKLOG 미구현/부분구현 스토리 6건을 완성한다.
- 범위 밖(YAGNI): 에러 무음 처리(`catch {}`) 개선, `alert()` → toast 전환, 목록 페이지네이션. (품질 개선 항목으로 별도 관리)

## 배경

코드 기준(파일 직접 확인) 감사 결과, 다음 6개 갭이 확인되었다. BACKLOG의 '상태' 컬럼은 stale이므로 코드를 기준으로 판단했다.

| ID | User Story | 현 상태(코드 확인) | 분류 |
|----|-----------|-------------------|------|
| US-01-05 | 비밀번호 재설정 | 백엔드 `POST /api/auth/reset-password` 완비, 프론트 UI 없음 | 프론트 전용 |
| US-05-03 | 상담 교사별/날짜 필터 | 백엔드 `getAll(studentId, teacherId, from, to)` 완비, 프론트 UI 없음 | 프론트 전용 |
| G-HIGH-1 | 성적 관리 화면 기존 성적 미표시 | 반/과목 단위 조회 API 부재 → `getGrades(null)`이 빈값 반환 | 버그(백엔드 소폭+프론트) |
| US-06-04 | 알림 수신 설정 | User/Notification에 설정 필드 전무 | 도메인+백엔드+프론트 |
| US-03-03 | 특기사항 다항목 관리 | 단일 `specialNotes` TEXT 필드만 존재 | 스키마+백엔드+프론트 |
| US-05-04 | 상담 "특정 교사" 공유범위 | `ShareScope` = `ALL`/`PRIVATE` 2단계만 존재 | 도메인(M:N)+백엔드+프론트 |

## 공용 인프라 (선행)

### 교사 목록 조회 엔드포인트
- **신규**: `GET /api/users/teachers` — `@PreAuthorize("hasAnyRole('TEACHER','ADMIN')")`
- 반환: 교사(`Role.TEACHER`)의 `{ id, name }` 목록만 (이메일 등 민감정보 제외)
- 이유: 현재 `GET /api/users`는 ADMIN 전용이라 교사가 교사 목록을 얻을 수 없다. US-05-03(교사 필터)와 US-05-04(대상 교사 선택)가 공유한다.
- 구성: `UserController.getTeachers()` → `UserService.getTeachers()` → `UserRepository.findByRole(Role.TEACHER)`. 응답 DTO `TeacherOptionResponse(id, name)`.
- 프론트: `api/users.js`에 `getTeachers()` 추가.

## 기능별 설계

### 그룹 A — 프론트 전용

#### 1. 비밀번호 재설정 UI (US-01-05)
- `api/auth.js`에 `resetPassword(email)` → `POST /api/auth/reset-password { email }`.
- `Login.jsx`: 비밀번호 입력 아래 "비밀번호를 잊으셨나요?" 링크 → 이메일 입력 모달.
- 제출 시 호출 후 성공 토스트("임시 비밀번호를 이메일로 발송했습니다"). 404(미등록 이메일)도 동일 메시지로 처리해 계정 존재 여부 노출을 피한다.
- 백엔드 변경 없음.

#### 2. 상담 교사별/날짜 필터 (US-05-03)
- `api/counselings.js`의 목록 조회에 `teacherId, from, to` 파라미터 전달.
- `CounselingManagement.jsx` 필터바에 추가: 교사 select(`getTeachers()`), 기간 from/to date input. 기존 학생 이름 검색은 유지.
- 서버 필터(teacherId/from/to)와 클라이언트 학생 이름 검색을 조합.
- 백엔드 변경 없음(이미 지원).

### 그룹 B — 버그 수정

#### 3. 성적 관리 반/과목 일괄 조회 (G-HIGH-1)
- 근본 원인: 반·과목 단위로 여러 학생 성적을 한 번에 조회하는 API가 없어, `GradeManagement`가 `getGrades(null, ...)`로 `/students/null/grades`를 호출 → 백엔드 `Long` 파싱 실패 → `.catch(()=>[])`로 삼켜져 **기존 성적이 항상 미입력으로 표시**된다.
- **신규**: `GET /api/grades?grade=&classNum=&subjectId=&year=&semester=` — `@PreAuthorize("hasRole('TEACHER')")`
  - 반환: 조건에 맞는 성적 목록(학생 id 포함). `getStats` 쿼리 패턴 재사용.
  - `GradeRepository`에 JPQL 추가, `GradeService.getGradesByClass(...)`, `GradeController`에 매핑.
- 프론트: `api/grades.js`에 `getGradesByClass(params)` 추가, `GradeManagement.loadData`에서 `getGrades(null, ...)` → `getGradesByClass(...)`로 교체.

### 그룹 C — 도메인/스키마 변경

#### 4. 알림 수신 설정 (US-06-04)
- 도메인: `User` 엔티티에 boolean 3개 추가 — `notifyGrade`, `notifyFeedback`, `notifyCounseling` (기본값 `true`). 알림 타입이 3종으로 고정적이라 별도 1:1 엔티티는 과설계로 판단.
  - `ddl-auto: update`로 컬럼 자동 추가. 기존 행이 NULL이 되지 않도록 `@Column(nullable = false, columnDefinition = "boolean default true")`로 DB 기본값을 명시한다.
- 발송 가드: `NotificationService.send()`에서 수신자의 해당 타입 설정이 false면 생성/발송하지 않는다.
- API:
  - `GET /api/users/me/notification-settings` → 현재 사용자 3개 설정 반환
  - `PUT /api/users/me/notification-settings { notifyGrade, notifyFeedback, notifyCounseling }`
- 프론트: `Settings.jsx`에 토글 3개 + 저장. `api/users.js`에 get/update 추가.

#### 5. 특기사항 다항목 관리 (US-03-03)
- 도메인: **신규 1:N 엔티티 `StudentRecordNote`** (`id`, `record_id`(FK→student_records), `content` TEXT, `createdAt`). "항목 추가·수정·삭제" 요구에 자연스럽다. (JSON 배열案은 항목별 조작이 어색해 제외)
- 마이그레이션: 기존 단일 `special_notes` 컬럼은 **보존**(파괴적 변경 회피). 신규 notes 테이블과 병행하며, 기존 비어있지 않은 `special_notes` 값은 1회성으로 첫 note로 이전(시딩/마이그레이션 로직). 응답에서는 notes 배열을 정본으로 사용.
- API (학생부 하위 리소스):
  - `GET /api/students/{studentId}/record/notes`
  - `POST /api/students/{studentId}/record/notes { content }` (TEACHER)
  - `PUT /api/record-notes/{noteId} { content }` (TEACHER)
  - `DELETE /api/record-notes/{noteId}` (TEACHER)
- 프론트: `StudentDetail` 학생부 탭에 특기사항 항목 리스트(추가/수정/삭제) UI. 교사만 편집, 학생/학부모는 읽기 전용.

#### 6. 상담 "특정 교사" 공유범위 (US-05-04)
- 도메인:
  - `Counseling.ShareScope`에 **`SELECTED` 추가** (`ALL`, `PRIVATE`, `SELECTED`).
  - 조인 테이블 `counseling_shared_teachers` (`counseling_id`, `teacher_id`) — `Counseling`에 `@ManyToMany` 대상 교사 집합.
- 접근 제어: `CounselingService.getAll`/`getById`에 공유범위 기반 필터를 추가한다. 조회 교사가 다음 중 하나일 때만 노출:
  - 작성자 본인, 또는 `ALL`, 또는 (`SELECTED` ∧ 대상 교사 집합에 포함).
  - (현재 모든 교사가 PRIVATE 포함 전부 열람하는 동작을 공유범위를 존중하도록 수정한다.)
- API: `CounselingRequest`에 `shareScope`(확장) + `sharedTeacherIds: Long[]`(SELECTED일 때) 추가. 생성/수정 시 대상 교사 동기화.
- 프론트: 상담 작성/수정 모달의 공개범위를 라디오 3종(전체공개/특정 교사/비공개)으로 확장. "특정 교사" 선택 시 교사 멀티셀렉트(`getTeachers()`) 노출.

## 테스트 전략 (TDD)

기존 패턴을 따른다.
- **Controller**: `@WebMvcTest` + `@TestPropertySource` + `SecurityTestHelper`(역할 스텁). 신규 엔드포인트별 권한/정상/예외 케이스.
- **Service**: 클래스 레벨 `@Transactional(readOnly=true)` 패턴 유지. `TestFixtures`로 엔티티 생성.
- 핵심 신규 테스트:
  - `GET /api/users/teachers` 권한(TEACHER/ADMIN 허용, STUDENT/PARENT 거부)
  - 성적 반/과목 일괄 조회 필터 정확성
  - 알림 설정 false 시 `NotificationService.send()`가 생성하지 않음
  - 상담 `SELECTED` 접근제어(대상 교사만 열람, 비대상 교사 차단)
  - 특기사항 note CRUD + 기존 `special_notes` 이전

## 구현 순서 / 커밋 단위

1. 공용: `GET /api/users/teachers` (+테스트)
2. A-1 비밀번호 재설정 UI
3. A-2 상담 교사/날짜 필터
4. B-3 성적 반/과목 일괄 조회(+버그 수정)
5. C-4 알림 수신 설정
6. C-5 특기사항 다항목
7. C-6 상담 특정 교사 공유범위

각 항목 독립 커밋(`[feat]`/`[fix]`), 백엔드 변경은 `./gradlew test` 통과 확인 후 커밋.

## 검증 한계 / 리스크

- `ddl-auto: update`는 컬럼·테이블 추가는 자동 반영하나 데이터 마이그레이션(특기사항 이전)은 애플리케이션 로직/시딩으로 처리해야 한다.
- 상담 접근제어 변경은 기존 동작(모든 교사 열람)을 바꾸므로, 기존 데이터의 `shareScope` 기본값(`ALL`) 영향 확인 필요.
- 로컬에 MySQL/Docker 미가동 시 통합 동작은 단위 테스트로만 검증되며, 실제 DB 마이그레이션은 실행 환경에서 확인 필요.
