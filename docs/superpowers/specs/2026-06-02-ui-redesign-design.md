# UI 전면 개편 설계 — "따뜻하고 친근한" 디자인 시스템

- 작성일: 2026-06-02
- 대상 브랜치: `feature/ui-redesign` (base: `feature/feature-completion`)
- 목표: "AI로 생성한 다른 결과물과 비슷하다"는 제네릭함을 탈피하고, 교육 도메인에 맞는 따뜻하고 친근한 디자인 시스템으로 전 페이지를 정밀 개편한다.

## 배경 — 현재가 제네릭한 이유 (코드 감사 결과)

| # | 요인 | 근거 |
|---|------|------|
| 1 | primary가 표준 Tailwind 블루 | `tailwind.config.js` primary = #3b82f6/#2563eb/#1d4ed8 |
| 2 | 모든 카드가 동일 | `.card` = `bg-white rounded-xl border border-gray-200 shadow-card` |
| 3 | 배지가 기본 Tailwind 색 | `.badge-*` = `bg-{color}-100 text-{color}-800` |
| 4 | 다크 네이비 사이드바 + 라이트 메인 | `Sidebar` `bg-primary-900`, 전형적 admin 레이아웃 |
| 5 | 버튼/입력이 기본 Tailwind | `.btn` rounded-md, `.input` gray-300 border |

핵심 레버리지: 전 페이지가 `index.css`의 공용 클래스(`.card`/`.btn`/`.badge`/`.input`/`.table`)와 `tailwind.config.js` 토큰을 공유한다. 이 레이어를 재정의하면 새 룩이 전 페이지에 cascade되고, 그 위에서 페이지별 정밀 개편을 얹는다.

## 디자인 방향

- **무드**: 따뜻하고 친근함 (교육친화) — 둥근 모서리, 넉넉한 여백, 부드러운 확산 그림자, 웜 뉴트럴.
- **테마**: 라이트 전용. 단, 토큰을 CSS 변수로 구조화해 향후 다크 확장이 가능하도록 한다(다크 구현은 범위 밖).

## 1. 색상 시스템

CSS 변수로 시맨틱 토큰을 `:root`에 정의하고, `tailwind.config.js`가 이를 참조한다(`rgb(var(--brand-600) / <alpha-value>)` 패턴). 색 변경을 한 곳에서 관리하고 다크 확장을 쉽게 한다.

### 브랜드 (틸/에메랄드 그린 — 신뢰 + 친근)
```
brand-50  #f0fdfa   brand-100 #ccfbf1   brand-200 #99f6e4
brand-500 #14b8a6   brand-600 #0d9488   brand-700 #0f766e
brand-800 #115e59   brand-900 #134e4a
```

### 액센트 (따뜻한 앰버/허니 + 보조 코랄)
```
accent-amber  #f59e0b (강조/하이라이트)
accent-coral  #fb7185 (보조 포인트)
```

### 웜 뉴트럴 (따뜻함의 운반체 — 차가운 gray 대체, stone 계열)
```
surface     #ffffff (카드)
bg          #faf9f7 (앱 배경, 크림 오프화이트)
neutral-100 #f5f5f4  neutral-200 #e7e5e4  neutral-300 #d6d3d1
neutral-500 #78716c  neutral-700 #44403c  neutral-900 #292524
```

### 상태색 (웜 톤 조정)
```
success #16a34a   warning #f59e0b   danger #ef4444   info #0ea5e9
```

> `primary` 키는 하위 호환을 위해 `brand`의 별칭으로 유지(기존 `primary-*` 클래스가 즉시 새 색으로 동작).

## 2. 타이포그래피

- 본문/UI: **Pretendard** 유지(한글), Inter 폴백.
- **디스플레이 스케일** 도입: 페이지 타이틀/대형 숫자에 더 큰 사이즈 + 굵은 weight + 좁은 트래킹.
- 숫자(성적·통계·KPI): `font-variant-numeric: tabular-nums`로 정렬 안정화.
- 행간: 본문 `leading-relaxed`로 편안하게.

## 3. 컴포넌트 (index.css `@layer components` 재정의)

| 컴포넌트 | 변경 |
|----------|------|
| `.card` | 크림 서피스 + `rounded-2xl` + 부드러운 확산 그림자(`shadow-soft`), 테두리 최소화(`border-neutral-200/60` 또는 제거) |
| `.btn` | `rounded-lg`, 브랜드 솔리드(`btn-primary` = brand-600→700), 호버 시 살짝 상승(translate-y + shadow) |
| `.btn-soft` | 신규: brand-50 배경 + brand-700 텍스트(보조 강조 버튼) |
| `.badge-*` | 웜 파스텔 세트로 교체(brand/amber/coral/neutral 기반) |
| `.input` | `rounded-lg`, 웜 포커스 링(`ring-brand-200`), neutral-300 테두리 |
| `.table-*` | 헤더 웜톤(neutral-100), 행 호버 `brand-50` |

새 그림자 토큰: `soft`(부드러운 확산), `soft-lg`(호버/모달). 기존 `card`/`modal`은 유지하되 값 웜튜닝.

## 4. 레이아웃 (제네릭 요인 #4 직접 해결)

- **Sidebar**: 다크 네이비(`bg-primary-900`) → **라이트 크림 사이드바**(`bg-surface` 또는 `bg-bg`) + 브랜드색 액티브 "필(pill)"(`bg-brand-50 text-brand-700`), 아이콘 brand 틴트. 로고 영역 웜 처리.
- **Header**: 더 가볍고 여백 있게. 알림/프로필 둥근 처리, 호버 웜톤.
- **Layout**: 배경 `bg-bg`(크림), 메인 영역 패딩·여백 확대.

## 5. 페이지별 정밀 개편 (11개)

공용 토큰 cascade를 기본으로 깔고, 각 페이지에 아래 정밀 개선을 적용한다.

| 페이지 | 핵심 무브 |
|--------|-----------|
| Login / Register | 웜 그라데이션 split 레이아웃, 브랜드 일러스트적 요소, 카드 라운드/그림자 강화 |
| Dashboard | 환영 헤더, KPI 카드 위계 재설계(아이콘 틴트·대형 숫자·서브텍스트), 차트 웜 팔레트, 상담/알림 리스트 톤업 |
| StudentList | 필터바·검색 웜 처리, 학생 행/카드 톤업, 빈 상태 친근하게 |
| StudentDetail | 탭 디자인(브랜드 언더라인/필), 레이더 차트·과목 테이블 새 색, 특기사항/출결 카드 정리 |
| GradeManagement | 입력 그리드 가독성, 저장 버튼/배지 톤업, 통계 카드 |
| FeedbackManagement | 피드백 카드 재디자인(아바타·카테고리 배지·공개 토글), 역할별 뷰 정리 |
| CounselingManagement | 목록/달력 뷰 웜 처리, 공유범위 라디오·교사 멀티셀렉트 정리, 모달 톤업 |
| Reports | 보고서 타입 선택 카드, 미리보기 테이블, 다운로드 버튼 |
| Settings | 탭(프로필/비밀번호/알림) 정리, 알림 토글 스위치 스타일 |
| AdminUsers | 사용자 테이블·역할 배지·모달 톤업 |

## 6. 토큰 아키텍처

- `src/index.css` `:root`에 CSS 변수로 시맨틱 토큰 정의(`--brand-600`, `--bg`, `--surface`, `--accent-amber`, `--text` 등).
- `tailwind.config.js`가 변수를 참조하도록 `colors` 매핑(`brand: { 600: 'rgb(var(--brand-600) / <alpha-value>)' ... }`), `primary`는 `brand` 별칭.
- `@layer components`의 공용 클래스를 새 토큰으로 재작성.
- 결과: 색은 변수 한 곳에서 관리, 컴포넌트 클래스가 전 페이지에 일관 적용, 다크 확장은 `:root` 변수 오버라이드만 추가하면 됨(이번엔 미구현).

## 7. 구현 순서

1. **토큰·컴포넌트 레이어** (`tailwind.config.js`, `index.css`) — 전 페이지 cascade 기반.
2. **레이아웃** (`Layout`, `Sidebar`, `Header`).
3. **첫인상 화면** (Login, Register, Dashboard).
4. **핵심 업무 화면** (StudentList, StudentDetail, GradeManagement).
5. **나머지** (Feedback, Counseling, Reports, Settings, AdminUsers).

각 배치(또는 페이지)마다 `npm run build`로 빌드 무결성 확인 후 커밋.

## 8. 검증

- **빌드**: 페이지 배치마다 `npm run build` 통과 확인.
- **시각 검증(중요·한계)**: 자동 스크린샷 수단이 없으므로 **실제 시각 확인은 `npm run dev` 수동 점검이 필요**하다. 본 작업은 코드/빌드 무결성까지만 보장하며, 픽셀 단위 시각 품질은 사용자 또는 dev 서버 확인으로 최종 검수한다.
- **회귀**: 클래스명 유지(`.card`/`.btn-primary` 등) 원칙 — 마크업 구조 변경은 페이지 개편 시에만, API/데이터 흐름은 불변.

## 9. 범위 제외 (YAGNI)

- 다크 모드 구현(토큰만 대비).
- 새 컴포넌트 라이브러리(shadcn 등)·아이콘 세트·애니메이션 라이브러리 도입.
- 차트 라이브러리 교체(Recharts 유지, 색만 조정).
- 기능 로직·라우팅·상태관리 변경.

## 리스크

- 전 페이지 정밀 개편은 범위가 크다 → 토큰 우선 cascade로 기본선을 빠르게 올리고, 페이지별 개편을 점진 커밋해 위험 분산.
- 시각 품질은 빌드로 검증 불가 → dev 서버 수동 점검을 명시적 단계로 둔다.
- `feature/feature-completion`(미병합 PR) 기반이므로, 해당 PR 병합 시 이 브랜치는 main으로 rebase 필요.
