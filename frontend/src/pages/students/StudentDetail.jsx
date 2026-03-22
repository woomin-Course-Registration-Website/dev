import { useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import {
  RadarChart, Radar, PolarGrid, PolarAngleAxis, ResponsiveContainer, Tooltip,
  BarChart, Bar, XAxis, YAxis, CartesianGrid,
} from 'recharts'

/* ── 목 데이터 ── */
const STUDENT = {
  id: 1, name: '홍길동', grade: 2, classNum: 3, studentNum: 15,
  email: 'hong@school.kr',
}

const GRADES = [
  { subject: '국어', score: 88, rank: 'B+' },
  { subject: '수학', score: 75, rank: 'C+' },
  { subject: '영어', score: 92, rank: 'A'  },
  { subject: '과학', score: 83, rank: 'B+' },
  { subject: '체육', score: 95, rank: 'A+' },
  { subject: '사회', score: 79, rank: 'C+' },
]

const RECORD = {
  attendance: { present: 180, absent: 2, late: 3, early: 1 },
  notes: '수학 학습 의욕이 높고 과학 올림피아드 참가 예정. 진로에 관심이 많으며 상담 적극적으로 임함.',
}

const FEEDBACKS = [
  { id: 1, category: '성적',   date: '2025-03-15', teacher: '김선생님', content: '수학 3단원 이해도가 낮습니다. 추가 학습을 권장합니다.', isPublic: true  },
  { id: 2, category: '행동',   date: '2025-03-10', teacher: '김선생님', content: '수업 중 집중력이 향상됨. 계속 격려 필요.',             isPublic: false },
  { id: 3, category: '태도',   date: '2025-03-01', teacher: '박선생님', content: '발표력이 뛰어나며 친구들과 협력 잘 함.',              isPublic: true  },
]

const COUNSELINGS = [
  { id: 1, date: '2025-03-20', teacher: '김선생님', content: '진로에 대한 고민 상담. 의대 진학에 관심 있음.', nextPlan: '진로 담당 교사와 연계 상담 예정', scope: '전체공개' },
  { id: 2, date: '2025-02-28', teacher: '김선생님', content: '수학 성적 하락 원인 파악. 개념 이해 부족으로 판단.',  nextPlan: '주 2회 방과 후 보충수업 권유', scope: '비공개' },
]

const catColor = {
  성적: 'badge-blue', 행동: 'badge-green', 태도: 'badge-purple', 출결: 'badge-amber', 기타: 'badge-gray',
}

const rankColor = (r) => {
  if (r?.startsWith('A')) return 'text-green-600 font-bold'
  if (r?.startsWith('B')) return 'text-blue-600 font-bold'
  if (r?.startsWith('C')) return 'text-amber-600 font-bold'
  return 'text-red-600 font-bold'
}

const TABS = ['성적', '학생부', '피드백', '상담']

export default function StudentDetail() {
  const { id } = useParams()
  const navigate = useNavigate()
  const [tab, setTab] = useState('성적')
  const [year, setYear] = useState('2025')
  const [semester, setSemester] = useState('1')
  const [editingNote, setEditingNote] = useState(false)
  const [note, setNote] = useState(RECORD.notes)

  const avg = (GRADES.reduce((s, g) => s + g.score, 0) / GRADES.length).toFixed(1)
  const useBar = typeof window !== 'undefined' && window.innerWidth < 640

  return (
    <div className="space-y-5 animate-fade-in">
      {/* 뒤로가기 + 학생 정보 헤더 */}
      <div>
        <button
          onClick={() => navigate('/students')}
          className="flex items-center gap-1.5 text-sm text-gray-500 hover:text-gray-700 mb-3 transition-colors"
        >
          <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" d="M15.75 19.5L8.25 12l7.5-7.5" />
          </svg>
          목록으로
        </button>

        <div className="card p-5 flex items-center gap-4">
          <div className="w-14 h-14 rounded-2xl bg-primary-700 text-white flex items-center justify-center text-2xl font-bold flex-shrink-0">
            {STUDENT.name[0]}
          </div>
          <div>
            <h1 className="text-xl font-bold text-gray-900">{STUDENT.name}</h1>
            <p className="text-sm text-gray-500 mt-0.5">
              {STUDENT.grade}학년 {STUDENT.classNum}반 {STUDENT.studentNum}번
            </p>
          </div>
          <div className="ml-auto flex gap-2">
            <span className="badge badge-blue">학생</span>
          </div>
        </div>
      </div>

      {/* 탭 */}
      <div className="flex gap-1 bg-gray-100 p-1 rounded-xl w-fit">
        {TABS.map((t) => (
          <button
            key={t}
            onClick={() => setTab(t)}
            className={`px-5 py-2 rounded-lg text-sm font-medium transition-all duration-150 ${
              tab === t ? 'bg-white text-gray-900 shadow-card' : 'text-gray-500 hover:text-gray-700'
            }`}
          >
            {t}
          </button>
        ))}
      </div>

      {/* ── 탭: 성적 ── */}
      {tab === '성적' && (
        <div className="space-y-5 animate-fade-in">
          <div className="flex gap-3">
            <select value={year} onChange={(e) => setYear(e.target.value)} className="input w-28 h-9 py-1.5">
              <option>2025</option><option>2024</option>
            </select>
            <select value={semester} onChange={(e) => setSemester(e.target.value)} className="input w-24 h-9 py-1.5">
              <option value="1">1학기</option><option value="2">2학기</option>
            </select>
          </div>

          <div className="grid grid-cols-1 lg:grid-cols-2 gap-5">
            {/* 레이더 차트 */}
            <div className="card p-6">
              <h3 className="font-semibold text-gray-900 mb-4">성적 레이더 차트</h3>
              <ResponsiveContainer width="100%" height={280}>
                {useBar ? (
                  <BarChart data={GRADES} layout="vertical">
                    <CartesianGrid strokeDasharray="3 3" stroke="#f3f4f6" />
                    <XAxis type="number" domain={[0, 100]} tick={{ fontSize: 12 }} />
                    <YAxis dataKey="subject" type="category" tick={{ fontSize: 12 }} width={36} />
                    <Tooltip formatter={(v) => [`${v}점`]} />
                    <Bar dataKey="score" fill="#3b82f6" radius={[0, 4, 4, 0]} />
                  </BarChart>
                ) : (
                  <RadarChart data={GRADES}>
                    <PolarGrid stroke="#e5e7eb" />
                    <PolarAngleAxis dataKey="subject" tick={{ fontSize: 12, fill: '#6b7280' }} />
                    <Radar
                      name="성적"
                      dataKey="score"
                      stroke="#1d4ed8"
                      fill="#3b82f6"
                      fillOpacity={0.25}
                      dot={{ r: 3, fill: '#1d4ed8' }}
                    />
                    <Tooltip formatter={(v) => [`${v}점`, '점수']} />
                  </RadarChart>
                )}
              </ResponsiveContainer>
            </div>

            {/* 과목별 성적표 */}
            <div className="card p-6">
              <h3 className="font-semibold text-gray-900 mb-4">과목별 성적표</h3>
              <table className="w-full">
                <thead>
                  <tr className="table-header border-b border-gray-100">
                    <th className="table-cell">과목</th>
                    <th className="table-cell text-right">점수</th>
                    <th className="table-cell text-center">등급</th>
                  </tr>
                </thead>
                <tbody>
                  {GRADES.map((g) => (
                    <tr key={g.subject} className="border-b border-gray-50 last:border-0">
                      <td className="table-cell font-medium text-gray-800">{g.subject}</td>
                      <td className="table-cell text-right font-mono text-gray-900">{g.score}</td>
                      <td className={`table-cell text-center font-mono ${rankColor(g.rank)}`}>{g.rank}</td>
                    </tr>
                  ))}
                  <tr className="bg-gray-50">
                    <td className="table-cell font-semibold text-gray-900">평균</td>
                    <td className="table-cell text-right font-mono font-semibold text-gray-900">{avg}</td>
                    <td className="table-cell text-center">—</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}

      {/* ── 탭: 학생부 ── */}
      {tab === '학생부' && (
        <div className="space-y-5 animate-fade-in">
          <div className="card p-6">
            <h3 className="font-semibold text-gray-900 mb-4">출결 현황</h3>
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 mb-5">
              {[
                { label: '출석', value: RECORD.attendance.present, color: 'text-green-600' },
                { label: '결석', value: RECORD.attendance.absent,  color: 'text-red-500'   },
                { label: '지각', value: RECORD.attendance.late,    color: 'text-amber-600' },
                { label: '조퇴', value: RECORD.attendance.early,   color: 'text-purple-600'},
              ].map(({ label, value, color }) => (
                <div key={label} className="bg-gray-50 rounded-xl p-4 text-center">
                  <p className="text-xs text-gray-500 mb-1">{label}</p>
                  <p className={`text-2xl font-bold ${color}`}>{value}</p>
                  <p className="text-xs text-gray-400">일</p>
                </div>
              ))}
            </div>
            <div>
              <div className="flex items-center justify-between mb-1.5">
                <span className="text-sm font-medium text-gray-700">출석률</span>
                <span className="text-sm font-semibold text-green-600">
                  {((RECORD.attendance.present / (RECORD.attendance.present + RECORD.attendance.absent + RECORD.attendance.late)) * 100).toFixed(1)}%
                </span>
              </div>
              <div className="h-2.5 bg-gray-100 rounded-full overflow-hidden">
                <div className="h-full bg-green-500 rounded-full" style={{ width: '95.8%' }} />
              </div>
            </div>
          </div>

          <div className="card p-6">
            <div className="flex items-center justify-between mb-3">
              <h3 className="font-semibold text-gray-900">특이사항 / 메모</h3>
              <button
                onClick={() => setEditingNote((v) => !v)}
                className="btn-sm btn-secondary"
              >
                {editingNote ? '취소' : '수정'}
              </button>
            </div>
            {editingNote ? (
              <div className="space-y-3">
                <textarea
                  rows={4}
                  value={note}
                  onChange={(e) => setNote(e.target.value)}
                  className="input resize-none"
                />
                <div className="flex justify-end">
                  <button onClick={() => setEditingNote(false)} className="btn-sm btn-primary">저장</button>
                </div>
              </div>
            ) : (
              <p className="text-sm text-gray-700 leading-relaxed">{note || '—'}</p>
            )}
          </div>
        </div>
      )}

      {/* ── 탭: 피드백 ── */}
      {tab === '피드백' && (
        <div className="space-y-4 animate-fade-in">
          <div className="flex justify-end">
            <button className="btn-md btn-primary gap-2">
              <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" strokeWidth={2} stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" d="M12 4.5v15m7.5-7.5h-15" />
              </svg>
              피드백 추가
            </button>
          </div>
          {FEEDBACKS.map((f) => (
            <div key={f.id} className="card p-5">
              <div className="flex items-start justify-between gap-3 mb-3">
                <div className="flex items-center gap-2 flex-wrap">
                  <span className={`badge ${catColor[f.category] ?? 'badge-gray'}`}>{f.category}</span>
                  <span className="text-xs text-gray-400">{f.date}</span>
                  <span className="text-xs text-gray-500">{f.teacher}</span>
                </div>
                <div className="flex items-center gap-1.5 flex-shrink-0">
                  <span className={`badge ${f.isPublic ? 'badge-green' : 'badge-gray'}`}>
                    {f.isPublic ? '공개' : '비공개'}
                  </span>
                  <button className="btn-sm btn-ghost px-2">수정</button>
                  <button className="btn-sm text-red-500 hover:bg-red-50 rounded-md px-2 text-sm font-medium transition-colors">삭제</button>
                </div>
              </div>
              <p className="text-sm text-gray-700 leading-relaxed">{f.content}</p>
            </div>
          ))}
        </div>
      )}

      {/* ── 탭: 상담 ── */}
      {tab === '상담' && (
        <div className="space-y-4 animate-fade-in">
          <div className="flex justify-end">
            <button className="btn-md btn-primary gap-2">
              <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" strokeWidth={2} stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" d="M12 4.5v15m7.5-7.5h-15" />
              </svg>
              상담 추가
            </button>
          </div>
          {COUNSELINGS.map((c) => (
            <div key={c.id} className="card p-5">
              <div className="flex items-start justify-between gap-3 mb-3">
                <div className="flex items-center gap-2 flex-wrap">
                  <span className="text-sm font-semibold text-gray-900">{c.date}</span>
                  <span className="text-sm text-gray-500">{c.teacher}</span>
                </div>
                <div className="flex items-center gap-1.5 flex-shrink-0">
                  <span className={`badge ${c.scope === '전체공개' ? 'badge-green' : 'badge-gray'}`}>{c.scope}</span>
                  <button className="btn-sm btn-ghost px-2">수정</button>
                  <button className="btn-sm text-red-500 hover:bg-red-50 rounded-md px-2 text-sm font-medium transition-colors">삭제</button>
                </div>
              </div>
              <p className="text-sm text-gray-700 leading-relaxed mb-3">{c.content}</p>
              {c.nextPlan && (
                <div className="bg-blue-50 border border-blue-100 rounded-lg px-4 py-3">
                  <p className="text-xs font-medium text-blue-700 mb-0.5">다음 계획</p>
                  <p className="text-sm text-blue-800">{c.nextPlan}</p>
                </div>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
