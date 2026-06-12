import client from './client'

/** 분석 개요 (적재된 학생·과목 목록) */
export const getOverview = () =>
  client.get('/analytics/overview').then((r) => r.data.data)

/** 학생별 학습 현황 집계 */
export const getStudentSummary = (studentId) =>
  client.get(`/analytics/students/${studentId}/summary`).then((r) => r.data.data)

/** 과목별 학습 현황 집계 */
export const getSubjectDistribution = (subjectId) =>
  client.get(`/analytics/subjects/${subjectId}/distribution`).then((r) => r.data.data)

/** ETL 수동 실행 */
export const runEtl = () =>
  client.post('/analytics/etl/run').then((r) => r.data.data)

/** AI 챗봇 활성 여부 (LLM_API_KEY 설정 시 true) */
export const chatStatus = () =>
  client.get('/analytics/chat/status').then((r) => r.data.data)

/** AI 챗봇 질의 (선택 기능, LLM_API_KEY 설정 시) */
export const chat = (message, studentId) =>
  client.post('/analytics/chat', { message, studentId }).then((r) => r.data.data)
