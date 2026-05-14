import { render, screen } from '@testing-library/react'
import { beforeEach, describe, expect, it } from 'vitest'
import Toaster from './Toaster'
import useToastStore from '../../store/toastStore'

describe('Toaster', () => {
  beforeEach(() => {
    useToastStore.setState({ toasts: [] })
  })

  it('renders nothing when no toasts', () => {
    const { container } = render(<Toaster />)
    expect(container).toBeEmptyDOMElement()
  })

  it('renders error toast with alert role', () => {
    useToastStore.getState().error('서버 오류가 발생했습니다')
    render(<Toaster />)

    const alert = screen.getByRole('alert')
    expect(alert).toHaveTextContent('서버 오류가 발생했습니다')
  })

  it('renders info toast with status role (non-error)', () => {
    useToastStore.getState().info('알림')
    render(<Toaster />)

    expect(screen.getByRole('status')).toHaveTextContent('알림')
  })
})
