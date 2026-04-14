import { create } from 'zustand'

/**
 * 인증 전역 상태 (Zustand)
 *
 * - accessToken / refreshToken / user 를 localStorage에 영속적으로 저장합니다.
 * - 페이지 새로고침 시 localStorage에서 상태를 복원합니다.
 * - 토큰 갱신은 api/client.js 인터셉터에서 자동으로 처리됩니다.
 */

// 초기 상태를 localStorage에서 복원
const storedUser  = JSON.parse(localStorage.getItem('user') || 'null')
const storedToken = localStorage.getItem('accessToken')

const useAuthStore = create((set) => ({
  user:            storedUser,
  accessToken:     storedToken,
  isAuthenticated: !!(storedUser && storedToken),

  /**
   * 로그인 성공 시 호출
   * @param {Object} user  - { id, name, role }
   * @param {string} accessToken
   * @param {string} refreshToken
   */
  login: (user, accessToken, refreshToken) => {
    localStorage.setItem('user', JSON.stringify(user))
    localStorage.setItem('accessToken', accessToken)
    localStorage.setItem('refreshToken', refreshToken)
    set({ user, accessToken, isAuthenticated: true })
  },

  /** 로그아웃 시 호출 — localStorage 및 상태 초기화 */
  logout: () => {
    localStorage.removeItem('user')
    localStorage.removeItem('accessToken')
    localStorage.removeItem('refreshToken')
    set({ user: null, accessToken: null, isAuthenticated: false })
  },

  /** 토큰 갱신 후 새 accessToken 저장 (client.js 인터셉터에서 직접 localStorage 처리) */
  setAccessToken: (accessToken) => {
    localStorage.setItem('accessToken', accessToken)
    set({ accessToken })
  },
}))

export default useAuthStore
