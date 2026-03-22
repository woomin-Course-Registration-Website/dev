import { useState } from 'react'

const SUBJECTS = ['국어', '수학', '영어', '과학', '사회', '체육']

const INITIAL_GRADES = [
  { id: 1,  name: '홍길동', score: 88 },
  { id: 2,  name: '김철수', score: 75 },
  { id: 3,  name: '이영희', score: 92 },
  { id: 4,  name: '박민준', score: null },
  { id: 5,  name: '최수진', score: 88 },
  { id: 6,  name: '정하늘', score: 77 },
  { id: 7,  name: '강지훈', score: 94 },
  { id: 8,  name: '윤서연', score: null },
  { id: 9,  name: '임도현', score: 80 },
  { id: 10, name: '한소희', score: 87 },
]

function calcRank(score) {
  if (score === null || score === '') return ''
  const s = Number(score)
  if (s >= 95) return 'A+'
  if (s >= 90) return 'A'
  if (s >= 85) return 'B+'
  if (s >= 80) return 'B'
  if (s >= 75) return 'C+'
  if (s >= 70) return 'C'
  if (s >= 60) return 'D'
  return 'F'
}

const rankColor = (r) => {
  if (!r) return 'text-gray-300'
  if (r.startsWith('A')) return 'text-green-600'
  if (r.startsWith('B')) return 'text-blue-600'
  if (r.startsWith('C')) return 'text-amber-600'
  return 'text-red-500'
}

export default function GradeManagement() {
  const [year, setYear]       = useState('2025')
  const [semester, setSemester] = useState('1')
  const [gradeFilter, setGradeFilter] = useState('2')
  const [classFilter, setClassFilter] = useState('3')
  const [subject, setSubject] = useState('국어')
  const [grades, setGrades]   = useState(INITIAL_GRADES)
  const [saved, setSaved]     = useState(new Set(INITIAL_GRADES.filter((g) => g.score !== null).map((g) => g.id)))
  const [toast, setToast]     = useState(null)

  const setScore = (id, val) => {
    setGrades((prev) => prev.map((g) => g.id === id ? { ...g, score: val === '' ? null : Number(val) } : g))
    setSaved((prev) => { const s = new Set(prev); s.delete(id); return s })
  }

  const handleSaveAll = () => {
    const changed = grades.filter((g) => !saved.has(g.id) && g.score !== null)
    if (changed.length === 0) {
      showToast('저장할 변경 사항이 없습니다.', 'warn')
      return
    }
    setSaved(new Set(grades.filter((g) => g.score !== null).map((g) => g.id)))
    showToast(`${changed.length}건 저장 완료`, 'ok')
  }

  const showToast = (msg, type = 'ok') => {
    setToast({ msg, type })
    setTimeout(() => setToast(null), 2500)
  }

  const filled = grades.filter((g) => g.score !== null)
  const avg    = filled.length ? (filled.reduce((s, g) => s + g.score, 0) / filled.length).toFixed(1) : '—'
  const max    = filled.length ? Math.max(...filled.map((g) => g.score)) : '—'
  const min    = filled.length ? Math.min(...filled.map((g) => g.score)) : '—'

  const pending = grades.filter((g) => !saved.has(g.id) && g.score !== null).length

  return (
    <div className="space-y-5 animate-fade-in relative">
      {/* 토스트 */}
      {toast && (
        <div className={`fixed bottom-6 right-6 z-50 flex items-center gap-2 px-4 py-3 rounded-xl shadow-modal text-sm font-medium animate-slide-up
          ${toast.type === 'ok' ? 'bg-green-600 text-white' : 'bg-amber-500 text-white'}`}
        >
          {toast.type === 'ok'
            ? <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" strokeWidth={2} stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" d="M4.5 12.75l6 6 9-13.5" /></svg>
            : <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" strokeWidth={2} stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" d="M12 9v3.75m-9.303 3.376c-.866 1.5.217 3.374 1.948 3.374h14.71c1.73 0 2.813-1.874 1.948-3.374L13.949 3.378c-.866-1.5-3.032-1.5-3.898 0L2.697 16.126z" /></svg>
          }
          {toast.msg}
        </div>
      )}

      {/* 헤더 */}
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-gray-900">성적 관리</h1>
      </div>

      {/* 필터 바 */}
      <div className="card p-4 flex flex-wrap gap-3 items-center">
        <select value={year}       onChange={(e) => setYear(e.target.value)}       className="input w-28 h-9 py-1.5">
          <option>2025</option><option>2024</option>
        </select>
        <select value={semester}   onChange={(e) => setSemester(e.target.value)}   className="input w-24 h-9 py-1.5">
          <option value="1">1학기</option><option value="2">2학기</option>
        </select>
        <select value={gradeFilter} onChange={(e) => setGradeFilter(e.target.value)} className="input w-28 h-9 py-1.5">
          {[1,2,3].map((g) => <option key={g} value={g}>{g}학년</option>)}
        </select>
        <select value={classFilter} onChange={(e) => setClassFilter(e.target.value)} className="input w-24 h-9 py-1.5">
          {[1,2,3,4,5,6].map((c) => <option key={c} value={c}>{c}반</option>)}
        </select>
        <select value={subject}    onChange={(e) => setSubject(e.target.value)}    className="input w-28 h-9 py-1.5">
          {SUBJECTS.map((s) => <option key={s}>{s}</option>)}
        </select>

        <div className="ml-auto flex gap-2 items-center">
          <button className="btn-sm btn-secondary gap-1.5">
            <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" d="M3 16.5v2.25A2.25 2.25 0 005.25 21h13.5A2.25 2.25 0 0021 18.75V16.5m-13.5-9L12 3m0 0l4.5 4.5M12 3v13.5" /></svg>
            Excel 업로드
          </button>
          <button
            onClick={handleSaveAll}
            className="btn-sm btn-primary gap-1.5"
          >
            <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" strokeWidth={2} stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" d="M4.5 12.75l6 6 9-13.5" /></svg>
            일괄 저장{pending > 0 && <span className="bg-white/25 rounded-full px-1.5 py-0.5 text-xs">{pending}</span>}
          </button>
        </div>
      </div>

      {/* 테이블 */}
      <div className="card overflow-hidden">
        <table className="w-full">
          <thead>
            <tr className="table-header border-b border-gray-200">
              <th className="table-cell w-12 text-center">#</th>
              <th className="table-cell">이름</th>
              <th className="table-cell text-right w-36">점수 (입력)</th>
              <th className="table-cell text-center w-24">등급 (자동)</th>
              <th className="table-cell text-center w-20">저장</th>
            </tr>
          </thead>
          <tbody>
            {grades.map((g, i) => {
              const rank = calcRank(g.score)
              const isSaved = saved.has(g.id)
              return (
                <tr key={g.id} className={`table-row ${g.score === null ? 'bg-gray-50/60' : ''}`}>
                  <td className="table-cell text-center text-gray-400 font-mono text-xs">{i + 1}</td>
                  <td className="table-cell">
                    <div className="flex items-center gap-2.5">
                      <div className="w-8 h-8 rounded-full bg-primary-100 text-primary-700 flex items-center justify-center text-sm font-semibold flex-shrink-0">
                        {g.name[0]}
                      </div>
                      <span className={`font-medium ${g.score === null ? 'text-gray-400' : 'text-gray-900'}`}>{g.name}</span>
                    </div>
                  </td>
                  <td className="table-cell text-right">
                    <input
                      type="number"
                      min={0}
                      max={100}
                      placeholder="점수 입력"
                      value={g.score ?? ''}
                      onChange={(e) => setScore(g.id, e.target.value)}
                      className="input text-right w-28 h-8 py-1 font-mono"
                    />
                  </td>
                  <td className={`table-cell text-center font-mono text-base ${rankColor(rank)}`}>
                    {rank || <span className="text-gray-300">—</span>}
                  </td>
                  <td className="table-cell text-center">
                    {g.score !== null && (
                      isSaved
                        ? <svg className="w-5 h-5 text-green-500 mx-auto" fill="none" viewBox="0 0 24 24" strokeWidth={2} stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" d="M4.5 12.75l6 6 9-13.5" /></svg>
                        : <span className="w-2 h-2 bg-amber-400 rounded-full inline-block" />
                    )}
                  </td>
                </tr>
              )
            })}
          </tbody>
        </table>
      </div>

      {/* 통계 */}
      <div className="card p-5">
        <h3 className="font-semibold text-gray-900 mb-3 text-sm">반 통계 ({subject})</h3>
        <div className="flex flex-wrap gap-6">
          {[
            { label: '평균', value: avg,              color: 'text-primary-700' },
            { label: '최고', value: max,              color: 'text-green-600'   },
            { label: '최저', value: min,              color: 'text-red-500'     },
            { label: '입력 완료', value: `${filled.length}/${grades.length}명`, color: 'text-gray-900' },
          ].map(({ label, value, color }) => (
            <div key={label} className="text-center">
              <p className="text-xs text-gray-500 mb-0.5">{label}</p>
              <p className={`text-xl font-bold font-mono ${color}`}>{value}</p>
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}
