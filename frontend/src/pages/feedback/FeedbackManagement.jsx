import { useState, useEffect, useCallback } from 'react'
import { getStudents, getMyStudent, getMyChildren } from '../../api/students'
import { getFeedbacks, createFeedback, updateFeedback, deleteFeedback } from '../../api/feedbacks'
import useAuthStore from '../../store/authStore'

const CATEGORY_MAP     = { GRADE: '성적', BEHAVIOR: '행동', ATTENDANCE: '출결', ATTITUDE: '태도', OTHER: '기타' }
const CATEGORY_REVERSE = { '성적': 'GRADE', '행동': 'BEHAVIOR', '출결': 'ATTENDANCE', '태도': 'ATTITUDE', '기타': 'OTHER' }
const catColor         = { 성적: 'badge-blue', 행동: 'badge-green', 태도: 'badge-purple', 출결: 'badge-amber', 기타: 'badge-gray' }
const CATEGORIES       = ['전체', '성적', '행동', '태도', '출결', '기타']
const EMPTY_FORM       = { studentId: '', category: '성적', content: '', isPublic: false }

// 피드백 카드 (읽기 전용)
function FeedbackCard({ f, studentName, onToggle, onEdit, onDelete, editable }) {
  const catKo = CATEGORY_MAP[f.category] || f.category
  return (
    <div className="card p-5">
      <div className="flex items-start justify-between gap-3 mb-2.5">
        <div className="flex items-center gap-2 flex-wrap">
          <div className="w-7 h-7 rounded-full bg-primary-100 text-primary-700 flex items-center justify-center text-xs font-semibold">
            {(f.studentName || studentName || '?')[0]}
          </div>
          <span className="font-semibold text-gray-900 text-sm">{f.studentName || studentName}</span>
          <span className={`badge ${catColor[catKo] || 'badge-gray'}`}>{catKo}</span>
          <span className="text-xs text-gray-400">{f.createdAt?.slice(0, 10)}</span>
          <span className="text-xs text-gray-400">· {f.teacherName}</span>
        </div>
        {editable ? (
          <div className="flex items-center gap-1.5 flex-shrink-0">
            <button
              onClick={() => onToggle(f)}
              className={`badge cursor-pointer transition-colors ${f.isPublic ? 'badge-green' : 'badge-gray'}`}
            >
              {f.isPublic ? '공개' : '비공개'}
            </button>
            <button onClick={() => onEdit(f)} className="btn-sm btn-ghost px-2 text-xs">수정</button>
            <button onClick={() => onDelete(f.id)} className="btn-sm text-red-500 hover:bg-red-50 rounded-md px-2 text-xs font-medium transition-colors">삭제</button>
          </div>
        ) : (
          <span className={`badge ${f.isPublic ? 'badge-green' : 'badge-gray'}`}>공개</span>
        )}
      </div>
      <p className="text-sm text-gray-700 leading-relaxed">{f.content}</p>
    </div>
  )
}

// 교사용 뷰 (전체 관리)
function TeacherFeedbackView() {
  const [students,      setStudents]      = useState([])
  const [feedbacks,     setFeedbacks]     = useState([])
  const [loading,       setLoading]       = useState(false)
  const [catFilter,     setCatFilter]     = useState('전체')
  const [pubFilter,     setPubFilter]     = useState('전체')
  const [studentFilter, setStudentFilter] = useState('')
  const [modal,         setModal]         = useState(false)
  const [editTarget,    setEditTarget]    = useState(null)
  const [form,          setForm]          = useState(EMPTY_FORM)
  const [saving,        setSaving]        = useState(false)
  const [deleteId,      setDeleteId]      = useState(null)

  useEffect(() => {
    getStudents().then((data) => setStudents(data || [])).catch(() => {})
  }, [])

  const loadFeedbacks = useCallback(async (sid) => {
    if (!sid) { setFeedbacks([]); return }
    setLoading(true)
    try { setFeedbacks((await getFeedbacks(sid)) || []) }
    catch { setFeedbacks([]) }
    finally { setLoading(false) }
  }, [])

  useEffect(() => { loadFeedbacks(studentFilter) }, [studentFilter, loadFeedbacks])

  const filtered = feedbacks
    .filter((f) => catFilter === '전체' || CATEGORY_MAP[f.category] === catFilter)
    .filter((f) => pubFilter === '전체' || (pubFilter === '공개' ? f.isPublic : !f.isPublic))

  const openCreate = () => { setEditTarget(null); setForm({ ...EMPTY_FORM, studentId: studentFilter }); setModal(true) }
  const openEdit   = (f) => {
    setEditTarget(f)
    setForm({ studentId: String(f.studentId), category: CATEGORY_MAP[f.category] || '기타', content: f.content, isPublic: f.isPublic })
    setModal(true)
  }

  const handleSave = async () => {
    if (!form.studentId || !form.content) return
    setSaving(true)
    try {
      const body = { category: CATEGORY_REVERSE[form.category] || 'OTHER', content: form.content, isPublic: form.isPublic }
      editTarget ? await updateFeedback(editTarget.id, body) : await createFeedback(form.studentId, body)
      setModal(false); setForm(EMPTY_FORM); setEditTarget(null)
      await loadFeedbacks(studentFilter || form.studentId)
    } finally { setSaving(false) }
  }

  const handleDelete = async (id) => {
    await deleteFeedback(id).catch(() => {})
    setDeleteId(null)
    await loadFeedbacks(studentFilter)
  }

  const handleTogglePublic = async (f) => {
    await updateFeedback(f.id, { category: f.category, content: f.content, isPublic: !f.isPublic }).catch(() => {})
    await loadFeedbacks(studentFilter)
  }

  const selectedStudent = students.find((s) => String(s.id) === String(studentFilter))

  return (
    <div className="space-y-5 animate-fade-in">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-gray-900">피드백 관리</h1>
        <button onClick={openCreate} className="btn-md btn-primary gap-2">
          <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" strokeWidth={2} stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" d="M12 4.5v15m7.5-7.5h-15" />
          </svg>
          피드백 작성
        </button>
      </div>

      <div className="card p-4 flex flex-wrap gap-3 items-center">
        <select value={studentFilter} onChange={(e) => setStudentFilter(e.target.value)} className="input w-40 h-9 py-1.5">
          <option value="">학생 선택</option>
          {students.map((s) => (
            <option key={s.id} value={s.id}>{s.name} ({s.grade}학년 {s.classNum}반)</option>
          ))}
        </select>
        <div className="flex gap-1 bg-gray-100 p-1 rounded-lg">
          {CATEGORIES.map((c) => (
            <button key={c} onClick={() => setCatFilter(c)}
              className={`px-3 py-1.5 rounded-md text-xs font-medium transition-all ${catFilter === c ? 'bg-white text-gray-900 shadow-card' : 'text-gray-500 hover:text-gray-700'}`}
            >{c}</button>
          ))}
        </div>
        <select value={pubFilter} onChange={(e) => setPubFilter(e.target.value)} className="input w-28 h-9 py-1.5">
          <option>전체</option><option>공개</option><option>비공개</option>
        </select>
        <span className="text-sm text-gray-400">총 {filtered.length}건</span>
      </div>

      <div className="space-y-3">
        {!studentFilter ? (
          <div className="card p-12 text-center text-gray-400">학생을 선택하면 피드백 목록이 표시됩니다.</div>
        ) : loading ? (
          <div className="card p-12 text-center text-gray-400">불러오는 중...</div>
        ) : filtered.length === 0 ? (
          <div className="card p-12 text-center text-gray-400">피드백이 없습니다.</div>
        ) : filtered.map((f) => (
          <FeedbackCard key={f.id} f={f} studentName={selectedStudent?.name}
            onToggle={handleTogglePublic} onEdit={openEdit} onDelete={setDeleteId} editable />
        ))}
      </div>

      {/* 작성/수정 모달 */}
      {modal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div className="absolute inset-0 bg-black/50 backdrop-blur-sm" onClick={() => setModal(false)} />
          <div className="relative bg-white rounded-2xl shadow-modal w-full max-w-lg p-6 animate-slide-up">
            <div className="flex items-center justify-between mb-5">
              <h2 className="text-lg font-bold text-gray-900">{editTarget ? '피드백 수정' : '피드백 작성'}</h2>
              <button onClick={() => setModal(false)} className="p-1.5 rounded-lg hover:bg-gray-100 text-gray-400">
                <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" /></svg>
              </button>
            </div>
            <div className="space-y-4">
              {!editTarget && (
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1.5">학생 선택</label>
                  <select value={form.studentId} onChange={(e) => setForm((f) => ({ ...f, studentId: e.target.value }))} className="input">
                    <option value="">학생을 선택하세요</option>
                    {students.map((s) => (
                      <option key={s.id} value={s.id}>{s.name} ({s.grade}학년 {s.classNum}반)</option>
                    ))}
                  </select>
                </div>
              )}
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1.5">카테고리</label>
                <div className="flex gap-2 flex-wrap">
                  {['성적', '행동', '출결', '태도', '기타'].map((c) => (
                    <button key={c} onClick={() => setForm((f) => ({ ...f, category: c }))}
                      className={`px-3 py-1.5 rounded-lg text-sm border transition-all ${form.category === c ? 'bg-primary-700 text-white border-primary-700' : 'border-gray-200 text-gray-600 hover:border-primary-300'}`}
                    >{c}</button>
                  ))}
                </div>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1.5">내용</label>
                <textarea rows={4} value={form.content} onChange={(e) => setForm((f) => ({ ...f, content: e.target.value }))}
                  placeholder="피드백 내용을 입력하세요." className="input resize-none" />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">공개 여부</label>
                <div className="flex gap-4">
                  {[{ v: true, l: '공개 (학생·학부모 열람 가능)' }, { v: false, l: '비공개 (교사만)' }].map(({ v, l }) => (
                    <label key={l} className="flex items-center gap-2 cursor-pointer">
                      <input type="radio" checked={form.isPublic === v} onChange={() => setForm((f) => ({ ...f, isPublic: v }))} className="accent-primary-700" />
                      <span className="text-sm text-gray-700">{l}</span>
                    </label>
                  ))}
                </div>
              </div>
            </div>
            <div className="flex justify-end gap-2 mt-6">
              <button onClick={() => setModal(false)} className="btn-md btn-secondary">취소</button>
              <button onClick={handleSave} disabled={saving} className="btn-md btn-primary">{saving ? '저장 중...' : '저장'}</button>
            </div>
          </div>
        </div>
      )}

      {/* 삭제 확인 모달 */}
      {deleteId && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div className="absolute inset-0 bg-black/50" onClick={() => setDeleteId(null)} />
          <div className="relative bg-white rounded-2xl shadow-modal w-full max-w-sm p-6 animate-slide-up text-center">
            <div className="w-12 h-12 bg-red-100 rounded-full flex items-center justify-center mx-auto mb-4">
              <svg className="w-6 h-6 text-red-500" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" d="M14.74 9l-.346 9m-4.788 0L9.26 9m9.968-3.21c.342.052.682.107 1.022.166m-1.022-.165L18.16 19.673a2.25 2.25 0 01-2.244 2.077H8.084a2.25 2.25 0 01-2.244-2.077L4.772 5.79m14.456 0a48.108 48.108 0 00-3.478-.397m-12 .562c.34-.059.68-.114 1.022-.165m0 0a48.11 48.11 0 013.478-.397m7.5 0v-.916c0-1.18-.91-2.164-2.09-2.201a51.964 51.964 0 00-3.32 0c-1.18.037-2.09 1.022-2.09 2.201v.916m7.5 0a48.667 48.667 0 00-7.5 0" /></svg>
            </div>
            <p className="font-semibold text-gray-900 mb-1">피드백 삭제</p>
            <p className="text-sm text-gray-500 mb-5">삭제하면 복구할 수 없습니다.</p>
            <div className="flex gap-2">
              <button onClick={() => setDeleteId(null)} className="btn-md btn-secondary flex-1">취소</button>
              <button onClick={() => handleDelete(deleteId)} className="btn-md btn-danger flex-1">삭제</button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

// 학생/학부모용 뷰 (읽기 전용)
function StudentParentFeedbackView({ role }) {
  const isStudent = role === 'STUDENT'
  const isParent  = role === 'PARENT'

  const [children,    setChildren]    = useState([])        // PARENT: 자녀 목록
  const [studentInfo, setStudentInfo] = useState(null)
  const [selectedId,  setSelectedId]  = useState(null)      // PARENT: 선택된 자녀 ID
  const [feedbacks,   setFeedbacks]   = useState([])
  const [loading,     setLoading]     = useState(false)
  const [catFilter,   setCatFilter]   = useState('전체')
  const [error,       setError]       = useState('')

  // STUDENT: 자신의 학생 정보 자동 로드
  useEffect(() => {
    if (!isStudent) return
    getMyStudent()
      .then((s) => { setStudentInfo(s); })
      .catch(() => setError('연동된 학생 정보를 찾을 수 없습니다. 담임 선생님에게 문의하세요.'))
  }, [isStudent])

  // PARENT: 자녀 목록 로드
  useEffect(() => {
    if (!isParent) return
    getMyChildren()
      .then((list) => {
        setChildren(list || [])
        if (list?.length === 1) setSelectedId(list[0].id)
      })
      .catch(() => setError('자녀 정보를 불러올 수 없습니다. 담임 선생님에게 연동을 요청하세요.'))
  }, [isParent])

  const loadFeedbacks = useCallback(async (sid) => {
    if (!sid) return
    setLoading(true)
    try { setFeedbacks((await getFeedbacks(sid)) || []) }
    catch { setFeedbacks([]) }
    finally { setLoading(false) }
  }, [])

  useEffect(() => {
    if (isStudent && studentInfo?.id) loadFeedbacks(studentInfo.id)
  }, [isStudent, studentInfo, loadFeedbacks])

  useEffect(() => {
    if (isParent && selectedId) {
      const child = children.find((c) => c.id === selectedId)
      setStudentInfo(child || null)
      loadFeedbacks(selectedId)
    }
  }, [isParent, selectedId, children, loadFeedbacks])

  const filtered = feedbacks.filter((f) => catFilter === '전체' || CATEGORY_MAP[f.category] === catFilter)

  return (
    <div className="space-y-5 animate-fade-in">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">내 피드백</h1>
          {studentInfo && (
            <p className="text-sm text-gray-500 mt-0.5">
              {studentInfo.name} · {studentInfo.grade}학년 {studentInfo.classNum}반 {studentInfo.studentNum}번
            </p>
          )}
        </div>
      </div>

      {error ? (
        <div className="card p-6 text-center text-red-500">{error}</div>
      ) : (
        <>
          <div className="card p-4 flex flex-wrap gap-3 items-center">
            {/* PARENT: 자녀 선택 드롭다운 */}
            {isParent && children.length > 1 && (
              <select
                value={selectedId ?? ''}
                onChange={(e) => setSelectedId(Number(e.target.value))}
                className="input w-44 h-9 py-1.5"
              >
                <option value="">자녀 선택</option>
                {children.map((c) => (
                  <option key={c.id} value={c.id}>{c.name} ({c.grade}학년 {c.classNum}반)</option>
                ))}
              </select>
            )}
            <div className="flex gap-1 bg-gray-100 p-1 rounded-lg">
              {CATEGORIES.map((c) => (
                <button key={c} onClick={() => setCatFilter(c)}
                  className={`px-3 py-1.5 rounded-md text-xs font-medium transition-all ${catFilter === c ? 'bg-white text-gray-900 shadow-card' : 'text-gray-500 hover:text-gray-700'}`}
                >{c}</button>
              ))}
            </div>
            <span className="text-sm text-gray-400">총 {filtered.length}건</span>
          </div>

          <div className="space-y-3">
            {isParent && !selectedId ? (
              <div className="card p-12 text-center text-gray-400">
                {children.length === 0 ? '연동된 자녀 정보가 없습니다. 담임 선생님에게 문의하세요.' : '자녀를 선택하세요.'}
              </div>
            ) : loading ? (
              <div className="card p-12 text-center text-gray-400">불러오는 중...</div>
            ) : filtered.length === 0 ? (
              <div className="card p-12 text-center text-gray-400">피드백이 없습니다.</div>
            ) : filtered.map((f) => (
              <FeedbackCard key={f.id} f={f} studentName={studentInfo?.name} editable={false} />
            ))}
          </div>
        </>
      )}
    </div>
  )
}

export default function FeedbackManagement() {
  const { user } = useAuthStore()
  if (user?.role === 'TEACHER') return <TeacherFeedbackView />
  return <StudentParentFeedbackView role={user?.role} />
}
