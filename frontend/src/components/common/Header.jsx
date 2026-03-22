import { useState } from 'react'
import useAuthStore from '../../store/authStore'

const mockNotifications = [
  { id: 1, type: 'GRADE',     message: '홍길동 학생의 수학 성적이 등록되었습니다.', time: '14:30', read: false },
  { id: 2, type: 'FEEDBACK',  message: '김철수 학생 피드백 공개 대기 중입니다.',     time: '11:00', read: false },
  { id: 3, type: 'COUNSELING',message: '내일 이영희 학생 상담이 예정되어 있습니다.', time: '어제',   read: true  },
]

const typeColor = {
  GRADE:      'bg-blue-100 text-blue-700',
  FEEDBACK:   'bg-purple-100 text-purple-700',
  COUNSELING: 'bg-green-100 text-green-700',
}
const typeLabel = { GRADE: '성적', FEEDBACK: '피드백', COUNSELING: '상담' }

export default function Header({ onMenuClick }) {
  const user = useAuthStore((s) => s.user)
  const [open, setOpen] = useState(false)
  const unread = mockNotifications.filter((n) => !n.read).length

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
      <div className="relative">
        <button
          onClick={() => setOpen((v) => !v)}
          className="relative p-2 rounded-lg hover:bg-gray-100 text-gray-500 transition-colors"
        >
          <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" d="M14.857 17.082a23.848 23.848 0 005.454-1.31A8.967 8.967 0 0118 9.75v-.7V9A6 6 0 006 9v.75a8.967 8.967 0 01-2.312 6.022c1.733.64 3.56 1.085 5.455 1.31m5.714 0a24.255 24.255 0 01-5.714 0m5.714 0a3 3 0 11-5.714 0" />
          </svg>
          {unread > 0 && (
            <span className="absolute top-1 right-1 w-4 h-4 bg-red-500 text-white text-[10px] font-bold rounded-full flex items-center justify-center">
              {unread}
            </span>
          )}
        </button>

        {open && (
          <div className="absolute right-0 mt-2 w-80 bg-white rounded-xl shadow-modal border border-gray-200 overflow-hidden animate-slide-up z-50">
            <div className="px-4 py-3 border-b border-gray-100 flex items-center justify-between">
              <span className="font-semibold text-sm text-gray-900">알림</span>
              <button className="text-xs text-primary-600 hover:text-primary-700 font-medium">모두 읽음</button>
            </div>
            <ul className="max-h-72 overflow-y-auto divide-y divide-gray-50">
              {mockNotifications.map((n) => (
                <li
                  key={n.id}
                  className={`px-4 py-3 flex gap-3 cursor-pointer hover:bg-gray-50 transition-colors ${!n.read ? 'bg-primary-50/50' : ''}`}
                >
                  <div className="flex-shrink-0 mt-0.5">
                    <span className={`badge text-[10px] px-1.5 py-0.5 ${typeColor[n.type]}`}>{typeLabel[n.type]}</span>
                  </div>
                  <div className="flex-1 min-w-0">
                    <p className="text-sm text-gray-700 leading-snug">{n.message}</p>
                    <p className="text-xs text-gray-400 mt-1">{n.time}</p>
                  </div>
                  {!n.read && <div className="w-2 h-2 rounded-full bg-primary-500 flex-shrink-0 mt-1.5" />}
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
          <p className="text-xs text-gray-400 mt-0.5">교사</p>
        </div>
      </div>
    </header>
  )
}
