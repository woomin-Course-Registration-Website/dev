# UI 전면 개편 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** "따뜻하고 친근한" 디자인 시스템으로 전 페이지를 정밀 개편한다 (틸-그린 브랜드 + 웜 스톤 뉴트럴 + 앰버/코랄 액센트).

**Architecture:** 2단계. (1) 토큰·컴포넌트 레이어(`tailwind.config.js`, `index.css`) 재정의 — `primary`→브랜드 틸, `gray`→웜 스톤으로 **전역 리매핑**하고 공용 클래스(`.card/.btn/.badge/.input/.table`)를 재작성하면, 마크업을 건드리지 않아도 모든 페이지가 새 룩으로 cascade된다. (2) 그 위에서 레이아웃과 11개 페이지를 페이지별로 정밀 개편한다.

**Tech Stack:** React 18, Vite, Tailwind CSS 3.4.7, clsx, Recharts. UI 작업이므로 단위 테스트 대신 `npm run build`(빌드 무결성)로 검증하고, 시각 품질은 dev 서버 수동 점검으로 최종 검수한다.

**작업 위치:** worktree `feature/ui-redesign` (base `feature/feature-completion`). 모든 경로는 이 worktree의 `frontend/` 기준. 빌드: `npm run build --prefix frontend`.

**공통 원칙:**
- 색은 직접 hex 대신 토큰 클래스(`brand-*`, `accent-*`, `bg`, `surface`, 그리고 stone으로 리매핑된 `gray-*`)를 사용.
- 마크업 구조/데이터 흐름/이벤트 핸들러는 불변. 클래스명(`.card`, `.btn-primary` 등)은 유지하고 정의만 교체.
- 각 Task 끝에 `npm run build` 통과 확인 후 커밋. 커밋 메시지 본문 끝에 `Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>`.

---

## Task 1: 디자인 토큰 + 공용 컴포넌트 레이어 (키스톤)

전 페이지 cascade의 기반. `primary`→브랜드 틸, `gray`→웜 스톤 전역 리매핑 + 공용 클래스 재작성.

**Files:**
- Modify(전체 교체): `frontend/tailwind.config.js`
- Modify(전체 교체): `frontend/src/index.css`

- [ ] **Step 1: `tailwind.config.js`를 아래 내용으로 전체 교체**

```javascript
import colors from 'tailwindcss/colors'

/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,jsx}'],
  theme: {
    extend: {
      colors: {
        // 웜 스톤으로 전역 뉴트럴 리매핑 (기존 gray-* 사용처가 모두 따뜻해짐)
        gray: colors.stone,
        // 브랜드(틸-그린) — CSS 변수 참조로 향후 다크 확장 대비
        brand: {
          50:  'rgb(var(--brand-50) / <alpha-value>)',
          100: 'rgb(var(--brand-100) / <alpha-value>)',
          200: 'rgb(var(--brand-200) / <alpha-value>)',
          500: 'rgb(var(--brand-500) / <alpha-value>)',
          600: 'rgb(var(--brand-600) / <alpha-value>)',
          700: 'rgb(var(--brand-700) / <alpha-value>)',
          800: 'rgb(var(--brand-800) / <alpha-value>)',
          900: 'rgb(var(--brand-900) / <alpha-value>)',
        },
        // primary는 brand의 별칭 — 기존 primary-* 클래스가 즉시 틸로 동작
        primary: {
          50:  'rgb(var(--brand-50) / <alpha-value>)',
          100: 'rgb(var(--brand-100) / <alpha-value>)',
          200: 'rgb(var(--brand-200) / <alpha-value>)',
          500: 'rgb(var(--brand-500) / <alpha-value>)',
          600: 'rgb(var(--brand-600) / <alpha-value>)',
          700: 'rgb(var(--brand-700) / <alpha-value>)',
          800: 'rgb(var(--brand-800) / <alpha-value>)',
          900: 'rgb(var(--brand-900) / <alpha-value>)',
        },
        accent: {
          amber: 'rgb(var(--accent-amber) / <alpha-value>)',
          coral: 'rgb(var(--accent-coral) / <alpha-value>)',
        },
        surface: 'rgb(var(--surface) / <alpha-value>)',
        bg:      'rgb(var(--bg) / <alpha-value>)',
      },
      fontFamily: {
        sans: ['Pretendard', 'Inter', 'system-ui', 'sans-serif'],
        mono: ['JetBrains Mono', 'Menlo', 'monospace'],
      },
      boxShadow: {
        soft:    '0 1px 2px rgba(41,37,36,0.04), 0 4px 16px rgba(41,37,36,0.06)',
        'soft-lg':'0 8px 30px rgba(41,37,36,0.10)',
        card:    '0 1px 2px rgba(41,37,36,0.04), 0 4px 16px rgba(41,37,36,0.06)',
        modal:   '0 24px 60px rgba(41,37,36,0.18)',
      },
      borderRadius: {
        '2xl': '1rem',
        '3xl': '1.5rem',
      },
      animation: {
        'fade-in': 'fadeIn 0.2s ease-out',
        'slide-up': 'slideUp 0.25s ease-out',
        shake: 'shake 0.3s ease-in-out',
        pop: 'pop 0.18s ease-out',
      },
      keyframes: {
        fadeIn: { from: { opacity: 0 }, to: { opacity: 1 } },
        slideUp: { from: { opacity: 0, transform: 'translateY(8px)' }, to: { opacity: 1, transform: 'translateY(0)' } },
        shake: { '0%,100%': { transform: 'translateX(0)' }, '25%': { transform: 'translateX(-4px)' }, '75%': { transform: 'translateX(4px)' } },
        pop: { from: { transform: 'scale(0.96)', opacity: 0 }, to: { transform: 'scale(1)', opacity: 1 } },
      },
    },
  },
  plugins: [],
}
```

- [ ] **Step 2: `src/index.css`를 아래 내용으로 전체 교체**

```css
@tailwind base;
@tailwind components;
@tailwind utilities;

@layer base {
  :root {
    /* 브랜드 (틸-그린) — RGB 트리플릿 */
    --brand-50: 240 253 250;
    --brand-100: 204 251 241;
    --brand-200: 153 246 224;
    --brand-500: 20 184 166;
    --brand-600: 13 148 136;
    --brand-700: 15 118 110;
    --brand-800: 17 94 89;
    --brand-900: 19 78 74;
    /* 액센트 (따뜻한) */
    --accent-amber: 245 158 11;
    --accent-coral: 251 113 133;
    /* 서피스 / 배경 (웜 크림) */
    --surface: 255 255 255;
    --bg: 250 249 247;
  }

  * { box-sizing: border-box; }
  body {
    @apply bg-bg text-gray-800 font-sans antialiased;
  }
  /* 숫자 정렬 안정화 (성적/통계) */
  .tabular { font-variant-numeric: tabular-nums; }
  :focus-visible {
    @apply outline-none ring-2 ring-brand-500 ring-offset-2 ring-offset-bg;
  }
}

@layer components {
  .btn {
    @apply inline-flex items-center justify-center gap-2 rounded-lg font-medium transition-all duration-150
           disabled:opacity-50 disabled:cursor-not-allowed
           focus-visible:ring-2 focus-visible:ring-brand-500 focus-visible:ring-offset-2;
  }
  .btn-sm { @apply btn h-8 px-3 text-sm; }
  .btn-md { @apply btn h-10 px-4 text-sm; }
  .btn-lg { @apply btn h-12 px-6 text-base; }

  .btn-primary   { @apply bg-brand-600 text-white shadow-soft hover:bg-brand-700 hover:-translate-y-0.5 active:translate-y-0 active:bg-brand-800; }
  .btn-soft      { @apply bg-brand-50 text-brand-700 hover:bg-brand-100; }
  .btn-secondary { @apply bg-surface text-gray-700 border border-gray-200 hover:bg-gray-50 active:bg-gray-100; }
  .btn-danger    { @apply bg-red-500 text-white shadow-soft hover:bg-red-600 active:bg-red-700; }
  .btn-ghost     { @apply text-brand-700 hover:bg-brand-50 active:bg-brand-100; }

  .input {
    @apply w-full rounded-lg border border-gray-300 bg-surface px-3.5 py-2 text-sm text-gray-900 placeholder-gray-400
           focus:border-brand-500 focus:outline-none focus:ring-2 focus:ring-brand-100
           disabled:bg-gray-100 disabled:cursor-not-allowed transition-colors duration-150;
  }
  .input-error { @apply input border-red-400 focus:border-red-500 focus:ring-red-100; }

  .card {
    @apply bg-surface rounded-2xl border border-gray-200/70 shadow-soft;
  }
  .card-hover { @apply card transition-shadow duration-200 hover:shadow-soft-lg; }

  .badge {
    @apply inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium;
  }
  .badge-brand  { @apply badge bg-brand-100 text-brand-800; }
  .badge-green  { @apply badge bg-green-100 text-green-800; }
  .badge-blue   { @apply badge bg-brand-100 text-brand-800; }
  .badge-amber  { @apply badge bg-amber-100 text-amber-800; }
  .badge-coral  { @apply badge; background-color: rgb(254 226 226); color: rgb(159 18 57); }
  .badge-red    { @apply badge bg-red-100 text-red-800; }
  .badge-gray   { @apply badge bg-gray-100 text-gray-600; }
  .badge-purple { @apply badge bg-purple-100 text-purple-800; }
  .badge-orange { @apply badge bg-orange-100 text-orange-800; }

  .table-header { @apply bg-gray-50 text-xs font-semibold text-gray-500 uppercase tracking-wider; }
  .table-row    { @apply border-b border-gray-100 hover:bg-brand-50/60 transition-colors duration-100; }
  .table-cell   { @apply px-4 py-3 text-sm text-gray-700; }
}

/* 스크롤바 (웜) */
::-webkit-scrollbar { width: 8px; height: 8px; }
::-webkit-scrollbar-track { @apply bg-transparent; }
::-webkit-scrollbar-thumb { @apply bg-gray-300 rounded-full; }
::-webkit-scrollbar-thumb:hover { @apply bg-gray-400; }
```

- [ ] **Step 3: 빌드 검증**

Run: `npm run build --prefix frontend`
Expected: 빌드 성공. (기존 `primary-*`/`gray-*` 클래스가 새 토큰으로 해석되어 전 페이지가 틸+웜 톤으로 변함. `bg-primary-900`(사이드바)은 이 시점엔 다크 틸 — Task 2에서 라이트로 교체.)

- [ ] **Step 4: 커밋**

```bash
git add frontend/tailwind.config.js frontend/src/index.css
git commit -m "[feat] 디자인 토큰·공용 컴포넌트 레이어 재정의 (틸 브랜드 + 웜 스톤)"
```

---

## Task 2: 레이아웃 — 라이트 사이드바 + 웜 헤더

제네릭 요인 #4(다크 네이비 사이드바) 직접 해결. 다크→라이트 크림 사이드바 + 브랜드 액티브 필.

**Files:**
- Modify: `frontend/src/components/common/Sidebar.jsx`
- Modify: `frontend/src/components/common/Header.jsx`
- Modify: `frontend/src/components/common/Layout.jsx`

- [ ] **Step 1: 파일 3개 정독**

Read 세 파일. Sidebar는 현재 `bg-primary-900 text-white`(다크), 로고 박스 `bg-primary-500`, NavLink 액티브 `bg-primary-700`. Header는 `bg-white border-b`. Layout은 `bg-gray-50`.

- [ ] **Step 2: Sidebar를 라이트 테마로 변경**

다음 클래스 치환을 적용 (마크업 구조·nav 항목·아이콘 SVG는 유지):
- 루트 `<aside>`: `bg-primary-900 text-white` → `bg-surface border-r border-gray-200 text-gray-700`
- 로고 박스: `bg-primary-500` 유지(=brand-500) 또는 `bg-brand-600 text-white`로; 브랜드명 텍스트 `text-white` → `text-gray-900`
- 로고 영역 구분선 `border-white/10` → `border-gray-200`
- NavLink className(active/inactive):
```jsx
className={({ isActive }) => clsx(
  'flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm font-medium transition-colors',
  isActive
    ? 'bg-brand-50 text-brand-700 font-semibold'
    : 'text-gray-500 hover:bg-gray-100 hover:text-gray-800'
)}
```
- 기타 `text-primary-100`, `hover:bg-white/8` 등 다크 전제 클래스를 위 라이트 팔레트로 교체. 하단 로그아웃/유저 영역도 동일 원칙(`text-gray-*`, `hover:bg-gray-100`).

- [ ] **Step 3: Header를 웜 처리**

- 루트: `bg-white border-b border-gray-200` → `bg-surface/80 backdrop-blur-sm border-b border-gray-200`
- 햄버거/아이콘 버튼: `hover:bg-gray-100 text-gray-500` 유지(이미 토큰).
- 프로필 아바타: `bg-primary-700` → `bg-brand-600`.
- 알림 드롭다운 카드: `.card` 클래스 사용 또는 `bg-surface rounded-2xl shadow-soft border border-gray-200`로.

- [ ] **Step 4: Layout 배경**

- 루트 컨테이너 `bg-gray-50` → `bg-bg`. 메인 `<main>` 패딩 `p-6` → `p-6 lg:p-8`로 여백 확대.

- [ ] **Step 5: 빌드 + 커밋**

```bash
npm run build --prefix frontend
git add frontend/src/components/common/
git commit -m "[feat] 레이아웃 라이트 사이드바·웜 헤더로 개편"
```

---

## Task 3: 인증 화면 — Login / Register

**Files:**
- Modify: `frontend/src/pages/Login.jsx`
- Modify: `frontend/src/pages/Register.jsx`

- [ ] **Step 1: 두 파일 정독.** 현재 Login은 `bg-gradient-to-br from-primary-900 via-primary-800 to-primary-700` 풀스크린 그라데이션 + 흰 카드.

- [ ] **Step 2: 웜 split/그라데이션으로 변경**

- 배경 그라데이션을 브랜드 웜 톤으로: `from-brand-700 via-brand-600 to-brand-500`(=틸) 유지하되, 장식 원(`bg-white/5`)에 `accent-amber/10` 같은 따뜻한 포인트 추가.
- 로고 박스/타이틀: `text-white` 유지(다크 배경 위), 카드 `bg-white rounded-2xl shadow-modal` → `rounded-3xl shadow-soft-lg`로 더 부드럽게.
- 입력/버튼은 공용 `.input`/`.btn-primary`가 자동 적용됨 — 하드코딩된 색 있으면 토큰으로 교체.
- 비밀번호 재설정 모달(이미 존재): 카드 라운드/그림자 통일.
- Register도 동일 톤으로 맞춤.

- [ ] **Step 3: 빌드 + 커밋**

```bash
npm run build --prefix frontend
git add frontend/src/pages/Login.jsx frontend/src/pages/Register.jsx
git commit -m "[feat] 로그인·회원가입 화면 웜 톤 개편"
```

---

## Task 4: Dashboard

**Files:**
- Modify: `frontend/src/pages/Dashboard.jsx`

- [ ] **Step 1: 정독.** KPI 카드는 `color` prop으로 `bg-blue-50 text-blue-700` 등 인라인 주입, 차트는 Recharts.

- [ ] **Step 2: 정밀 개편**

- 상단 환영 헤더 추가(사용자 이름 + 날짜): `<h1 className="text-2xl font-bold text-gray-900">` + 서브텍스트 `text-gray-500`.
- KPI 카드: `.card p-5` 유지, 아이콘 박스 색을 브랜드/액센트 틴트로 통일(`bg-brand-50 text-brand-700`, `bg-amber-50 text-amber-600`, `bg-coral`(rgb 254 226 226)/`text-rose-600` 등). 숫자에 `tabular` 클래스 추가, `text-2xl`→`text-3xl font-bold`.
- 차트(Recharts) 색: `stroke`/`fill`을 브랜드 틸(`#0d9488`, `#14b8a6`)·앰버(`#f59e0b`)로 교체. PolarGrid/축 `stroke="#e7e5e4"`(stone-200).
- 상담 일정/알림 리스트: 행 호버 `hover:bg-brand-50/60`, 미읽음 알림 점 `bg-accent-coral`.

- [ ] **Step 3: 빌드 + 커밋**

```bash
npm run build --prefix frontend
git add frontend/src/pages/Dashboard.jsx
git commit -m "[feat] 대시보드 정밀 개편 (KPI·차트 웜 팔레트)"
```

---

## Task 5: 학생 화면 — StudentList / StudentDetail

**Files:**
- Modify: `frontend/src/pages/students/StudentList.jsx`
- Modify: `frontend/src/pages/students/StudentDetail.jsx`

- [ ] **Step 1: 두 파일 정독.**

- [ ] **Step 2: 정밀 개편**

- StudentList: 필터바를 `.card p-4`로 감싸 정리, 검색 input `.input`, 학생 행 호버 `hover:bg-brand-50/60`, 빈 상태 메시지에 친근한 일러스트적 아이콘 + `text-gray-400`.
- StudentDetail: 탭 바를 브랜드 언더라인/필 스타일로(`데이터 활성 탭` `text-brand-700 border-b-2 border-brand-600`, 비활성 `text-gray-500`). 레이더 차트 `stroke="#0f766e" fill="#14b8a6" fillOpacity={0.2}`, PolarGrid `stroke="#e7e5e4"`. 출결 카드 색(`text-green-600`/`text-red-500`/`text-amber-600`) 유지하되 배경 `bg-gray-50` → `bg-bg` 또는 brand 틴트. 특기사항/피드백/상담 카드 `.card` 톤 통일.

- [ ] **Step 3: 빌드 + 커밋**

```bash
npm run build --prefix frontend
git add frontend/src/pages/students/
git commit -m "[feat] 학생 목록·상세 화면 정밀 개편"
```

---

## Task 6: 성적 / 피드백 — GradeManagement / FeedbackManagement

**Files:**
- Modify: `frontend/src/pages/grades/GradeManagement.jsx`
- Modify: `frontend/src/pages/feedback/FeedbackManagement.jsx`

- [ ] **Step 1: 두 파일 정독.**

- [ ] **Step 2: 정밀 개편**

- GradeManagement: 필터바 `.card`, 입력 그리드 행 호버/zebra(`even:bg-gray-50/50`), 점수 입력 `.input` 정리, 저장 배지 `bg-white/25` → `bg-brand-700/20`, 반 통계 카드 숫자 `tabular`.
- FeedbackManagement: 피드백 카드(`FeedbackCard`) 아바타 `bg-primary-100 text-primary-700`(=brand) 유지, 카테고리 배지를 웜 세트(`badge-brand`/`badge-amber`/`badge-coral`/`badge-purple`)로 매핑, 공개 토글 `badge-green`/`badge-gray`. 역할별 뷰 헤더 정리.

- [ ] **Step 3: 빌드 + 커밋**

```bash
npm run build --prefix frontend
git add frontend/src/pages/grades/ frontend/src/pages/feedback/
git commit -m "[feat] 성적·피드백 화면 정밀 개편"
```

---

## Task 7: 상담 / 보고서 — CounselingManagement / Reports

**Files:**
- Modify: `frontend/src/pages/counseling/CounselingManagement.jsx`
- Modify: `frontend/src/pages/reports/Reports.jsx`

- [ ] **Step 1: 두 파일 정독.**

- [ ] **Step 2: 정밀 개편**

- CounselingManagement: 목록/달력 토글 버튼 브랜드 톤, 달력 셀 활성 `border-primary-200 bg-primary-50`(=brand) 유지, 이벤트 칩 `bg-primary-600`→`bg-brand-600`. 작성/수정 모달 `.card`/라운드 통일, 공유범위 라디오·교사 멀티셀렉트(이미 존재) 간격·웜톤 정리.
- Reports: 보고서 타입 선택을 카드형 선택지로(선택 시 `border-brand-500 bg-brand-50 ring-2 ring-brand-100`), 미리보기 테이블 `.table-*`, 다운로드 버튼 `.btn-primary`/`.btn-soft`. `alert()` 그대로 두되(범위 밖) 가능하면 인라인 메시지로.

- [ ] **Step 3: 빌드 + 커밋**

```bash
npm run build --prefix frontend
git add frontend/src/pages/counseling/ frontend/src/pages/reports/
git commit -m "[feat] 상담·보고서 화면 정밀 개편"
```

---

## Task 8: 설정 / 관리자 — Settings / AdminUsers

**Files:**
- Modify: `frontend/src/pages/settings/Settings.jsx`
- Modify: `frontend/src/pages/admin/AdminUsers.jsx`

- [ ] **Step 1: 두 파일 정독.**

- [ ] **Step 2: 정밀 개편**

- Settings: 탭(프로필/비밀번호/알림) 브랜드 언더라인, 알림 토글 체크박스를 **스위치 스타일**로(또는 `accent-brand-600` 유지 + 카드 정리), 저장 메시지 `text-green-600`/`text-red-500`.
- AdminUsers: 사용자 테이블 `.table-*`, 역할 배지를 역할별 색 매핑(TEACHER `badge-brand`, STUDENT `badge-blue`, PARENT `badge-amber`, ADMIN `badge-purple`), 생성/수정 모달 `.card`/라운드 통일, 검색 input `.input`.

- [ ] **Step 3: 빌드 + 커밋**

```bash
npm run build --prefix frontend
git add frontend/src/pages/settings/ frontend/src/pages/admin/
git commit -m "[feat] 설정·관리자 화면 정밀 개편"
```

---

## Self-Review (작성자 점검)

**Spec coverage:** 색상 시스템·토큰 아키텍처=Task1; 레이아웃=Task2; 11개 페이지=Task3(Login/Register)·Task4(Dashboard)·Task5(StudentList/Detail)·Task6(Grade/Feedback)·Task7(Counseling/Reports)·Task8(Settings/AdminUsers). 전 항목 매핑됨.

**스타일 작업의 검증 한계(명시):** UI 시각 품질은 `npm run build`로 검증 불가 — 빌드는 컴파일/클래스 무결성만 보장한다. **각 Task의 시각 결과는 `npm run dev --prefix frontend`로 사용자/dev 서버 수동 점검이 필요**하다. 이는 스타일 개편의 본질적 한계이며, 본 계획은 빌드 통과 + 토큰 일관 적용까지를 보장한다.

**일관성:** 모든 페이지가 동일 토큰 어휘(`brand-*`, `accent-*`, `bg`, `surface`, stone-매핑된 `gray-*`, `.card`/`.btn-*`/`.badge-*`)를 사용. Recharts 색은 토큰과 동일 hex(`#0d9488`/`#14b8a6`/`#f59e0b`/`#e7e5e4`)로 통일.

**리스크:** Task1의 `gray→stone`·`primary→brand` 전역 리매핑이 광범위하므로 Task1 직후 빌드 + 주요 페이지 1~2개 육안 확인 권장. 이후 페이지 Task는 독립적이라 순서 무관하게 진행 가능.

---

## Execution Handoff

계획 저장: `docs/superpowers/plans/2026-06-02-ui-redesign.md`

per-page Task는 각 Step 1에서 해당 파일을 Read해 실제 마크업에 맞춰 토큰을 적용한다. 색은 직접 hex 대신 토큰 클래스를 우선 사용하고, Recharts 등 불가피한 hex는 스펙의 토큰 값과 일치시킨다.
