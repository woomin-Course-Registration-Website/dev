import client from './client'

/**
 * 학생 성적 목록 조회
 * @param {number} studentId
 * @param {Object} params - { year, semester, subjectId }
 */
export const getGrades = (studentId, params = {}) =>
  client.get(`/students/${studentId}/grades`, { params }).then((r) => r.data.data)

/** 성적 입력 */
export const createGrade = (studentId, body) =>
  client.post(`/students/${studentId}/grades`, body).then((r) => r.data.data)

/** 성적 수정 */
export const updateGrade = (gradeId, body) =>
  client.put(`/grades/${gradeId}`, body).then((r) => r.data.data)

/** 성적 삭제 */
export const deleteGrade = (gradeId) =>
  client.delete(`/grades/${gradeId}`).then((r) => r.data)

/** 과목별 성적 입력 현황 (대시보드용) */
export const getGradeStats = (params = {}) =>
  client.get('/grades/stats', { params }).then((r) => r.data.data)
