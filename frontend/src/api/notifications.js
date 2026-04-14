import client from './client'

/** 내 알림 목록 조회 */
export const getNotifications = () =>
  client.get('/notifications').then((r) => r.data.data)

/** 알림 1개 읽음 처리 */
export const markAsRead = (id) =>
  client.put(`/notifications/${id}/read`).then((r) => r.data)

/** 전체 알림 읽음 처리 */
export const markAllAsRead = () =>
  client.put('/notifications/read-all').then((r) => r.data)
