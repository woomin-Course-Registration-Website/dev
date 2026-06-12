import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import useToastStore from './toastStore'

describe('toastStore', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    // 이전 테스트의 잔존 토스트 제거
    useToastStore.setState({ toasts: [] })
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('push appends a toast with type and message', () => {
    const id = useToastStore.getState().push({ type: 'error', message: '오류' })

    const { toasts } = useToastStore.getState()
    expect(toasts).toHaveLength(1)
    expect(toasts[0]).toMatchObject({ id, type: 'error', message: '오류' })
  })

  it('defaults type to info when omitted', () => {
    useToastStore.getState().push({ message: '안내' })
    expect(useToastStore.getState().toasts[0].type).toBe('info')
  })

  it('error / success / info helpers shortcut to push', () => {
    useToastStore.getState().error('e')
    useToastStore.getState().success('s')
    useToastStore.getState().info('i')

    const toasts = useToastStore.getState().toasts
    expect(toasts.map((t) => t.type)).toEqual(['error', 'success', 'info'])
  })

  it('auto-dismisses after 4 seconds', () => {
    useToastStore.getState().error('flashing')

    expect(useToastStore.getState().toasts).toHaveLength(1)
    vi.advanceTimersByTime(4000)
    expect(useToastStore.getState().toasts).toHaveLength(0)
  })

  it('dismiss removes only the targeted id', () => {
    const a = useToastStore.getState().push({ message: 'a' })
    const b = useToastStore.getState().push({ message: 'b' })

    useToastStore.getState().dismiss(a)

    const remaining = useToastStore.getState().toasts
    expect(remaining).toHaveLength(1)
    expect(remaining[0].id).toBe(b)
  })
})
