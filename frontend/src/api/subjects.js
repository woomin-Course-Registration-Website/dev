import client from './client'

/** 과목 목록 조회 */
export const getSubjects = () =>
  client.get('/subjects').then((r) => r.data.data)

/** 과목 추가 (ADMIN 전용) */
export const createSubject = (name) =>
  client.post('/subjects', { name }).then((r) => r.data.data)
