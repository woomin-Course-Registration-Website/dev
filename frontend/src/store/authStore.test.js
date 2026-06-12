import { beforeEach, describe, expect, it } from 'vitest'
import useAuthStore from './authStore'

describe('authStore', () => {
  beforeEach(() => {
    localStorage.clear()
    // 상태 초기화 — module-level 초기값은 첫 import 시점에 고정되므로 setState로 강제
    useAuthStore.setState({ user: null, accessToken: null, isAuthenticated: false })
  })

  it('login persists tokens and user to localStorage and updates state', () => {
    const user = { id: 1, name: '교사', role: 'TEACHER' }

    useAuthStore.getState().login(user, 'access-1', 'refresh-1')

    expect(useAuthStore.getState()).toMatchObject({
      user,
      accessToken: 'access-1',
      isAuthenticated: true,
    })
    expect(localStorage.getItem('accessToken')).toBe('access-1')
    expect(localStorage.getItem('refreshToken')).toBe('refresh-1')
    expect(JSON.parse(localStorage.getItem('user'))).toEqual(user)
  })

  it('logout clears state and localStorage', () => {
    useAuthStore.getState().login({ id: 1, name: 'x', role: 'TEACHER' }, 'a', 'r')

    useAuthStore.getState().logout()

    expect(useAuthStore.getState()).toMatchObject({
      user: null,
      accessToken: null,
      isAuthenticated: false,
    })
    expect(localStorage.getItem('accessToken')).toBeNull()
    expect(localStorage.getItem('refreshToken')).toBeNull()
    expect(localStorage.getItem('user')).toBeNull()
  })

  it('setAccessToken updates token in both store and localStorage', () => {
    useAuthStore.getState().setAccessToken('new-access')

    expect(useAuthStore.getState().accessToken).toBe('new-access')
    expect(localStorage.getItem('accessToken')).toBe('new-access')
  })
})
