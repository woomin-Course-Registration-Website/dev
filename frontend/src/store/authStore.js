import { create } from 'zustand'

const useAuthStore = create((set) => ({
  user: null,
  token: null,
  isAuthenticated: false,

  login: (user, token) => {
    localStorage.setItem('token', token)
    set({ user, token, isAuthenticated: true })
  },

  logout: () => {
    localStorage.removeItem('token')
    set({ user: null, token: null, isAuthenticated: false })
  },

  // 데모용: 미리 세팅된 교사 계정
  loginDemo: () => {
    const demoUser = { id: 1, name: '김선생님', email: 'teacher@school.kr', role: 'TEACHER' }
    set({ user: demoUser, token: 'demo-token', isAuthenticated: true })
  },
}))

export default useAuthStore
