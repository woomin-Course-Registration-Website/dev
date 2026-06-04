import { useState, useEffect } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import {
  RadarChart, Radar, PolarGrid, PolarAngleAxis, ResponsiveContainer, Tooltip,
  BarChart, Bar, XAxis, YAxis, CartesianGrid,
} from 'recharts'
import { getStudent, addParent, removeParent } from '../../api/students'
import { getGrades } from '../../api/grades'
import { getRecord, updateRecord, listRecordNotes, addRecordNote, updateRecordNote, deleteRecordNote } from '../../api/records'
import { getFeedbacks, createFeedback, deleteFeedback } from '../../api/feedbacks'
import { getCounselings, createCounseling, deleteCounseling, getPublicCounselings } from '../../api/counselings'
import useAuthStore from '../../store/authStore'

const catColorMap = {
  GRADE: 'badge-blue', BEHAVIOR: 'badge-green', ATTENDANCE: 'badge-amber',
  ATTITUDE: 'badge-purple', OTHER: 'badge-gray',
}
const catLabel = { GRADE: '성적', BEHAVIOR: '행동', ATTENDANCE: '출결', ATTITUDE: '태도', OTHER: '기타' }

const rankColor = (r) => {
  if (!r) return 'text-gray-300'
  if (r.startsWith('A')) return 'text-green-600 font-bold'
  if (r.startsWith('B')) return 'text-blue-600 font-bold'
  if (r.startsWith('C')) return 'text-amber-600 font-bold'
  return 'text-red-600 font-bold'
}

const TEACHER_TABS = ['성적', '학생부', '피드백', '상담', '학부모']
const STUDENT_TABS = ['성적', '학생부', '피드백', '상담']
const FEEDBACK_CATEGORIES = ['GRADE', 'BEHAVIOR', 'ATTENDANCE', 'ATTITUDE', 'OTHER']

export default function StudentDetail() {
  const { id } = useParams()
  const navigate = useNavigate()
  const user = useAuthStore((s) => s.user)
  const isTeacher = user?.role === 'TEACHER'

  const TABS = isTeacher ? TEACHER_TABS : STUDENT_TABS
  const [tab, setTab]     = useState('성적')
  const [year, setYear]   = useState(String(new Date().getFullYear()))
  const [semester, setSemester] = useState('1')

  // 데이터 상태
  const [student, setStudent]       = useState(null)
  const [grades, setGrades]         = useState([])
  const [record, setRecord]         = useState(null)
  const [feedbacks, setFeedbacks]   = useState([])
  const [counselings, setCounselings] = useState([])
  const [loading, setLoading]       = useState(true)

  // 학생부 수정
  const [editingNote, setEditingNote] = useState(false)
  const [noteForm, setNoteForm]       = useState({ present: '', absent: '', late: '' })

  // 특기사항 다항목
  const [notes, setNotes]               = useState([])
  const [noteInput, setNoteInput]       = useState('')
  const [editNoteId, setEditNoteId]     = useState(null)
  const [editNoteText, setEditNoteText] = useState('')

  // 피드백 추가 폼
  const [fbForm, setFbForm]   = useState({ category: 'GRADE', content: '', isPublic: true })
  const [fbModal, setFbModal] = useState(false)

  // 상담 추가 폼
  const [csForm, setCsForm]   = useState({ date: '', content: '', nextPlan: '', shareScope: 'ALL' })
  const [csModal, setCsModal] = useState(false)

  // 학부모 연동 (교사용)
  const [parentIdInput, setParentIdInput] = useState('')
  const [parentSaving,  setParentSaving]  = useState(false)

  // 초기 데이터 로드
  useEffect(() => {
    const load = async () => {
      setLoading(true)
      try {
        const [s] = await Promise.all([getStudent(id)])
        setStudent(s)
      } catch { /* 에러 무시 */ }
      setLoading(false)
    }
    load()
  }, [id])

  // 탭별 데이터 로드
  useEffect(() => {
    if (!id) return
    if (tab === '성적') {
      getGrades(id, { year, semester }).then(setGrades).catch(() => setGrades([]))
    } else if (tab === '학생부') {
      getRecord(id).then((r) => {
        setRecord(r)
        const att = r.attendance ? JSON.parse(r.attendance) : {}
        setNoteForm({ present: att.present ?? '', absent: att.absent ?? '', late: att.late ?? '' })
      }).catch(() => setRecord(null))
      listRecordNotes(id).then(setNotes).catch(() => setNotes([]))
    } else if (tab === '피드백') {
      getFeedbacks(id).then(setFeedbacks).catch(() => setFeedbacks([]))
    } else if (tab === '상담') {
      const loader = isTeacher ? getCounselings({ studentId: id }) : getPublicCounselings(id)
      loader.then(setCounselings).catch(() => setCounselings([]))
    }
  }, [id, tab, year, semester, isTeacher])

  const handleSaveRecord = async () => {
    try {
      await updateRecord(id, {
        attendance: { present: Number(noteForm.present), absent: Number(noteForm.absent), late: Number(noteForm.late) },
      })
      setEditingNote(false)
    } catch { /* 에러 무시 */ }
  }

  const handleAddNote = async () => {
    if (!noteInput.trim()) return
    try {
      const created = await addRecordNote(id, noteInput.trim())
      setNotes((prev) => [...prev, created])
      setNoteInput('')
    } catch { /* 에러 무시 */ }
  }

  const handleUpdateNote = async (noteId) => {
    if (!editNoteText.trim()) return
    try {
      const updated = await updateRecordNote(noteId, editNoteText.trim())
      setNotes((prev) => prev.map((n) => (n.id === noteId ? updated : n)))
      setEditNoteId(null); setEditNoteText('')
    } catch { /* 에러 무시 */ }
  }

  const handleDeleteNote = async (noteId) => {
    try {
      await deleteRecordNote(noteId)
      setNotes((prev) => prev.filter((n) => n.id !== noteId))
    } catch { /* 에러 무시 */ }
  }

  const handleAddFeedback = async () => {
    if (!fbForm.content.trim()) return
    try {
      const created = await createFeedback(id, fbForm)
      setFeedbacks((prev) => [created, ...prev])
      setFbModal(false)
      setFbForm({ category: 'GRADE', content: '', isPublic: true })
    } catch { /* 에러 무시 */ }
  }

  const handleDeleteFeedback = async (fbId) => {
    if (!confirm('피드백을 삭제하시겠습니까?')) return
    try {
      await deleteFeedback(fbId)
      setFeedbacks((prev) => prev.filter((f) => f.id !== fbId))
    } catch { /* 에러 무시 */ }
  }

  const handleAddCounseling = async () => {
    if (!csForm.date || !csForm.content.trim()) return
    try {
      const created = await createCounseling({ ...csForm, studentId: Number(id) })
      setCounselings((prev) => [created, ...prev])
      setCsModal(false)
      setCsForm({ date: '', content: '', nextPlan: '', shareScope: 'ALL' })
    } catch { /* 에러 무시 */ }
  }

  const handleDeleteCounseling = async (csId) => {
    if (!confirm('상담 내역을 삭제하시겠습니까?')) return
    try {
      await deleteCounseling(csId)
      setCounselings((prev) => prev.filter((c) => c.id !== csId))
    } catch { /* 에러 무시 */ }
  }

  const handleAddParent = async () => {
    const pid = Number(parentIdInput.trim())
    if (!pid) return
    setParentSaving(true)
    try {
      const updated = await addParent(id, pid)
      setStudent(updated)
      setParentIdInput('')
    } catch { /* 에러 무시 */ }
    finally { setParentSaving(false) }
  }

  const handleRemoveParent = async (parentUserId) => {
    if (!confirm('학부모 연동을 해제하시겠습니까?')) return
    try {
      const updated = await removeParent(id, parentUserId)
      setStudent(updated)
    } catch { /* 에러 무시 */ }
  }

  const avg = grades.length
    ? (grades.reduce((s, g) => s + Number(g.score ?? 0), 0) / grades.length).toFixed(1)
    : '—'

  const useBar = typeof window !== 'undefined' && window.innerWidth < 640
  const radarData = grades.map((g) => ({ subject: g.subject?.name, score: Number(g.score ?? 0) }))

  if (loading) return (
    <div className="flex items-center justify-center h-64">
      <svg className="w-8 h-8 animate-spin text-brand-500" fill="none" viewBox="0 0 24 24">
        <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
        <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
      </svg>
    </div>
  )

  if (!student) return (
    <div className="card p-8 text-center text-gray-500">학생을 찾을 수 없습니다.</div>
  )

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
          <div className="w-14 h-14 rounded-2xl bg-brand-700 text-white flex items-center justify-center text-2xl font-bold flex-shrink-0">
            {student.name[0]}
          </div>
          <div>
            <h1 className="text-xl font-bold text-gray-900">{student.name}</h1>
            <p className="text-sm text-gray-500 mt-0.5">
              {student.grade}학년 {student.classNum}반 {student.studentNum}번
            </p>
          </div>
        </div>
      </div>

      {/* 탭 */}
      <div className="flex gap-0 border-b border-gray-200">
        {TABS.map((t) => (
          <button key={t} onClick={() => setTab(t)}
            className={`px-5 py-2.5 text-sm transition-all duration-150 -mb-px ${
              tab === t
                ? 'text-brand-700 border-b-2 border-brand-600 font-semibold'
                : 'text-gray-500 hover:text-gray-800 border-b-2 border-transparent'
            }`}
          >{t}</button>
        ))}
      </div>

      {/* ── 탭: 성적 ── */}
      {tab === '성적' && (
        <div className="space-y-5 animate-fade-in">
          <div className="flex gap-3">
            <select value={year} onChange={(e) => setYear(e.target.value)} className="input w-28 h-9 py-1.5">
              {[2025, 2024, 2023].map((y) => <option key={y}>{y}</option>)}
            </select>
            <select value={semester} onChange={(e) => setSemester(e.target.value)} className="input w-24 h-9 py-1.5">
              <option value="1">1학기</option><option value="2">2학기</option>
            </select>
          </div>
          {grades.length === 0 ? (
            <div className="card p-8 text-center text-gray-400">성적 데이터가 없습니다.</div>
          ) : (
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-5">
              <div className="card p-6">
                <h3 className="font-semibold text-gray-900 mb-4">성적 레이더 차트</h3>
                <ResponsiveContainer width="100%" height={280}>
                  {useBar ? (
                    <BarChart data={radarData} layout="vertical">
                      <CartesianGrid strokeDasharray="3 3" stroke="#e7e5e4" />
                      <XAxis type="number" domain={[0, 100]} tick={{ fontSize: 12 }} />
                      <YAxis dataKey="subject" type="category" tick={{ fontSize: 12, fill: '#78716c' }} width={36} />
                      <Tooltip formatter={(v) => [`${v}점`]} />
                      <Bar dataKey="score" fill="#14b8a6" radius={[0, 4, 4, 0]} />
                    </BarChart>
                  ) : (
                    <RadarChart data={radarData}>
                      <PolarGrid stroke="#e7e5e4" />
                      <PolarAngleAxis dataKey="subject" tick={{ fontSize: 12, fill: '#78716c' }} />
                      <Radar name="성적" dataKey="score" stroke="#0f766e" fill="#14b8a6" fillOpacity={0.2} dot={{ r: 3, fill: '#0f766e' }} />
                      <Tooltip formatter={(v) => [`${v}점`, '점수']} />
                    </RadarChart>
                  )}
                </ResponsiveContainer>
              </div>
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
                    {grades.map((g) => (
                      <tr key={g.id} className="border-b border-gray-50 last:border-0">
                        <td className="table-cell font-medium text-gray-800">{g.subject?.name}</td>
                        <td className="table-cell text-right font-mono text-gray-900">{g.score}</td>
                        <td className={`table-cell text-center font-mono ${rankColor(g.gradeRank)}`}>{g.gradeRank}</td>
                      </tr>
                    ))}
                    <tr className="bg-brand-50/40">
                      <td className="table-cell font-semibold text-gray-900">평균</td>
                      <td className="table-cell text-right font-mono font-semibold text-gray-900">{avg}</td>
                      <td className="table-cell text-center">—</td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>
          )}
        </div>
      )}

      {/* ── 탭: 학생부 ── */}
      {tab === '학생부' && (
        <div className="space-y-5 animate-fade-in">
          {record ? (
            <div className="card p-6">
              <h3 className="font-semibold text-gray-900 mb-4">출결 현황</h3>
              {editingNote ? (
                <div className="space-y-3">
                  <div className="grid grid-cols-3 gap-3">
                    {['present', 'absent', 'late'].map((k) => (
                      <div key={k}>
                        <label className="text-xs text-gray-500 mb-1 block">{{ present: '출석', absent: '결석', late: '지각' }[k]}</label>
                        <input type="number" value={noteForm[k]} onChange={(e) => setNoteForm((f) => ({ ...f, [k]: e.target.value }))} className="input h-9" />
                      </div>
                    ))}
                  </div>
                  <div className="flex justify-end gap-2">
                    <button onClick={() => setEditingNote(false)} className="btn-sm btn-secondary">취소</button>
                    <button onClick={handleSaveRecord} className="btn-sm btn-primary">저장</button>
                  </div>
                </div>
              ) : (
                <>
                  <div className="grid grid-cols-3 gap-4 mb-4">
                    {[
                      { label: '출석', key: 'present', color: 'text-green-600' },
                      { label: '결석', key: 'absent',  color: 'text-red-500'   },
                      { label: '지각', key: 'late',    color: 'text-amber-600' },
                    ].map(({ label, key, color }) => {
                      const att = record.attendance ? JSON.parse(record.attendance) : {}
                      return (
                        <div key={key} className="bg-bg rounded-xl p-4 text-center border border-gray-100">
                          <p className="text-xs text-gray-500 mb-1">{label}</p>
                          <p className={`text-2xl font-bold ${color}`}>{att[key] ?? 0}</p>
                        </div>
                      )
                    })}
                  </div>
                  {isTeacher && (
                    <div className="flex justify-end mt-3">
                      <button onClick={() => setEditingNote(true)} className="btn-sm btn-secondary">출결 수정</button>
                    </div>
                  )}
                </>
              )}
            </div>
          ) : (
            <div className="card p-8 text-center text-gray-400">
              {isTeacher ? (
                <button onClick={() => setEditingNote(true)} className="btn-md btn-primary">학생부 작성하기</button>
              ) : '학생부가 없습니다.'}
            </div>
          )}

          {/* 특기사항 다항목 */}
          <div className="card p-6">
            <h3 className="font-semibold text-gray-900 mb-4">특기사항</h3>
            {notes.length === 0 ? (
              <p className="text-sm text-gray-400">등록된 특기사항이 없습니다.</p>
            ) : (
              <ul className="space-y-2">
                {notes.map((n) => (
                  <li key={n.id} className="flex items-start gap-2 p-3 rounded-lg border border-gray-100">
                    {editNoteId === n.id ? (
                      <>
                        <input value={editNoteText} onChange={(e) => setEditNoteText(e.target.value)} className="input h-9 flex-1" />
                        <button onClick={() => handleUpdateNote(n.id)} className="btn-sm btn-primary">저장</button>
                        <button onClick={() => { setEditNoteId(null); setEditNoteText('') }} className="btn-sm btn-secondary">취소</button>
                      </>
                    ) : (
                      <>
                        <p className="text-sm text-gray-700 leading-relaxed flex-1">{n.content}</p>
                        {isTeacher && (
                          <div className="flex gap-1 flex-shrink-0">
                            <button onClick={() => { setEditNoteId(n.id); setEditNoteText(n.content) }} className="btn-sm btn-ghost px-2 text-xs">수정</button>
                            <button onClick={() => handleDeleteNote(n.id)} className="btn-sm text-red-500 hover:bg-red-50 rounded-md px-2 text-xs font-medium">삭제</button>
                          </div>
                        )}
                      </>
                    )}
                  </li>
                ))}
              </ul>
            )}
            {isTeacher && (
              <div className="flex gap-2 mt-4">
                <input
                  value={noteInput}
                  onChange={(e) => setNoteInput(e.target.value)}
                  onKeyDown={(e) => { if (e.key === 'Enter') handleAddNote() }}
                  placeholder="특기사항 추가 (예: 교내 수학경시 대상)"
                  className="input h-9 flex-1"
                />
                <button onClick={handleAddNote} className="btn-sm btn-primary">추가</button>
              </div>
            )}
          </div>
        </div>
      )}

      {/* ── 탭: 피드백 ── */}
      {tab === '피드백' && (
        <div className="space-y-4 animate-fade-in">
          {isTeacher && (
            <div className="flex justify-end">
              <button onClick={() => setFbModal(true)} className="btn-md btn-primary gap-2">
                <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" strokeWidth={2} stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" d="M12 4.5v15m7.5-7.5h-15" />
                </svg>
                피드백 추가
              </button>
            </div>
          )}
          {fbModal && (
            <div className="card p-5 border-2 border-brand-200 space-y-3">
              <h3 className="font-semibold text-gray-900">피드백 작성</h3>
              <select value={fbForm.category} onChange={(e) => setFbForm((f) => ({ ...f, category: e.target.value }))} className="input">
                {FEEDBACK_CATEGORIES.map((c) => <option key={c} value={c}>{catLabel[c]}</option>)}
              </select>
              <textarea rows={3} placeholder="피드백 내용" value={fbForm.content}
                onChange={(e) => setFbForm((f) => ({ ...f, content: e.target.value }))}
                className="input resize-none" />
              <label className="flex items-center gap-2 text-sm text-gray-700 cursor-pointer">
                <input type="checkbox" checked={fbForm.isPublic} onChange={(e) => setFbForm((f) => ({ ...f, isPublic: e.target.checked }))} />
                학생/학부모에게 공개
              </label>
              <div className="flex justify-end gap-2">
                <button onClick={() => setFbModal(false)} className="btn-sm btn-secondary">취소</button>
                <button onClick={handleAddFeedback} className="btn-sm btn-primary">저장</button>
              </div>
            </div>
          )}
          {feedbacks.length === 0 ? (
            <div className="card p-8 text-center text-gray-400">피드백이 없습니다.</div>
          ) : feedbacks.map((f) => (
            <div key={f.id} className="card p-5">
              <div className="flex items-start justify-between gap-3 mb-3">
                <div className="flex items-center gap-2 flex-wrap">
                  <span className={`badge ${catColorMap[f.category] ?? 'badge-gray'}`}>{catLabel[f.category]}</span>
                  <span className="text-xs text-gray-400">{f.createdAt?.substring(0, 10)}</span>
                  <span className="text-xs text-gray-500">{f.teacher?.name}</span>
                </div>
                <div className="flex items-center gap-1.5 flex-shrink-0">
                  <span className={`badge ${f.isPublic ? 'badge-green' : 'badge-gray'}`}>
                    {f.isPublic ? '공개' : '비공개'}
                  </span>
                  {isTeacher && (
                    <button onClick={() => handleDeleteFeedback(f.id)}
                      className="btn-sm text-red-500 hover:bg-red-50 rounded-md px-2 text-sm font-medium transition-colors">
                      삭제
                    </button>
                  )}
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
          {isTeacher && (
            <div className="flex justify-end">
              <button onClick={() => setCsModal(true)} className="btn-md btn-primary gap-2">
                <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" strokeWidth={2} stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" d="M12 4.5v15m7.5-7.5h-15" />
                </svg>
                상담 추가
              </button>
            </div>
          )}
          {csModal && (
            <div className="card p-5 border-2 border-brand-200 space-y-3">
              <h3 className="font-semibold text-gray-900">상담 등록</h3>
              <input type="date" value={csForm.date} onChange={(e) => setCsForm((f) => ({ ...f, date: e.target.value }))} className="input" />
              <textarea rows={3} placeholder="상담 내용" value={csForm.content}
                onChange={(e) => setCsForm((f) => ({ ...f, content: e.target.value }))}
                className="input resize-none" />
              <input placeholder="다음 계획 (선택)" value={csForm.nextPlan}
                onChange={(e) => setCsForm((f) => ({ ...f, nextPlan: e.target.value }))}
                className="input" />
              <select value={csForm.shareScope} onChange={(e) => setCsForm((f) => ({ ...f, shareScope: e.target.value }))} className="input">
                <option value="ALL">전체 공개</option>
                <option value="PRIVATE">비공개</option>
              </select>
              <div className="flex justify-end gap-2">
                <button onClick={() => setCsModal(false)} className="btn-sm btn-secondary">취소</button>
                <button onClick={handleAddCounseling} className="btn-sm btn-primary">저장</button>
              </div>
            </div>
          )}
          {counselings.length === 0 ? (
            <div className="card p-8 text-center text-gray-400">상담 내역이 없습니다.</div>
          ) : counselings.map((c) => (
            <div key={c.id} className="card p-5">
              <div className="flex items-start justify-between gap-3 mb-3">
                <div className="flex items-center gap-2 flex-wrap">
                  <span className="text-sm font-semibold text-gray-900">{c.date}</span>
                  <span className="text-sm text-gray-500">{c.teacher?.name}</span>
                </div>
                <div className="flex items-center gap-1.5 flex-shrink-0">
                  <span className={`badge ${c.shareScope === 'ALL' ? 'badge-green' : 'badge-gray'}`}>
                    {c.shareScope === 'ALL' ? '전체공개' : '비공개'}
                  </span>
                  {isTeacher && (
                    <button onClick={() => handleDeleteCounseling(c.id)}
                      className="btn-sm text-red-500 hover:bg-red-50 rounded-md px-2 text-sm font-medium transition-colors">
                      삭제
                    </button>
                  )}
                </div>
              </div>
              <p className="text-sm text-gray-700 leading-relaxed mb-3">{c.content}</p>
              {c.nextPlan && (
                <div className="bg-brand-50 border border-brand-100 rounded-lg px-4 py-3">
                  <p className="text-xs font-medium text-brand-700 mb-0.5">다음 계획</p>
                  <p className="text-sm text-brand-800">{c.nextPlan}</p>
                </div>
              )}
            </div>
          ))}
        </div>
      )}
      {/* ── 탭: 학부모 (교사 전용) ── */}
      {tab === '학부모' && isTeacher && (
        <div className="space-y-4 animate-fade-in">
          <div className="card p-6">
            <h3 className="font-semibold text-gray-900 mb-4">연동된 학부모 계정</h3>
            {student.parents?.length === 0 ? (
              <p className="text-sm text-gray-400 mb-4">연동된 학부모 계정이 없습니다.</p>
            ) : (
              <ul className="divide-y divide-gray-100 mb-4">
                {student.parents?.map((p) => (
                  <li key={p.id} className="flex items-center justify-between py-3">
                    <div>
                      <span className="font-medium text-gray-900 text-sm">{p.name}</span>
                      <span className="text-gray-400 text-xs ml-2">{p.email}</span>
                    </div>
                    <button
                      onClick={() => handleRemoveParent(p.id)}
                      className="btn-sm text-red-500 hover:bg-red-50 rounded-md px-2 text-xs font-medium transition-colors"
                    >
                      연동 해제
                    </button>
                  </li>
                ))}
              </ul>
            )}
            <div className="flex gap-2">
              <input
                type="number"
                value={parentIdInput}
                onChange={(e) => setParentIdInput(e.target.value)}
                placeholder="학부모 계정 User ID 입력"
                className="input flex-1 h-9 py-1.5"
              />
              <button
                onClick={handleAddParent}
                disabled={parentSaving || !parentIdInput}
                className="btn-md btn-primary px-4"
              >
                {parentSaving ? '연동 중...' : '연동'}
              </button>
            </div>
            <p className="text-xs text-gray-400 mt-2">
              사용자 관리 페이지에서 PARENT 역할 계정의 ID를 확인하세요.
            </p>
          </div>
        </div>
      )}
    </div>
  )
}
