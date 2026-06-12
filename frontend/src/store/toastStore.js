import { create } from 'zustand'

/**
 * 전역 토스트 알림 상태
 *
 * - axios 응답 인터셉터에서 비-2xx 응답 발생 시 자동 노출
 * - 컴포넌트에서 직접 push 가능: useToastStore.getState().push({ type, message })
 * - 4초 후 자동 dismiss
 */

let nextId = 1

const useToastStore = create((set, get) => ({
  toasts: [], // [{ id, type: 'error'|'info'|'success', message }]

  push: (toast) => {
    const id = nextId++
    const t = { id, type: 'info', ...toast }
    set((state) => ({ toasts: [...state.toasts, t] }))
    setTimeout(() => get().dismiss(id), 4000)
    return id
  },

  dismiss: (id) => {
    set((state) => ({ toasts: state.toasts.filter((t) => t.id !== id) }))
  },

  error: (message) => get().push({ type: 'error', message }),
  info:  (message) => get().push({ type: 'info', message }),
  success: (message) => get().push({ type: 'success', message }),
}))

export default useToastStore
