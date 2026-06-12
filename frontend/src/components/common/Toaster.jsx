import useToastStore from '../../store/toastStore'

/**
 * 전역 토스트 표시 영역.
 * App 루트에 한 번만 마운트.
 */
const STYLE_BY_TYPE = {
  error:   'bg-red-50 border-red-300 text-red-800',
  success: 'bg-green-50 border-green-300 text-green-800',
  info:    'bg-blue-50 border-blue-300 text-blue-800',
}

export default function Toaster() {
  const toasts = useToastStore((s) => s.toasts)
  const dismiss = useToastStore((s) => s.dismiss)

  if (toasts.length === 0) return null

  return (
    <div
      aria-live="polite"
      aria-atomic="true"
      className="fixed top-4 right-4 z-50 flex flex-col gap-2 max-w-sm"
    >
      {toasts.map((t) => (
        <div
          key={t.id}
          role={t.type === 'error' ? 'alert' : 'status'}
          className={`border rounded shadow px-4 py-3 text-sm flex items-start gap-2 ${STYLE_BY_TYPE[t.type] || STYLE_BY_TYPE.info}`}
        >
          <span className="flex-1">{t.message}</span>
          <button
            type="button"
            aria-label="알림 닫기"
            onClick={() => dismiss(t.id)}
            className="text-current opacity-60 hover:opacity-100"
          >
            ✕
          </button>
        </div>
      ))}
    </div>
  )
}
