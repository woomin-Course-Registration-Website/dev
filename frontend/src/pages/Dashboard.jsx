import { useState, useEffect, useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import { getStudents } from '../api/students'
import { getCounselings } from '../api/counselings'
import { getNotifications, markAsRead } from '../api/notifications'
import { getGradeStats } from '../api/grades'

const currentYear     = new Date().getFullYear()
const currentSemester = new Date().getMonth() >= 8 ? 2 : 1

const ntypeColor = { GRADE: 'badge-blue', FEEDBACK: 'badge-purple', COUNSELING: 'badge-green' }
const ntypeLabel = { GRADE: '성적', FEEDBACK: '피드백', COUNSELING: '상담' }

function progressColor(pct) {
  if (pct === 1) return 'bg-green-500'
  if (pct >= 0.8) return 'bg-primary-500'
  if (pct >= 0.5) return 'bg-amber-500'
  return 'bg-red-400'
}

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

export default function Dashboard() {
  const navigate = useNavigate()
  const today = new Date().toLocaleDateString('ko-KR', { year: 'numeric', month: 'long', day: 'numeric', weekday: 'short' })
  const todayStr = new Date().toISOString().slice(0, 10)

  const [students,      setStudents]      = useState([])
  const [counselings,   setCounselings]   = useState([])
  const [notifications, setNotifications] = useState([])
  const [gradeProgress, setGradeProgress] = useState([])
  const [loading,       setLoading]       = useState(true)

  const loadData = useCallback(async () => {
    try {
      const [s, c, n, stats] = await Promise.all([
        getStudents(),
        getCounselings(),
        getNotifications(),
        getGradeStats({ semester: currentSemester }).catch(() => []),
      ])
      setStudents(s     || [])
      setCounselings(c  || [])
      setNotifications(n || [])
      setGradeProgress(
        (stats || []).map((item) => ({
          subject: item.subjectName,
          done:    Number(item.gradeCount),
          total:   Number(item.studentCount),
        }))
      )
    } catch {
      // 에러는 axios 인터셉터에서 처리
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => { loadData() }, [loadData])

  const todayCounselings  = counselings.filter((c) => c.date === todayStr)
  const unreadCount       = notifications.filter((n) => !n.isRead).length
  const recentCounselings = [...counselings].sort((a, b) => b.date.localeCompare(a.date)).slice(0, 4)
  const recentNotifs      = notifications.slice(0, 5)

  const kpiCards = [
    {
      label: '담당 학생',
      value: loading ? '...' : `${students.length}명`,
      sub: '전체 학생',
      color: 'bg-blue-50 text-blue-700',
      icon: IconUsers,
    },
    {
      label: '오늘 상담',
      value: loading ? '...' : `${todayCounselings.length}건`,
      sub: '오늘 일정',
      color: 'bg-green-50 text-green-700',
      icon: IconCalendar,
    },
    {
      label: '미읽은 알림',
      value: loading ? '...' : `${unreadCount}건`,
      sub: '확인 필요',
      color: 'bg-purple-50 text-purple-700',
      icon: IconBell,
    },
    {
      label: '전체 상담',
      value: loading ? '...' : `${counselings.length}건`,
      sub: '누적 기록',
      color: 'bg-amber-50 text-amber-700',
      icon: IconChat,
    },
  ]

  const handleNotifClick = async (n) => {
    if (!n.isRead) {
      await markAsRead(n.id).catch(() => {})
      setNotifications((prev) => prev.map((x) => x.id === n.id ? { ...x, isRead: true } : x))
    }
  }

  return (
    <div className="space-y-6 animate-fade-in">
      {/* 페이지 제목 */}
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-gray-900">대시보드</h1>
        <span className="text-sm text-gray-400">{today}</span>
      </div>

      {/* KPI 카드 */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        {kpiCards.map(({ label, value, sub, color, icon: Icon }) => (
          <div key={label} className="card p-5 flex items-start gap-4">
            <div className={`w-10 h-10 rounded-xl flex items-center justify-center flex-shrink-0 ${color}`}>
              <Icon className="w-5 h-5" />
            </div>
            <div>
              <p className="text-xs text-gray-500 font-medium">{label}</p>
              <p className="text-2xl font-bold text-gray-900 leading-tight mt-0.5">{value}</p>
              <p className="text-xs text-gray-400 mt-0.5">{sub}</p>
            </div>
          </div>
        ))}
      </div>

      {/* 중간 섹션 */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* 성적 입력 현황 */}
        <div className="card p-6">
          <div className="flex items-center justify-between mb-5">
            <h2 className="font-semibold text-gray-900">성적 입력 현황 <span className="text-xs font-normal text-gray-400">{currentYear}년 {currentSemester}학기</span></h2>
            <button
              onClick={() => navigate('/grades')}
              className="text-xs text-primary-600 hover:text-primary-700 font-medium flex items-center gap-1"
            >
              전체 보기
              <svg className="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" strokeWidth={2} stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" d="M8.25 4.5l7.5 7.5-7.5 7.5" />
              </svg>
            </button>
          </div>
          {gradeProgress.length === 0 && !loading && (
            <div className="flex items-center justify-center h-24 text-gray-400 text-sm">
              등록된 과목 또는 학생이 없습니다.
            </div>
          )}
          <div className="space-y-3.5">
            {gradeProgress.map(({ subject, done, total }) => {
              const pct = done / total
              return (
                <div key={subject}>
                  <div className="flex items-center justify-between mb-1.5">
                    <span className="text-sm font-medium text-gray-700">{subject}</span>
                    <span className="text-sm text-gray-500 font-mono">{done}/{total}</span>
                  </div>
                  <div className="h-2 bg-gray-100 rounded-full overflow-hidden">
                    <div
                      className={`h-full rounded-full transition-all duration-500 ${progressColor(pct)}`}
                      style={{ width: `${pct * 100}%` }}
                    />
                  </div>
                </div>
              )
            })}
          </div>
        </div>

        {/* 최근 상담 일정 */}
        <div className="card p-6">
          <div className="flex items-center justify-between mb-5">
            <h2 className="font-semibold text-gray-900">상담 일정</h2>
            <button
              onClick={() => navigate('/counseling')}
              className="text-xs text-primary-600 hover:text-primary-700 font-medium flex items-center gap-1"
            >
              전체 보기
              <svg className="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" strokeWidth={2} stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" d="M8.25 4.5l7.5 7.5-7.5 7.5" />
              </svg>
            </button>
          </div>
          {loading ? (
            <div className="flex items-center justify-center h-32 text-gray-400 text-sm">불러오는 중...</div>
          ) : recentCounselings.length === 0 ? (
            <div className="flex items-center justify-center h-32 text-gray-400 text-sm">상담 기록이 없습니다.</div>
          ) : (
            <ul className="space-y-3">
              {recentCounselings.map((c) => (
                <li
                  key={c.id}
                  onClick={() => navigate('/counseling')}
                  className="flex items-center gap-3 p-3 rounded-xl hover:bg-gray-50 transition-colors cursor-pointer group"
                >
                  <div className="w-10 h-10 rounded-xl bg-primary-100 text-primary-700 flex items-center justify-center font-semibold text-sm flex-shrink-0">
                    {(c.studentName || '?')[0]}
                  </div>
                  <div className="flex-1 min-w-0">
                    <p className="text-sm font-medium text-gray-900">{c.studentName}</p>
                    <p className="text-xs text-gray-400 truncate">{c.content?.slice(0, 30)}</p>
                  </div>
                  <div className="text-right flex-shrink-0">
                    <p className="text-xs font-medium text-gray-600">{c.date}</p>
                  </div>
                </li>
              ))}
            </ul>
          )}
        </div>
      </div>

      {/* 최근 알림 */}
      <div className="card p-6">
        <div className="flex items-center justify-between mb-4">
          <h2 className="font-semibold text-gray-900">최근 알림</h2>
          {unreadCount > 0 && (
            <span className="text-xs bg-primary-100 text-primary-700 font-semibold px-2 py-0.5 rounded-full">
              {unreadCount}개 미읽음
            </span>
          )}
        </div>
        {loading ? (
          <div className="text-center text-gray-400 text-sm py-6">불러오는 중...</div>
        ) : recentNotifs.length === 0 ? (
          <div className="text-center text-gray-400 text-sm py-6">새로운 알림이 없습니다.</div>
        ) : (
          <ul className="space-y-2">
            {recentNotifs.map((n) => (
              <li
                key={n.id}
                onClick={() => handleNotifClick(n)}
                className={`flex items-start gap-3 p-3 rounded-xl cursor-pointer hover:bg-gray-50 transition-colors ${!n.isRead ? 'bg-primary-50/60' : ''}`}
              >
                <div className={`w-2 h-2 rounded-full mt-1.5 flex-shrink-0 ${n.isRead ? 'bg-gray-300' : 'bg-primary-500'}`} />
                <div className="flex-1 min-w-0">
                  <span className={`badge mr-2 ${ntypeColor[n.type] || 'badge-gray'}`}>{ntypeLabel[n.type] || n.type}</span>
                  <span className="text-sm text-gray-700">{n.message}</span>
                </div>
                <span className="text-xs text-gray-400 flex-shrink-0">{timeAgo(n.createdAt)}</span>
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  )
}

/* ── 아이콘 ── */
function IconUsers({ className }) {
  return <svg className={className} fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" d="M15 19.128a9.38 9.38 0 002.625.372 9.337 9.337 0 004.121-.952 4.125 4.125 0 00-7.533-2.493M15 19.128v-.003c0-1.113-.285-2.16-.786-3.07M15 19.128v.106A12.318 12.318 0 018.624 21c-2.331 0-4.512-.645-6.374-1.766l-.001-.109a6.375 6.375 0 0111.964-3.07M12 6.375a3.375 3.375 0 11-6.75 0 3.375 3.375 0 016.75 0zm8.25 2.25a2.625 2.625 0 11-5.25 0 2.625 2.625 0 015.25 0z" /></svg>
}
function IconCalendar({ className }) {
  return <svg className={className} fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" d="M6.75 3v2.25M17.25 3v2.25M3 18.75V7.5a2.25 2.25 0 012.25-2.25h13.5A2.25 2.25 0 0121 7.5v11.25m-18 0A2.25 2.25 0 005.25 21h13.5A2.25 2.25 0 0021 18.75m-18 0v-7.5A2.25 2.25 0 015.25 9h13.5A2.25 2.25 0 0121 11.25v7.5" /></svg>
}
function IconChat({ className }) {
  return <svg className={className} fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" d="M7.5 8.25h9m-9 3H12m-9.75 1.51c0 1.6 1.123 2.994 2.707 3.227 1.129.166 2.27.293 3.423.379.35.026.67.21.865.501L12 21l2.755-4.133a1.14 1.14 0 01.865-.501 48.172 48.172 0 003.423-.379c1.584-.233 2.707-1.626 2.707-3.228V6.741c0-1.602-1.123-2.995-2.707-3.228A48.394 48.394 0 0012 3c-2.392 0-4.744.175-7.043.513C3.373 3.746 2.25 5.14 2.25 6.741v6.018z" /></svg>
}
function IconBell({ className }) {
  return <svg className={className} fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" d="M14.857 17.082a23.848 23.848 0 005.454-1.31A8.967 8.967 0 0118 9.75v-.7V9A6 6 0 006 9v.75a8.967 8.967 0 01-2.312 6.022c1.733.64 3.56 1.085 5.455 1.31m5.714 0a24.255 24.255 0 01-5.714 0m5.714 0a3 3 0 11-5.714 0" /></svg>
}
