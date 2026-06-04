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
        soft:     '0 1px 2px rgba(41,37,36,0.04), 0 4px 16px rgba(41,37,36,0.06)',
        'soft-lg':'0 8px 30px rgba(41,37,36,0.10)',
        card:     '0 1px 2px rgba(41,37,36,0.04), 0 4px 16px rgba(41,37,36,0.06)',
        modal:    '0 24px 60px rgba(41,37,36,0.18)',
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
