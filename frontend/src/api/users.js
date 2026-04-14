import client from './client'

/** 내 프로필 조회 */
export const getMe = () =>
  client.get('/users/me').then((r) => r.data.data)

/** 내 이름 변경 */
export const updateMe = (name) =>
  client.put('/users/me', { name }).then((r) => r.data.data)

/** 전체 사용자 목록 (ADMIN) */
export const getUsers = () =>
  client.get('/users').then((r) => r.data.data)

/** 사용자 생성 (ADMIN) */
export const createUser = (body) =>
  client.post('/users', body).then((r) => r.data.data)

/** 사용자 수정 (ADMIN) */
export const updateUser = (id, body) =>
  client.put(`/users/${id}`, body).then((r) => r.data.data)

/** 사용자 삭제 (ADMIN) */
export const deleteUser = (id) =>
  client.delete(`/users/${id}`).then((r) => r.data)
