import { useState, useEffect, useCallback } from 'react'
import { getStudents } from '../../api/students'
import { getCounselings, createCounseling, updateCounseling, deleteCounseling } from '../../api/counselings'

// 백엔드 enum ↔ 한국어 변환
const SCOPE_MAP     = { ALL: '전체공개', PRIVATE: '비공개' }
const SCOPE_REVERSE = { '전체공개': 'ALL', '비공개': 'PRIVATE' }

const EMPTY_FORM = { studentId: '', date: '', content: '', nextPlan: '', scope: '전체공개' }

export default function CounselingManagement() {
  const [students,   setStudents]   = useState([])
  const [list,       setList]       = useState([])
  const [loading,    setLoading]    = useState(true)
  const [view,       setView]       = useState('list')   // 'list' | 'calendar'
  const [search,     setSearch]     = useState('')
  const [month,      setMonth]      = useState(() => new Date().toISOString().slice(0, 7))
  const [modal,      setModal]      = useState(false)
  const [editTarget, setEditTarget] = useState(null)
  const [form,       setForm]       = useState(EMPTY_FORM)
  const [saving,     setSaving]     = useState(false)
  const [deleteId,   setDeleteId]   = useState(null)

  // 학생 목록 로드
  useEffect(() => {
    getStudents().then((data) => setStudents(data || [])).catch(() => {})
  }, [])

  // 상담 목록 로드
  const loadCounselings = useCallback(async () => {
    setLoading(true)
    try {
      const data = await getCounselings()
      setList(data || [])
    } catch {
      setList([])
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => { loadCounselings() }, [loadCounselings])

  // 필터링 & 정렬
  const filtered = list
    .filter((c) => {
      const name = c.studentName || ''
      return name.includes(search)
    })
    .sort((a, b) => b.date.localeCompare(a.date))

  // 달력용: 해당 월 날짜 생성
  const calDates = (() => {
    const [y, m] = month.split('-').map(Number)
    const first  = new Date(y, m - 1, 1).getDay()
    const days   = new Date(y, m, 0).getDate()
    return { first, days, entries: list.filter((c) => c.date?.startsWith(month)) }
  })()

  // 모달 열기
  const openCreate = () => {
    setEditTarget(null)
    setForm(EMPTY_FORM)
    setModal(true)
  }

  const openEdit = (c) => {
    setEditTarget(c)
    setForm({
      studentId: String(c.studentId),
      date:      c.date,
      content:   c.content,
      nextPlan:  c.nextPlan || '',
      scope:     SCOPE_MAP[c.shareScope] || '전체공개',
    })
    setModal(true)
  }

  const handleSave = async () => {
    if (!form.studentId || !form.date || !form.content) return
    setSaving(true)
    try {
      const body = {
        studentId:  Number(form.studentId),
        date:       form.date,
        content:    form.content,
        nextPlan:   form.nextPlan || null,
        shareScope: SCOPE_REVERSE[form.scope] || 'ALL',
      }
      if (editTarget) {
        await updateCounseling(editTarget.id, body)
      } else {
        await createCounseling(body)
      }
      setModal(false)
      setForm(EMPTY_FORM)
      setEditTarget(null)
      await loadCounselings()
    } finally {
      setSaving(false)
    }
  }

  const handleDelete = async (id) => {
    await deleteCounseling(id).catch(() => {})
    setDeleteId(null)
    await loadCounselings()
  }

  return (
    <div className="space-y-5 animate-fade-in">
      {/* 헤더 */}
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-gray-900">상담 관리</h1>
        <button onClick={openCreate} className="btn-md btn-primary gap-2">
          <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" strokeWidth={2} stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" d="M12 4.5v15m7.5-7.5h-15" />
          </svg>
          상담 기록
        </button>
      </div>

      {/* 필터 바 */}
      <div className="card p-4 flex flex-wrap gap-3 items-center">
        {/* 뷰 전환 */}
        <div className="flex gap-1 bg-gray-100 p-1 rounded-lg">
          {['list', 'calendar'].map((v) => (
            <button key={v} onClick={() => setView(v)}
              className={`px-3 py-1.5 rounded-md text-xs font-medium transition-all ${view === v ? 'bg-white text-gray-900 shadow-card' : 'text-gray-500 hover:text-gray-700'}`}
            >{v === 'list' ? '목록' : '캘린더'}</button>
          ))}
        </div>
        {view === 'calendar' && (
          <input type="month" value={month} onChange={(e) => setMonth(e.target.value)} className="input w-36 h-9 py-1.5" />
        )}
        <div className="relative flex-1 min-w-40">
          <svg className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" d="M21 21l-5.197-5.197m0 0A7.5 7.5 0 105.196 5.196a7.5 7.5 0 0010.607 10.607z" />
          </svg>
          <input placeholder="학생 이름 검색" value={search} onChange={(e) => setSearch(e.target.value)} className="input pl-9 h-9 py-1.5" />
        </div>
        <span className="text-sm text-gray-400">총 {filtered.length}건</span>
      </div>

      {/* 캘린더 뷰 */}
      {view === 'calendar' && (
        <div className="card p-5 animate-fade-in">
          <div className="grid grid-cols-7 gap-1 mb-2">
            {['일', '월', '화', '수', '목', '금', '토'].map((d) => (
              <div key={d} className="text-center text-xs font-semibold text-gray-400 py-1">{d}</div>
            ))}
          </div>
          <div className="grid grid-cols-7 gap-1">
            {Array.from({ length: calDates.first }).map((_, i) => <div key={`e${i}`} />)}
            {Array.from({ length: calDates.days }).map((_, i) => {
              const day     = i + 1
              const dateStr = `${month}-${String(day).padStart(2, '0')}`
              const entries = calDates.entries.filter((c) => c.date === dateStr)
              return (
                <div key={day} className={`min-h-[72px] p-1.5 rounded-lg border text-xs ${entries.length ? 'border-primary-200 bg-primary-50' : 'border-transparent'}`}>
                  <span className={`font-medium ${entries.length ? 'text-primary-700' : 'text-gray-600'}`}>{day}</span>
                  {entries.map((e) => (
                    <div key={e.id} className="mt-1 bg-primary-600 text-white rounded px-1 py-0.5 truncate leading-tight">
                      {e.studentName}
                    </div>
                  ))}
                </div>
              )
            })}
          </div>
        </div>
      )}

      {/* 목록 뷰 */}
      {view === 'list' && (
        <div className="space-y-3 animate-fade-in">
          {loading ? (
            <div className="card p-12 text-center text-gray-400">불러오는 중...</div>
          ) : filtered.length === 0 ? (
            <div className="card p-12 text-center text-gray-400">상담 기록이 없습니다.</div>
          ) : filtered.map((c) => {
            const scopeKo = SCOPE_MAP[c.shareScope] || c.shareScope
            return (
              <div key={c.id} className="card p-5">
                <div className="flex items-start justify-between gap-3 mb-2.5">
                  <div className="flex items-center gap-2 flex-wrap">
                    <div className="w-7 h-7 rounded-full bg-primary-100 text-primary-700 flex items-center justify-center text-xs font-semibold">
                      {(c.studentName || '?')[0]}
                    </div>
                    <span className="font-semibold text-gray-900 text-sm">{c.studentName}</span>
                    <span className="text-xs text-gray-400">{c.date}</span>
                    <span className="text-xs text-gray-400">· {c.teacherName}</span>
                  </div>
                  <div className="flex items-center gap-1.5 flex-shrink-0">
                    <span className={`badge ${scopeKo === '전체공개' ? 'badge-green' : 'badge-gray'}`}>{scopeKo}</span>
                    <button onClick={() => openEdit(c)} className="btn-sm btn-ghost px-2 text-xs">수정</button>
                    <button onClick={() => setDeleteId(c.id)} className="btn-sm text-red-500 hover:bg-red-50 rounded-md px-2 text-xs font-medium transition-colors">삭제</button>
                  </div>
                </div>
                <p className="text-sm text-gray-700 leading-relaxed mb-3">{c.content}</p>
                {c.nextPlan && (
                  <div className="bg-blue-50 border border-blue-100 rounded-lg px-4 py-2.5">
                    <p className="text-xs font-medium text-blue-700 mb-0.5">다음 계획</p>
                    <p className="text-sm text-blue-800">{c.nextPlan}</p>
                  </div>
                )}
              </div>
            )
          })}
        </div>
      )}

      {/* 작성/수정 모달 */}
      {modal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div className="absolute inset-0 bg-black/50 backdrop-blur-sm" onClick={() => setModal(false)} />
          <div className="relative bg-white rounded-2xl shadow-modal w-full max-w-lg p-6 animate-slide-up">
            <div className="flex items-center justify-between mb-5">
              <h2 className="text-lg font-bold text-gray-900">{editTarget ? '상담 수정' : '상담 기록'}</h2>
              <button onClick={() => setModal(false)} className="p-1.5 rounded-lg hover:bg-gray-100 text-gray-400">
                <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" /></svg>
              </button>
            </div>
            <div className="space-y-4">
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1.5">학생 선택</label>
                  <select
                    value={form.studentId}
                    onChange={(e) => setForm((f) => ({ ...f, studentId: e.target.value }))}
                    disabled={!!editTarget}
                    className="input disabled:bg-gray-50 disabled:text-gray-400"
                  >
                    <option value="">학생 선택</option>
                    {students.map((s) => (
                      <option key={s.id} value={s.id}>{s.name} ({s.grade}학년 {s.classNum}반)</option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1.5">상담 일자</label>
                  <input
                    type="date"
                    value={form.date}
                    onChange={(e) => setForm((f) => ({ ...f, date: e.target.value }))}
                    className="input"
                  />
                </div>
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1.5">상담 내용</label>
                <textarea
                  rows={4}
                  value={form.content}
                  onChange={(e) => setForm((f) => ({ ...f, content: e.target.value }))}
                  placeholder="상담 내용을 입력하세요."
                  className="input resize-none"
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1.5">
                  다음 계획 <span className="text-gray-400 font-normal">(선택)</span>
                </label>
                <input
                  value={form.nextPlan}
                  onChange={(e) => setForm((f) => ({ ...f, nextPlan: e.target.value }))}
                  placeholder="다음 상담 계획이나 조치 사항"
                  className="input"
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">공유 범위</label>
                <div className="flex gap-4">
                  {[{ v: '전체공개', l: '전체공개 (다른 교사 열람 가능)' }, { v: '비공개', l: '비공개 (본인만)' }].map(({ v, l }) => (
                    <label key={v} className="flex items-center gap-2 cursor-pointer">
                      <input
                        type="radio"
                        checked={form.scope === v}
                        onChange={() => setForm((f) => ({ ...f, scope: v }))}
                        className="accent-primary-700"
                      />
                      <span className="text-sm text-gray-700">{l}</span>
                    </label>
                  ))}
                </div>
              </div>
            </div>
            <div className="flex justify-end gap-2 mt-6">
              <button onClick={() => setModal(false)} className="btn-md btn-secondary">취소</button>
              <button onClick={handleSave} disabled={saving} className="btn-md btn-primary">
                {saving ? '저장 중...' : '저장'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* 삭제 확인 */}
      {deleteId && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div className="absolute inset-0 bg-black/50" onClick={() => setDeleteId(null)} />
          <div className="relative bg-white rounded-2xl shadow-modal w-full max-w-sm p-6 animate-slide-up text-center">
            <div className="w-12 h-12 bg-red-100 rounded-full flex items-center justify-center mx-auto mb-4">
              <svg className="w-6 h-6 text-red-500" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" d="M14.74 9l-.346 9m-4.788 0L9.26 9m9.968-3.21c.342.052.682.107 1.022.166m-1.022-.165L18.16 19.673a2.25 2.25 0 01-2.244 2.077H8.084a2.25 2.25 0 01-2.244-2.077L4.772 5.79m14.456 0a48.108 48.108 0 00-3.478-.397m-12 .562c.34-.059.68-.114 1.022-.165m0 0a48.11 48.11 0 013.478-.397m7.5 0v-.916c0-1.18-.91-2.164-2.09-2.201a51.964 51.964 0 00-3.32 0c-1.18.037-2.09 1.022-2.09 2.201v.916m7.5 0a48.667 48.667 0 00-7.5 0" /></svg>
            </div>
            <p className="font-semibold text-gray-900 mb-1">상담 기록 삭제</p>
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
