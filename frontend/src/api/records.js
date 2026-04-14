import client from './client'

/** 학생부 조회 */
export const getRecord = (studentId) =>
  client.get(`/students/${studentId}/records`).then((r) => r.data.data)

/**
 * 학생부 수정
 * @param {Object} body - { attendance: { present, absent, late }, specialNotes }
 */
export const updateRecord = (studentId, body) =>
  client.put(`/students/${studentId}/records`, body).then((r) => r.data.data)
