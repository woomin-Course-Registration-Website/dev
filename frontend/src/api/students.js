import client from './client'

/**
 * 학생 목록 조회
 * @param {Object} params - { grade, classNum, keyword }
 */
export const getStudents = (params = {}) =>
  client.get('/students', { params }).then((r) => r.data.data)

/** 학생 상세 조회 */
export const getStudent = (id) =>
  client.get(`/students/${id}`).then((r) => r.data.data)

/** 학생 등록 */
export const createStudent = (body) =>
  client.post('/students', body).then((r) => r.data.data)

/** 학생 정보 수정 */
export const updateStudent = (id, body) =>
  client.put(`/students/${id}`, body).then((r) => r.data.data)
