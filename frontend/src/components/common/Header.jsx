import { useState, useEffect, useRef } from 'react'
import useAuthStore from '../../store/authStore'
import { getNotifications, markAsRead, markAllAsRead } from '../../api/notifications'

const typeColor = {
  GRADE:      'bg-blue-100 text-blue-700',
  FEEDBACK:   'bg-purple-100 text-purple-700',
  COUNSELING: 'bg-green-100 text-green-700',
}
const typeLabel = { GRADE: '성적', FEEDBACK: '피드백', COUNSELING: '상담' }

function timeAgo(dateStr) {
  if (!dateStr) return ''
  const diff = Date.now() - new Date(dateStr).getTime()
  const mins = Math.floor(diff / 60000)
  if (mins < 1) return '방금'
  if (mins < 60) return `${mins}분 전`
  const hrs = Math.floor(mins / 60)
  if (hrs < 24) return `${hrs}시간 전`
  return `${Math.floor(hrs / 24)}일 전`
}

export default function Header({ onMenuClick }) {
  const user = useAuthStore((s) => s.user)
  const [open,          setOpen]          = useState(false)
  const [notifications, setNotifications] = useState([])
  const dropdownRef = useRef(null)

  // 알림 목록 로드
  useEffect(() => {
    getNotifications().then((data) => setNotifications(data || [])).catch(() => {})
  }, [])

  // 드롭다운 외부 클릭 시 닫기
  useEffect(() => {
    if (!open) return
    const handler = (e) => {
      if (dropdownRef.current && !dropdownRef.current.contains(e.target)) {
        setOpen(false)
      }
    }
    document.addEventListener('mousedown', handler)
    return () => document.removeEventListener('mousedown', handler)
  }, [open])

  const unread = notifications.filter((n) => !n.isRead).length

  const handleMarkOne = async (n) => {
    if (n.isRead) return
    await markAsRead(n.id).catch(() => {})
    setNotifications((prev) => prev.map((x) => x.id === n.id ? { ...x, isRead: true } : x))
  }

  const handleMarkAll = async () => {
    await markAllAsRead().catch(() => {})
    setNotifications((prev) => prev.map((n) => ({ ...n, isRead: true })))
  }

  return (
    <header className="h-16 bg-white border-b border-gray-200 flex items-center px-4 gap-3 z-30">
      {/* 햄버거 (모바일) */}
      <button
        onClick={onMenuClick}
        className="lg:hidden p-2 rounded-lg hover:bg-gray-100 text-gray-500 transition-colors"
      >
        <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor">
          <path strokeLinecap="round" strokeLinejoin="round" d="M3.75 6.75h16.5M3.75 12h16.5m-16.5 5.25h16.5" />
        </svg>
      </button>

      {/* 스페이서 */}
      <div className="flex-1" />

      {/* 알림 */}
      <div className="relative" ref={dropdownRef}>
        <button
          onClick={() => setOpen((v) => !v)}
          className="relative p-2 rounded-lg hover:bg-gray-100 text-gray-500 transition-colors"
        >
          <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" d="M14.857 17.082a23.848 23.848 0 005.454-1.31A8.967 8.967 0 0118 9.75v-.7V9A6 6 0 006 9v.75a8.967 8.967 0 01-2.312 6.022c1.733.64 3.56 1.085 5.455 1.31m5.714 0a24.255 24.255 0 01-5.714 0m5.714 0a3 3 0 11-5.714 0" />
          </svg>
          {unread > 0 && (
            <span className="absolute top-1 right-1 w-4 h-4 bg-red-500 text-white text-[10px] font-bold rounded-full flex items-center justify-center">
              {unread > 9 ? '9+' : unread}
            </span>
          )}
        </button>

        {open && (
          <div className="absolute right-0 mt-2 w-80 bg-white rounded-xl shadow-modal border border-gray-200 overflow-hidden animate-slide-up z-50">
            <div className="px-4 py-3 border-b border-gray-100 flex items-center justify-between">
              <span className="font-semibold text-sm text-gray-900">알림</span>
              {unread > 0 && (
                <button onClick={handleMarkAll} className="text-xs text-primary-600 hover:text-primary-700 font-medium">
                  모두 읽음
                </button>
              )}
            </div>
            <ul className="max-h-72 overflow-y-auto divide-y divide-gray-50">
              {notifications.length === 0 ? (
                <li className="px-4 py-6 text-center text-sm text-gray-400">알림이 없습니다.</li>
              ) : notifications.slice(0, 10).map((n) => (
                <li
                  key={n.id}
                  onClick={() => handleMarkOne(n)}
                  className={`px-4 py-3 flex gap-3 cursor-pointer hover:bg-gray-50 transition-colors ${!n.isRead ? 'bg-primary-50/50' : ''}`}
                >
                  <div className="flex-shrink-0 mt-0.5">
                    <span className={`badge text-[10px] px-1.5 py-0.5 ${typeColor[n.type] || 'bg-gray-100 text-gray-600'}`}>
                      {typeLabel[n.type] || n.type}
                    </span>
                  </div>
                  <div className="flex-1 min-w-0">
                    <p className="text-sm text-gray-700 leading-snug">{n.message}</p>
                    <p className="text-xs text-gray-400 mt-1">{timeAgo(n.createdAt)}</p>
                  </div>
                  {!n.isRead && <div className="w-2 h-2 rounded-full bg-primary-500 flex-shrink-0 mt-1.5" />}
                </li>
              ))}
            </ul>
            <div className="px-4 py-2.5 border-t border-gray-100 text-center">
              <button className="text-xs text-primary-600 hover:text-primary-700 font-medium" onClick={() => setOpen(false)}>
                전체 알림 보기
              </button>
            </div>
          </div>
        )}
      </div>

      {/* 프로필 */}
      <div className="flex items-center gap-2.5 pl-2">
        <div className="w-8 h-8 rounded-full bg-primary-700 text-white flex items-center justify-center text-sm font-semibold">
          {user?.name?.[0] ?? 'T'}
        </div>
        <div className="hidden sm:block">
          <p className="text-sm font-medium text-gray-900 leading-none">{user?.name ?? '선생님'}</p>
          <p className="text-xs text-gray-400 mt-0.5">{user?.role === 'TEACHER' ? '교사' : user?.role ?? '교사'}</p>
        </div>
      </div>
    </header>
  )
}
