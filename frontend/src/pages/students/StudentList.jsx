import { useState, useEffect, useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import { getStudentsPaged } from '../../api/students'

const PAGE_SIZE = 20

export default function StudentList() {
  const navigate = useNavigate()

  const [students, setStudents]       = useState([])
  const [loading, setLoading]         = useState(true)
  const [error, setError]             = useState('')
  const [search, setSearch]           = useState('')
  const [gradeFilter, setGradeFilter] = useState('')
  const [classFilter, setClassFilter] = useState('')
  const [sortKey, setSortKey]         = useState('studentNum')
  const [sortAsc, setSortAsc]         = useState(true)

  // 페이지 상태
  const [page, setPage]               = useState(0)
  const [totalPages, setTotalPages]   = useState(0)
  const [totalElements, setTotalEls]  = useState(0)

  const fetchStudents = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      const params = { page, size: PAGE_SIZE, sort: `${sortKey},${sortAsc ? 'asc' : 'desc'}` }
      if (gradeFilter) params.grade = gradeFilter
      if (classFilter) params.classNum = classFilter
      if (search)      params.keyword = search
      const data = await getStudentsPaged(params)
      setStudents(data?.content ?? [])
      setTotalPages(data?.totalPages ?? 0)
      setTotalEls(data?.totalElements ?? 0)
    } catch {
      setError('학생 목록을 불러오지 못했습니다.')
    } finally {
      setLoading(false)
    }
  }, [gradeFilter, classFilter, search, page, sortKey, sortAsc])

  // 필터 변경 시 첫 페이지로 리셋
  useEffect(() => {
    setPage(0)
  }, [gradeFilter, classFilter, search])

  useEffect(() => {
    const timer = setTimeout(fetchStudents, 300) // 검색어 디바운스
    return () => clearTimeout(timer)
  }, [fetchStudents])

  const toggleSort = (key) => {
    if (sortKey === key) setSortAsc((v) => !v)
    else { setSortKey(key); setSortAsc(true) }
  }

  const SortIcon = ({ col }) => (
    <span className="ml-1 inline-block">
      {sortKey === col ? (sortAsc ? '↑' : '↓') : <span className="text-gray-300">↕</span>}
    </span>
  )

  return (
    <div className="space-y-5 animate-fade-in">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-gray-900">학생 목록</h1>
        <span className="text-sm text-gray-400">총 {totalElements}명</span>
      </div>

      {/* 필터 바 */}
      <div className="card p-4 flex flex-wrap gap-3 items-center" role="search" aria-label="학생 검색 필터">
        <select
          aria-label="학년 필터"
          value={gradeFilter}
          onChange={(e) => setGradeFilter(e.target.value)}
          className="input w-28 h-9 py-1.5"
        >
          <option value="">전체 학년</option>
          <option value="1">1학년</option>
          <option value="2">2학년</option>
          <option value="3">3학년</option>
        </select>
        <select
          aria-label="반 필터"
          value={classFilter}
          onChange={(e) => setClassFilter(e.target.value)}
          className="input w-24 h-9 py-1.5"
        >
          <option value="">전체 반</option>
          {[1,2,3,4,5,6].map((c) => <option key={c} value={c}>{c}반</option>)}
        </select>
        <div className="relative flex-1 min-w-48">
          <svg aria-hidden="true" className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" d="M21 21l-5.197-5.197m0 0A7.5 7.5 0 105.196 5.196a7.5 7.5 0 0010.607 10.607z" />
          </svg>
          <input
            aria-label="학생 이름 검색"
            placeholder="학생 이름 검색"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="input pl-9 h-9 py-1.5"
          />
        </div>
      </div>

      {/* 에러 */}
      {error && (
        <div className="card p-4 text-red-600 text-sm bg-red-50 border border-red-200">{error}</div>
      )}

      {/* 테이블 */}
      <div className="card overflow-hidden">
        <table className="w-full">
          <thead>
            <tr className="table-header border-b border-gray-200">
              <th className="table-cell w-12 text-center">#</th>
              <th className="table-cell cursor-pointer select-none" onClick={() => toggleSort('name')}>
                이름 <SortIcon col="name" />
              </th>
              <th className="table-cell text-center">학년</th>
              <th className="table-cell text-center">반</th>
              <th className="table-cell cursor-pointer select-none text-center" onClick={() => toggleSort('studentNum')}>
                번호 <SortIcon col="studentNum" />
              </th>
              <th className="table-cell text-center w-24">액션</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr>
                <td colSpan={6} className="table-cell text-center text-gray-400 py-12">
                  <svg aria-label="로딩 중" className="w-6 h-6 animate-spin mx-auto text-primary-500" fill="none" viewBox="0 0 24 24">
                    <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                    <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
                  </svg>
                </td>
              </tr>
            ) : students.length === 0 ? (
              <tr>
                <td colSpan={6} className="table-cell text-center text-gray-400 py-12">
                  {search || gradeFilter || classFilter ? '검색 결과가 없습니다.' : '등록된 학생이 없습니다.'}
                </td>
              </tr>
            ) : (
              students.map((s, i) => (
                <tr key={s.id} className="table-row">
                  <td className="table-cell text-center text-gray-400 font-mono text-xs">{page * PAGE_SIZE + i + 1}</td>
                  <td className="table-cell">
                    <div className="flex items-center gap-2.5">
                      <div className="w-8 h-8 rounded-full bg-primary-100 text-primary-700 flex items-center justify-center text-sm font-semibold flex-shrink-0">
                        {s.name[0]}
                      </div>
                      <span className="font-medium text-gray-900">{s.name}</span>
                    </div>
                  </td>
                  <td className="table-cell text-center">{s.grade}학년</td>
                  <td className="table-cell text-center">{s.classNum}반</td>
                  <td className="table-cell text-center">{s.studentNum}번</td>
                  <td className="table-cell text-center">
                    <button
                      onClick={() => navigate(`/students/${s.id}`)}
                      className="btn-sm btn-ghost text-primary-700 px-3"
                    >
                      상세보기
                    </button>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {/* 페이지네이션 */}
      {totalPages > 1 && (
        <nav aria-label="페이지 네비게이션" className="flex items-center justify-center gap-2 pt-2">
          <button
            type="button"
            disabled={page === 0}
            onClick={() => setPage((p) => Math.max(0, p - 1))}
            className="btn-sm btn-ghost px-3 disabled:opacity-40 disabled:cursor-not-allowed"
            aria-label="이전 페이지"
          >
            이전
          </button>
          <span className="text-sm text-gray-600" aria-live="polite">
            {page + 1} / {totalPages}
          </span>
          <button
            type="button"
            disabled={page >= totalPages - 1}
            onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
            className="btn-sm btn-ghost px-3 disabled:opacity-40 disabled:cursor-not-allowed"
            aria-label="다음 페이지"
          >
            다음
          </button>
        </nav>
      )}
    </div>
  )
}
