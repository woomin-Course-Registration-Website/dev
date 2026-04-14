import client from './client'

/**
 * 상담 목록 조회
 * @param {Object} params - { studentId, teacherId, from, to }
 */
export const getCounselings = (params = {}) =>
  client.get('/counselings', { params }).then((r) => r.data.data)

/** 상담 상세 조회 */
export const getCounseling = (id) =>
  client.get(`/counselings/${id}`).then((r) => r.data.data)

/**
 * 상담 등록
 * @param {Object} body - { studentId, date, content, nextPlan, shareScope }
 */
export const createCounseling = (body) =>
  client.post('/counselings', body).then((r) => r.data.data)

/** 상담 수정 */
export const updateCounseling = (id, body) =>
  client.put(`/counselings/${id}`, body).then((r) => r.data.data)

/** 상담 삭제 */
export const deleteCounseling = (id) =>
  client.delete(`/counselings/${id}`).then((r) => r.data)
