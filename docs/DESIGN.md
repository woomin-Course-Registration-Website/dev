# UI/UX 디자인 시스템

> React + Tailwind CSS 기반 디자인 토큰 및 컴포넌트 가이드

---

## 1. 디자인 원칙

| 원칙 | 설명 |
|------|------|
| **명확성** | 교사가 빠르게 정보를 파악하고 입력할 수 있는 밀도 있는 UI |
| **신뢰감** | 학교 시스템 특성상 차분한 색상, 과도한 애니메이션 지양 |
| **일관성** | 동일한 액션은 항상 동일한 위치·컬러·아이콘 사용 |
| **반응형** | 교사(데스크톱 위주) / 학생·학부모(모바일 위주) 동시 지원 |

---

## 2. 컬러 팔레트

### Primary (메인 브랜드색 - 파란계열)
```
Primary-900  #1e3a5f   ← 헤더 배경, 강조 텍스트
Primary-700  #1d4ed8   ← 주 버튼, 링크
Primary-500  #3b82f6   ← 호버 상태, 아이콘
Primary-100  #dbeafe   ← 배경 하이라이트, 뱃지
Primary-50   #eff6ff   ← 선택된 사이드바 항목 배경
```

### Neutral (텍스트 & 배경)
```
Neutral-900  #111827   ← 본문 텍스트 (주)
Neutral-600  #4b5563   ← 보조 텍스트
Neutral-400  #9ca3af   ← 플레이스홀더, 비활성
Neutral-200  #e5e7eb   ← 테이블 구분선, 보더
Neutral-100  #f3f4f6   ← 테이블 홀수행 배경
Neutral-50   #f9fafb   ← 페이지 배경
White        #ffffff   ← 카드, 모달 배경
```

### Semantic (상태색)
```
Success-500  #22c55e   ← 저장 완료, 출석
Success-100  #dcfce7   ← 성공 배경
Warning-500  #f59e0b   ← 주의, 미입력
Warning-100  #fef3c7   ← 경고 배경
Danger-500   #ef4444   ← 오류, 삭제
Danger-100   #fee2e2   ← 오류 배경
Info-500     #3b82f6   ← 알림, 정보
Info-100     #dbeafe   ← 알림 배경
```

### 등급 색상 (성적 시각화)
```
A+ / A   #16a34a  (Green-600)
B+ / B   #2563eb  (Blue-600)
C+ / C   #d97706  (Amber-600)
D 이하   #dc2626  (Red-600)
```

---

## 3. 타이포그래피

### 폰트
- **한글**: Pretendard (웹폰트) — 가독성 최우선
- **숫자/영문**: Inter — 테이블 숫자 정렬에 최적
- **코드/등급**: JetBrains Mono — 성적 등급 표시

### 크기 스케일
```
text-xs     12px / 1.5   ← 뱃지, 보조 레이블
text-sm     14px / 1.5   ← 테이블 셀, 폼 레이블
text-base   16px / 1.6   ← 본문 기본
text-lg     18px / 1.5   ← 카드 제목
text-xl     20px / 1.4   ← 섹션 제목
text-2xl    24px / 1.3   ← 페이지 제목
text-3xl    30px / 1.2   ← 대시보드 숫자 KPI
```

### 굵기
```
font-normal  400  ← 본문
font-medium  500  ← 레이블, 버튼
font-semibold 600 ← 제목, 강조
font-bold    700  ← 페이지 제목
```

---

## 4. 레이아웃 & 간격

### 브레이크포인트
```
sm   640px   ← 소형 태블릿
md   768px   ← 태블릿
lg   1024px  ← 노트북 (사이드바 표시 시작)
xl   1280px  ← 데스크톱 (주 타겟)
2xl  1536px  ← 광폭 모니터
```

### 레이아웃 구조
```css
/* 전체 레이아웃 */
.layout {
  display: grid;
  grid-template-columns: 240px 1fr;  /* 사이드바 + 콘텐츠 */
  grid-template-rows: 64px 1fr;      /* 헤더 + 본문 */
  min-height: 100vh;
}

/* 모바일: 사이드바 숨김, 하단 탭 바 표시 */
@media (max-width: 1023px) {
  .sidebar { display: none; }
  .bottom-nav { display: flex; }
}
```

### 간격 토큰
```
space-1   4px
space-2   8px
space-3   12px
space-4   16px
space-5   20px
space-6   24px
space-8   32px
space-10  40px
space-12  48px
```

---

## 5. 컴포넌트 가이드

### 버튼

```
[Primary]      배경 Primary-700  텍스트 White    → 주요 액션 (저장, 로그인)
[Secondary]    배경 White        보더 Neutral-200  텍스트 Neutral-700 → 보조 액션
[Danger]       배경 Danger-500   텍스트 White    → 삭제
[Ghost]        배경 투명         텍스트 Primary-700 → 링크형 버튼

크기:
  sm: h-8  px-3  text-sm
  md: h-10 px-4  text-sm  (기본)
  lg: h-12 px-6  text-base

상태:
  hover:  밝기 10% 올림
  active: 밝기 10% 내림
  disabled: opacity-50, cursor-not-allowed
  loading: 텍스트 숨김 + 스피너 오버레이
```

### 입력 필드 (Input)

```
기본 상태:
  border: 1px solid Neutral-300
  border-radius: 6px
  padding: 8px 12px
  background: White

포커스:
  border: 2px solid Primary-500
  outline: none
  box-shadow: 0 0 0 3px Primary-100

오류:
  border: 1px solid Danger-500
  하단 오류 메시지: text-sm text-Danger-500

비활성:
  background: Neutral-100
  cursor: not-allowed
```

### 테이블

```
헤더행:   background Neutral-100, font-semibold, text-sm, text-Neutral-600
짝수행:   background White
홀수행:   background Neutral-50
호버행:   background Primary-50
선택행:   background Primary-100, 좌측 3px solid Primary-500

셀 패딩:  py-3 px-4
행 높이:  48px (데이터) / 44px (헤더)
구분선:   border-b 1px Neutral-200

정렬:
  텍스트 → 좌측
  숫자   → 우측 (font: JetBrains Mono)
  액션   → 중앙
```

### 카드 (Card)

```
background: White
border: 1px solid Neutral-200
border-radius: 12px
padding: 24px
box-shadow: 0 1px 3px rgba(0,0,0,0.08)

hover (클릭 가능 카드):
  box-shadow: 0 4px 12px rgba(0,0,0,0.12)
  transform: translateY(-1px)
  transition: 200ms ease
```

### 뱃지 (Badge)

```
기본 구조: rounded-full px-2.5 py-0.5 text-xs font-medium

등급 뱃지:
  A+/A → bg-Success-100  text-green-800
  B+/B → bg-Info-100     text-blue-800
  C+/C → bg-Warning-100  text-amber-800
  D 이하 → bg-Danger-100 text-red-800

역할 뱃지:
  TEACHER → bg-Primary-100  text-blue-800
  STUDENT → bg-purple-100   text-purple-800
  PARENT  → bg-orange-100   text-orange-800

공개여부 뱃지:
  공개    → bg-Success-100 text-green-700
  비공개  → bg-Neutral-100 text-Neutral-600
```

### 사이드바

```
너비: 240px (데스크톱), 슬라이드 오버레이 (모바일)
배경: Primary-900

메뉴 아이템:
  기본:    text-Primary-100, padding: 10px 16px
  호버:    background rgba(255,255,255,0.08)
  활성:    background Primary-700, text-White, 좌측 3px solid White
  아이콘:  20px, 오른쪽 12px 여백
  텍스트:  text-sm font-medium

섹션 구분:
  선: 1px solid rgba(255,255,255,0.1)
  여백: my-4
```

### 모달 (Modal)

```
오버레이: rgba(0,0,0,0.5)  backdrop-blur-sm
컨테이너: White, border-radius 16px, max-width 560px
패딩: 32px
헤더: font-bold text-xl + X 버튼 (우상단)
푸터: 버튼 우측 정렬 [취소] [확인]

애니메이션:
  진입: scale 0.95→1 + opacity 0→1, 200ms ease-out
  퇴장: scale 1→0.95 + opacity 1→0, 150ms ease-in
```

### 알림 드롭다운

```
위치: 헤더 🔔 아이콘 하단 우측 정렬
너비: 360px
max-height: 480px  overflow-y: auto

알림 아이템:
  읽지 않음: 좌측 3px solid Primary-500, bg-Primary-50
  읽음:      bg-White
  호버:      bg-Neutral-50

구분:  아이콘(타입) + 메시지 + 시간 (text-xs text-Neutral-400)
```

---

## 6. 레이더 차트 (성적 시각화)

```
라이브러리: Recharts (RadarChart)

크기: 320×320 (데스크톱) / 240×240 (모바일)

색상:
  현재 학기: fill Primary-500 opacity-0.25, stroke Primary-700
  이전 학기: fill Neutral-300 opacity-0.2, stroke Neutral-400

축 레이블: text-sm font-medium Neutral-700
격자:      stroke Neutral-200
점수 표시: 각 꼭짓점 호버 시 툴팁 (배경 White, shadow)

모바일 폴백:
  화면 너비 < 640px → 수평 바 차트(BarChart)로 대체
```

---

## 7. 진행률 바 (성적 입력 현황)

```
구조:
  배경: Neutral-200, border-radius full, height 8px
  진행: Primary-500 → Success-500 (100% 시)
  레이블: 우측 "28/32" text-sm font-medium

상태별 색상:
  0~50%    → Warning-500 (주황)
  51~79%   → Primary-500 (파랑)
  80~99%   → Success-400 (연두)
  100%     → Success-600 (초록)
```

---

## 8. 아이콘

라이브러리: **Heroicons** (Tailwind 공식, outline/solid 세트)

| 용도 | 아이콘 | 이름 |
|------|--------|------|
| 대시보드 | 📊 | `squares-2x2` |
| 학생 목록 | 👥 | `user-group` |
| 성적 관리 | 📝 | `pencil-square` |
| 피드백 | 💬 | `chat-bubble-left-ellipsis` |
| 상담 | 🗒 | `clipboard-document-list` |
| 보고서 | 📄 | `document-chart-bar` |
| 알림 | 🔔 | `bell` |
| 설정 | ⚙ | `cog-6-tooth` |
| 검색 | 🔍 | `magnifying-glass` |
| 삭제 | 🗑 | `trash` |
| 수정 | ✏ | `pencil` |
| 닫기 | ✕ | `x-mark` |
| 공개 | 👁 | `eye` |
| 비공개 | 🚫 | `eye-slash` |

크기 기준: 20px (목록), 24px (헤더), 16px (뱃지 내부)

---

## 9. 반응형 컴포넌트 전환 규칙

| 컴포넌트 | 데스크톱 | 모바일 |
|---------|---------|--------|
| 사이드바 | 고정 240px | 햄버거 메뉴 → 오버레이 |
| 네비게이션 | 사이드바 | 하단 탭 바 4개 |
| 학생 목록 | 테이블 | 카드 리스트 |
| 성적 테이블 | 전체 컬럼 | 이름·점수·등급만 |
| 레이더 차트 | 320px | 바 차트로 전환 |
| 모달 | 중앙 560px | 하단 시트 (bottom sheet) |
| 필터 바 | 인라인 | 접힘 → [필터 ▾] 버튼 |

---

## 10. 마이크로인터랙션

| 트리거 | 피드백 |
|--------|--------|
| 성적 점수 입력 | 등급 즉시 업데이트 (애니메이션 없음, 즉각 반응) |
| 저장 성공 | 체크마크 ✓ + "저장 완료" 토스트 (우하단, 3초) |
| 삭제 클릭 | 확인 모달 → 확인 시 행 페이드아웃 |
| 로딩 중 | 버튼 내 스피너, 테이블 스켈레톤 UI |
| 알림 읽음 | 파란 점 → 회색 점 전환 (200ms fade) |
| 사이드바 활성 | 슬라이드 + 하이라이트 (150ms) |
| 오류 메시지 | 입력 필드 흔들림 애니메이션 (shake, 300ms) |

---

## 11. 접근성 (A11y)

- 모든 버튼·링크에 `aria-label` 제공
- 색맹 대응: 색상만으로 구분하지 않음 (등급은 텍스트 병기)
- 키보드 내비게이션: Tab 순서 논리적 배치, Enter/Space로 활성화
- 포커스 링: Primary-500 2px outline (브라우저 기본 제거 금지)
- 최소 터치 영역: 44×44px (모바일)
- 명암비: 본문 텍스트 4.5:1 이상 (WCAG AA 준수)

---

## 12. Tailwind 설정 (tailwind.config.js)

```js
/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,jsx}'],
  theme: {
    extend: {
      colors: {
        primary: {
          50:  '#eff6ff',
          100: '#dbeafe',
          500: '#3b82f6',
          700: '#1d4ed8',
          900: '#1e3a5f',
        },
      },
      fontFamily: {
        sans: ['Pretendard', 'Inter', 'sans-serif'],
        mono: ['JetBrains Mono', 'monospace'],
      },
      borderRadius: {
        DEFAULT: '6px',
        lg: '12px',
        xl: '16px',
      },
      boxShadow: {
        card: '0 1px 3px rgba(0,0,0,0.08)',
        'card-hover': '0 4px 12px rgba(0,0,0,0.12)',
        modal: '0 20px 60px rgba(0,0,0,0.15)',
      },
    },
  },
  plugins: [],
}
```
