import client from './client'

/** 피드백 목록 조회 (역할에 따라 공개/전체 자동 분기) */
export const getFeedbacks = (studentId) =>
  client.get(`/students/${studentId}/feedbacks`).then((r) => r.data.data)

/**
 * 피드백 작성
 * @param {Object} body - { category, content, isPublic }
 */
export const createFeedback = (studentId, body) =>
  client.post(`/students/${studentId}/feedbacks`, body).then((r) => r.data.data)

/** 피드백 수정 */
export const updateFeedback = (feedbackId, body) =>
  client.put(`/feedbacks/${feedbackId}`, body).then((r) => r.data.data)

/** 피드백 삭제 */
export const deleteFeedback = (feedbackId) =>
  client.delete(`/feedbacks/${feedbackId}`).then((r) => r.data)
