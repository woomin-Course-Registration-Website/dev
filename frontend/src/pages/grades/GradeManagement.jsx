import { useState, useEffect, useCallback } from 'react'
import { getStudents } from '../../api/students'
import { getGrades, createGrade, updateGrade } from '../../api/grades'
import { getSubjects } from '../../api/subjects'

function calcRank(score) {
  if (score === null || score === '') return ''
  const s = Number(score)
  if (s >= 90) return 'A'
  if (s >= 80) return 'B'
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
  const [year, setYear]           = useState(String(new Date().getFullYear()))
  const [semester, setSemester]   = useState('1')
  const [gradeFilter, setGradeFilter] = useState('2')
  const [classFilter, setClassFilter] = useState('1')
  const [subjectId, setSubjectId] = useState('')

  const [subjects, setSubjects]   = useState([])
  const [students, setStudents]   = useState([])
  const [gradeMap, setGradeMap]   = useState({})   // studentId → grade record
  const [scoreMap, setScoreMap]   = useState({})   // studentId → 입력 중인 점수
  const [savedIds, setSavedIds]   = useState(new Set())
  const [loading, setLoading]     = useState(false)
  const [toast, setToast]         = useState(null)

  // 과목 목록 초기 로드
  useEffect(() => {
    getSubjects().then((list) => {
      setSubjects(list ?? [])
      if (list?.length) setSubjectId(String(list[0].id))
    }).catch(() => {})
  }, [])

  // 학생 + 성적 로드
  const loadData = useCallback(async () => {
    if (!subjectId) return
    setLoading(true)
    try {
      const [studentList, gradeList] = await Promise.all([
        getStudents({ grade: gradeFilter, classNum: classFilter }),
        getGrades(null, { year, semester, subjectId }).catch(() => []),
      ])
      // 학생 전체 조회 후 해당 반 필터
      setStudents(studentList ?? [])

      // studentId → grade 매핑
      const map = {}
      const scores = {}
      ;(gradeList ?? []).forEach((g) => {
        map[g.student?.id ?? g.studentId] = g
        scores[g.student?.id ?? g.studentId] = g.score
      })
      setGradeMap(map)
      setScoreMap(scores)
      setSavedIds(new Set(Object.keys(map).map(Number)))
    } catch {
      showToast('데이터를 불러오지 못했습니다.', 'warn')
    } finally {
      setLoading(false)
    }
  }, [year, semester, gradeFilter, classFilter, subjectId])

  useEffect(() => { loadData() }, [loadData])

  const setScore = (studentId, val) => {
    setScoreMap((prev) => ({ ...prev, [studentId]: val === '' ? null : val }))
    setSavedIds((prev) => { const s = new Set(prev); s.delete(studentId); return s })
  }

  const handleSaveAll = async () => {
    const changed = students.filter((s) => !savedIds.has(s.id) && scoreMap[s.id] != null)
    if (changed.length === 0) { showToast('저장할 변경 사항이 없습니다.', 'warn'); return }

    try {
      await Promise.all(changed.map((s) => {
        const existing = gradeMap[s.id]
        const body = { subjectId: Number(subjectId), year: Number(year), semester: Number(semester), score: Number(scoreMap[s.id]) }
        return existing ? updateGrade(existing.id, body) : createGrade(s.id, body)
      }))
      setSavedIds((prev) => new Set([...prev, ...changed.map((s) => s.id)]))
      showToast(`${changed.length}건 저장 완료`, 'ok')
      loadData()
    } catch {
      showToast('저장 중 오류가 발생했습니다.', 'warn')
    }
  }

  const showToast = (msg, type = 'ok') => {
    setToast({ msg, type })
    setTimeout(() => setToast(null), 2500)
  }

  const filled  = students.filter((s) => scoreMap[s.id] != null)
  const scores  = filled.map((s) => Number(scoreMap[s.id]))
  const avg     = scores.length ? (scores.reduce((a, b) => a + b, 0) / scores.length).toFixed(1) : '—'
  const max     = scores.length ? Math.max(...scores) : '—'
  const min     = scores.length ? Math.min(...scores) : '—'
  const pending = students.filter((s) => !savedIds.has(s.id) && scoreMap[s.id] != null).length

  return (
    <div className="space-y-5 animate-fade-in relative">
      {/* 토스트 */}
      {toast && (
        <div className={`fixed bottom-6 right-6 z-50 flex items-center gap-2 px-4 py-3 rounded-xl shadow-modal text-sm font-medium animate-slide-up
          ${toast.type === 'ok' ? 'bg-green-600 text-white' : 'bg-amber-500 text-white'}`}>
          {toast.type === 'ok'
            ? <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" strokeWidth={2} stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" d="M4.5 12.75l6 6 9-13.5" /></svg>
            : <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" strokeWidth={2} stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" d="M12 9v3.75m-9.303 3.376c-.866 1.5.217 3.374 1.948 3.374h14.71c1.73 0 2.813-1.874 1.948-3.374L13.949 3.378c-.866-1.5-3.032-1.5-3.898 0L2.697 16.126z" /></svg>
          }
          {toast.msg}
        </div>
      )}

      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-gray-900">성적 관리</h1>
      </div>

      {/* 필터 바 */}
      <div className="card p-4 flex flex-wrap gap-3 items-center">
        <select value={year} onChange={(e) => setYear(e.target.value)} className="input w-28 h-9 py-1.5">
          {[2025, 2024, 2023].map((y) => <option key={y}>{y}</option>)}
        </select>
        <select value={semester} onChange={(e) => setSemester(e.target.value)} className="input w-24 h-9 py-1.5">
          <option value="1">1학기</option><option value="2">2학기</option>
        </select>
        <select value={gradeFilter} onChange={(e) => setGradeFilter(e.target.value)} className="input w-28 h-9 py-1.5">
          {[1,2,3].map((g) => <option key={g} value={g}>{g}학년</option>)}
        </select>
        <select value={classFilter} onChange={(e) => setClassFilter(e.target.value)} className="input w-24 h-9 py-1.5">
          {[1,2,3,4,5,6].map((c) => <option key={c} value={c}>{c}반</option>)}
        </select>
        <select value={subjectId} onChange={(e) => setSubjectId(e.target.value)} className="input w-32 h-9 py-1.5">
          {subjects.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}
        </select>
        <div className="ml-auto">
          <button onClick={handleSaveAll} disabled={loading} className="btn-sm btn-primary gap-1.5">
            <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" strokeWidth={2} stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" d="M4.5 12.75l6 6 9-13.5" />
            </svg>
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
            {loading ? (
              <tr><td colSpan={5} className="table-cell text-center py-12">
                <svg className="w-6 h-6 animate-spin mx-auto text-primary-500" fill="none" viewBox="0 0 24 24">
                  <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                  <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
                </svg>
              </td></tr>
            ) : students.map((s, i) => {
              const score = scoreMap[s.id]
              const rank  = calcRank(score)
              const isSaved = savedIds.has(s.id)
              return (
                <tr key={s.id} className={`table-row ${score == null ? 'bg-gray-50/60' : ''}`}>
                  <td className="table-cell text-center text-gray-400 font-mono text-xs">{i + 1}</td>
                  <td className="table-cell">
                    <div className="flex items-center gap-2.5">
                      <div className="w-8 h-8 rounded-full bg-primary-100 text-primary-700 flex items-center justify-center text-sm font-semibold flex-shrink-0">
                        {s.name[0]}
                      </div>
                      <span className={`font-medium ${score == null ? 'text-gray-400' : 'text-gray-900'}`}>{s.name}</span>
                    </div>
                  </td>
                  <td className="table-cell text-right">
                    <input
                      type="number" min={0} max={100} placeholder="점수 입력"
                      value={score ?? ''}
                      onChange={(e) => setScore(s.id, e.target.value)}
                      className="input text-right w-28 h-8 py-1 font-mono"
                    />
                  </td>
                  <td className={`table-cell text-center font-mono text-base ${rankColor(rank)}`}>
                    {rank || <span className="text-gray-300">—</span>}
                  </td>
                  <td className="table-cell text-center">
                    {score != null && (
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
        <h3 className="font-semibold text-gray-900 mb-3 text-sm">반 통계</h3>
        <div className="flex flex-wrap gap-6">
          {[
            { label: '평균', value: avg, color: 'text-primary-700' },
            { label: '최고', value: max, color: 'text-green-600'   },
            { label: '최저', value: min, color: 'text-red-500'     },
            { label: '입력 완료', value: `${filled.length}/${students.length}명`, color: 'text-gray-900' },
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
