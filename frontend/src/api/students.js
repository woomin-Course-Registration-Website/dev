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

/** 현재 로그인한 STUDENT 계정의 학생 정보 조회 */
export const getMyStudent = () =>
  client.get('/students/me').then((r) => r.data.data)

/** 현재 로그인한 PARENT 계정의 자녀 목록 조회 */
export const getMyChildren = () =>
  client.get('/students/my-children').then((r) => r.data.data)

/** 학생에 학부모 계정 연동 (교사 전용) */
export const addParent = (studentId, parentUserId) =>
  client.post(`/students/${studentId}/parents`, { parentUserId }).then((r) => r.data.data)

/** 학생에서 학부모 계정 연동 해제 (교사 전용) */
export const removeParent = (studentId, parentUserId) =>
  client.delete(`/students/${studentId}/parents/${parentUserId}`).then((r) => r.data.data)
