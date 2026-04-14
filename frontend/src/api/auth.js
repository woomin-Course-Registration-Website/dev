import client from './client'

/** 로그인 → { accessToken, refreshToken, user } */
export const login = (email, password) =>
  client.post('/auth/login', { email, password }).then((r) => r.data)

/** 회원가입 → { success, data: UserResponse, message } */
export const register = (email, password, name, role) =>
  client.post('/auth/register', { email, password, name, role }).then((r) => r.data)

/** 로그아웃 */
export const logout = () =>
  client.post('/auth/logout').then((r) => r.data)

/** 비밀번호 재설정 이메일 발송 */
export const resetPassword = (email) =>
  client.post('/auth/reset-password', { email }).then((r) => r.data)

/** 비밀번호 변경 (로그인 상태) */
export const changePassword = (currentPassword, newPassword) =>
  client.post('/auth/change-password', { currentPassword, newPassword }).then((r) => r.data)
