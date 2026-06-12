import '@testing-library/jest-dom/vitest'
import { afterEach } from 'vitest'

// 각 테스트 종료 시 localStorage 정리 — store는 모듈 캐시되어 테스트 간 누수 방지
afterEach(() => {
  localStorage.clear()
})
