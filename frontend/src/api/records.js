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

/** 특기사항 항목 목록 → [{ id, content, createdAt }] */
export const listRecordNotes = (studentId) =>
  client.get(`/students/${studentId}/records/notes`).then((r) => r.data.data)

/** 특기사항 항목 추가 */
export const addRecordNote = (studentId, content) =>
  client.post(`/students/${studentId}/records/notes`, { content }).then((r) => r.data.data)

/** 특기사항 항목 수정 */
export const updateRecordNote = (noteId, content) =>
  client.put(`/record-notes/${noteId}`, { content }).then((r) => r.data.data)

/** 특기사항 항목 삭제 */
export const deleteRecordNote = (noteId) =>
  client.delete(`/record-notes/${noteId}`).then((r) => r.data)
