import { useState } from 'react'

const MOCK = [
  { id: 1, student: '홍길동', category: '성적',  date: '2025-03-15', teacher: '김선생님', content: '수학 3단원 이해도가 낮습니다. 추가 학습을 권장합니다.', isPublic: true  },
  { id: 2, student: '김철수', category: '행동',  date: '2025-03-10', teacher: '김선생님', content: '수업 중 집중력이 향상됨. 계속 격려 필요.',             isPublic: false },
  { id: 3, student: '이영희', category: '태도',  date: '2025-03-01', teacher: '박선생님', content: '발표력이 뛰어나며 친구들과 협력 잘 함.',              isPublic: true  },
  { id: 4, student: '박민준', category: '출결',  date: '2025-02-25', teacher: '김선생님', content: '지각이 잦아지고 있음. 가정과 연계 필요.',             isPublic: false },
  { id: 5, student: '최수진', category: '기타',  date: '2025-02-20', teacher: '김선생님', content: '학교 행사 준비에 적극적으로 참여함.',                 isPublic: true  },
]

const catColor = { 성적: 'badge-blue', 행동: 'badge-green', 태도: 'badge-purple', 출결: 'badge-amber', 기타: 'badge-gray' }
const CATEGORIES = ['전체', '성적', '행동', '태도', '출결', '기타']

const EMPTY_FORM = { student: '', category: '성적', content: '', isPublic: false }

export default function FeedbackManagement() {
  const [list, setList]         = useState(MOCK)
  const [catFilter, setCatFilter] = useState('전체')
  const [pubFilter, setPubFilter] = useState('전체')
  const [search, setSearch]     = useState('')
  const [modal, setModal]       = useState(false)
  const [form, setForm]         = useState(EMPTY_FORM)
  const [deleteId, setDeleteId] = useState(null)

  const filtered = list
    .filter((f) => catFilter === '전체' || f.category === catFilter)
    .filter((f) => pubFilter === '전체' || (pubFilter === '공개' ? f.isPublic : !f.isPublic))
    .filter((f) => f.student.includes(search))

  const handleAdd = () => {
    if (!form.student || !form.content) return
    setList((prev) => [{ id: Date.now(), teacher: '김선생님', date: new Date().toISOString().slice(0, 10), ...form }, ...prev])
    setModal(false)
    setForm(EMPTY_FORM)
  }

  const handleDelete = (id) => {
    setList((prev) => prev.filter((f) => f.id !== id))
    setDeleteId(null)
  }

  const togglePublic = (id) => {
    setList((prev) => prev.map((f) => f.id === id ? { ...f, isPublic: !f.isPublic } : f))
  }

  return (
    <div className="space-y-5 animate-fade-in">
      {/* 헤더 */}
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-gray-900">피드백 관리</h1>
        <button onClick={() => setModal(true)} className="btn-md btn-primary gap-2">
          <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" strokeWidth={2} stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" d="M12 4.5v15m7.5-7.5h-15" />
          </svg>
          피드백 작성
        </button>
      </div>

      {/* 필터 */}
      <div className="card p-4 flex flex-wrap gap-3 items-center">
        <div className="flex gap-1 bg-gray-100 p-1 rounded-lg">
          {CATEGORIES.map((c) => (
            <button
              key={c}
              onClick={() => setCatFilter(c)}
              className={`px-3 py-1.5 rounded-md text-xs font-medium transition-all ${catFilter === c ? 'bg-white text-gray-900 shadow-card' : 'text-gray-500 hover:text-gray-700'}`}
            >
              {c}
            </button>
          ))}
        </div>
        <select value={pubFilter} onChange={(e) => setPubFilter(e.target.value)} className="input w-28 h-9 py-1.5">
          <option>전체</option><option>공개</option><option>비공개</option>
        </select>
        <div className="relative flex-1 min-w-40">
          <svg className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" d="M21 21l-5.197-5.197m0 0A7.5 7.5 0 105.196 5.196a7.5 7.5 0 0010.607 10.607z" />
          </svg>
          <input placeholder="학생 이름 검색" value={search} onChange={(e) => setSearch(e.target.value)} className="input pl-9 h-9 py-1.5" />
        </div>
        <span className="text-sm text-gray-400">총 {filtered.length}건</span>
      </div>

      {/* 목록 */}
      <div className="space-y-3">
        {filtered.length === 0 ? (
          <div className="card p-12 text-center text-gray-400">피드백이 없습니다.</div>
        ) : filtered.map((f) => (
          <div key={f.id} className="card p-5">
            <div className="flex items-start justify-between gap-3 mb-2.5">
              <div className="flex items-center gap-2 flex-wrap">
                <div className="w-7 h-7 rounded-full bg-primary-100 text-primary-700 flex items-center justify-center text-xs font-semibold">{f.student[0]}</div>
                <span className="font-semibold text-gray-900 text-sm">{f.student}</span>
                <span className={`badge ${catColor[f.category]}`}>{f.category}</span>
                <span className="text-xs text-gray-400">{f.date}</span>
                <span className="text-xs text-gray-400">· {f.teacher}</span>
              </div>
              <div className="flex items-center gap-1.5 flex-shrink-0">
                <button onClick={() => togglePublic(f.id)} className={`badge cursor-pointer transition-colors ${f.isPublic ? 'badge-green' : 'badge-gray'}`}>
                  {f.isPublic ? '공개' : '비공개'}
                </button>
                <button className="btn-sm btn-ghost px-2 text-xs">수정</button>
                <button onClick={() => setDeleteId(f.id)} className="btn-sm text-red-500 hover:bg-red-50 rounded-md px-2 text-xs font-medium transition-colors">삭제</button>
              </div>
            </div>
            <p className="text-sm text-gray-700 leading-relaxed">{f.content}</p>
          </div>
        ))}
      </div>

      {/* 작성 모달 */}
      {modal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div className="absolute inset-0 bg-black/50 backdrop-blur-sm" onClick={() => setModal(false)} />
          <div className="relative bg-white rounded-2xl shadow-modal w-full max-w-lg p-6 animate-slide-up">
            <div className="flex items-center justify-between mb-5">
              <h2 className="text-lg font-bold text-gray-900">피드백 작성</h2>
              <button onClick={() => setModal(false)} className="p-1.5 rounded-lg hover:bg-gray-100 text-gray-400">
                <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" /></svg>
              </button>
            </div>
            <div className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1.5">학생 선택</label>
                <select value={form.student} onChange={(e) => setForm((f) => ({ ...f, student: e.target.value }))} className="input">
                  <option value="">학생을 선택하세요</option>
                  {['홍길동','김철수','이영희','박민준','최수진'].map((s) => <option key={s}>{s}</option>)}
                </select>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1.5">카테고리</label>
                <div className="flex gap-2 flex-wrap">
                  {['성적','행동','출결','태도','기타'].map((c) => (
                    <button key={c} onClick={() => setForm((f) => ({ ...f, category: c }))}
                      className={`px-3 py-1.5 rounded-lg text-sm border transition-all ${form.category === c ? 'bg-primary-700 text-white border-primary-700' : 'border-gray-200 text-gray-600 hover:border-primary-300'}`}
                    >{c}</button>
                  ))}
                </div>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1.5">내용</label>
                <textarea rows={4} value={form.content} onChange={(e) => setForm((f) => ({ ...f, content: e.target.value }))} placeholder="피드백 내용을 입력하세요." className="input resize-none" />
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
              <button onClick={handleAdd} className="btn-md btn-primary">저장</button>
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
