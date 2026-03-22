import { useState } from 'react'
import { useNavigate } from 'react-router-dom'

const MOCK_STUDENTS = [
  { id: 1,  name: '홍길동', grade: 2, classNum: 3, studentNum: 1,  avg: 85.4, hasAlert: true  },
  { id: 2,  name: '김철수', grade: 2, classNum: 3, studentNum: 2,  avg: 72.1, hasAlert: false },
  { id: 3,  name: '이영희', grade: 2, classNum: 3, studentNum: 3,  avg: 91.8, hasAlert: false },
  { id: 4,  name: '박민준', grade: 2, classNum: 3, studentNum: 4,  avg: 68.5, hasAlert: true  },
  { id: 5,  name: '최수진', grade: 2, classNum: 3, studentNum: 5,  avg: 88.2, hasAlert: false },
  { id: 6,  name: '정하늘', grade: 2, classNum: 3, studentNum: 6,  avg: 77.9, hasAlert: false },
  { id: 7,  name: '강지훈', grade: 2, classNum: 3, studentNum: 7,  avg: 94.1, hasAlert: false },
  { id: 8,  name: '윤서연', grade: 2, classNum: 3, studentNum: 8,  avg: 63.0, hasAlert: true  },
  { id: 9,  name: '임도현', grade: 2, classNum: 3, studentNum: 9,  avg: 80.6, hasAlert: false },
  { id: 10, name: '한소희', grade: 2, classNum: 3, studentNum: 10, avg: 87.3, hasAlert: false },
  { id: 11, name: '오태양', grade: 1, classNum: 2, studentNum: 1,  avg: 75.5, hasAlert: false },
  { id: 12, name: '서지아', grade: 1, classNum: 2, studentNum: 2,  avg: 90.2, hasAlert: false },
]

function gradeLabel(avg) {
  if (avg >= 90) return { label: 'A',  cls: 'badge-green'  }
  if (avg >= 80) return { label: 'B',  cls: 'badge-blue'   }
  if (avg >= 70) return { label: 'C',  cls: 'badge-amber'  }
  return               { label: 'D',  cls: 'badge-red'    }
}

export default function StudentList() {
  const navigate = useNavigate()
  const [search, setSearch]       = useState('')
  const [gradeFilter, setGradeFilter] = useState('전체')
  const [classFilter, setClassFilter] = useState('전체')
  const [sortKey, setSortKey]     = useState('studentNum')
  const [sortAsc, setSortAsc]     = useState(true)

  const toggleSort = (key) => {
    if (sortKey === key) setSortAsc((v) => !v)
    else { setSortKey(key); setSortAsc(false) }
  }

  const filtered = MOCK_STUDENTS
    .filter((s) => gradeFilter === '전체' || String(s.grade) === gradeFilter)
    .filter((s) => classFilter === '전체' || String(s.classNum) === classFilter)
    .filter((s) => s.name.includes(search))
    .sort((a, b) => {
      const v = sortAsc ? 1 : -1
      if (sortKey === 'avg') return (a.avg - b.avg) * v
      if (sortKey === 'name') return a.name.localeCompare(b.name) * v
      return (a.studentNum - b.studentNum) * v
    })

  const SortIcon = ({ col }) => (
    <span className="ml-1 inline-block">
      {sortKey === col ? (sortAsc ? '↑' : '↓') : <span className="text-gray-300">↕</span>}
    </span>
  )

  return (
    <div className="space-y-5 animate-fade-in">
      {/* 헤더 */}
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-gray-900">학생 목록</h1>
        <span className="text-sm text-gray-400">총 {filtered.length}명</span>
      </div>

      {/* 필터 바 */}
      <div className="card p-4 flex flex-wrap gap-3 items-center">
        <select
          value={gradeFilter}
          onChange={(e) => setGradeFilter(e.target.value)}
          className="input w-28 h-9 py-1.5"
        >
          <option>전체</option>
          <option value="1">1학년</option>
          <option value="2">2학년</option>
          <option value="3">3학년</option>
        </select>
        <select
          value={classFilter}
          onChange={(e) => setClassFilter(e.target.value)}
          className="input w-24 h-9 py-1.5"
        >
          <option>전체</option>
          {[1,2,3,4,5,6].map((c) => <option key={c} value={c}>{c}반</option>)}
        </select>
        <div className="relative flex-1 min-w-48">
          <svg className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" d="M21 21l-5.197-5.197m0 0A7.5 7.5 0 105.196 5.196a7.5 7.5 0 0010.607 10.607z" />
          </svg>
          <input
            placeholder="학생 이름 검색"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="input pl-9 h-9 py-1.5"
          />
        </div>
        <div className="ml-auto flex gap-2">
          <button className="btn-sm btn-secondary gap-1.5">
            <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" d="M3 16.5v2.25A2.25 2.25 0 005.25 21h13.5A2.25 2.25 0 0021 18.75V16.5M16.5 12L12 16.5m0 0L7.5 12m4.5 4.5V3" /></svg>
            Excel
          </button>
          <button className="btn-sm btn-secondary gap-1.5">
            <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" d="M19.5 14.25v-2.625a3.375 3.375 0 00-3.375-3.375h-1.5A1.125 1.125 0 0113.5 7.125v-1.5a3.375 3.375 0 00-3.375-3.375H8.25m2.25 0H5.625c-.621 0-1.125.504-1.125 1.125v17.25c0 .621.504 1.125 1.125 1.125h12.75c.621 0 1.125-.504 1.125-1.125V11.25a9 9 0 00-9-9z" /></svg>
            PDF
          </button>
        </div>
      </div>

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
              <th className="table-cell cursor-pointer select-none text-right" onClick={() => toggleSort('avg')}>
                평균 성적 <SortIcon col="avg" />
              </th>
              <th className="table-cell text-center">등급</th>
              <th className="table-cell text-center w-16">알림</th>
              <th className="table-cell text-center w-24">액션</th>
            </tr>
          </thead>
          <tbody>
            {filtered.length === 0 ? (
              <tr>
                <td colSpan={8} className="table-cell text-center text-gray-400 py-12">
                  검색 결과가 없습니다.
                </td>
              </tr>
            ) : (
              filtered.map((s, i) => {
                const { label, cls } = gradeLabel(s.avg)
                return (
                  <tr key={s.id} className="table-row">
                    <td className="table-cell text-center text-gray-400 font-mono text-xs">{i + 1}</td>
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
                    <td className="table-cell text-right font-mono font-medium text-gray-900">{s.avg.toFixed(1)}</td>
                    <td className="table-cell text-center">
                      <span className={`badge ${cls}`}>{label}</span>
                    </td>
                    <td className="table-cell text-center">
                      {s.hasAlert && (
                        <span className="inline-flex w-5 h-5 items-center justify-center rounded-full bg-red-100 text-red-600 text-[10px] font-bold">!</span>
                      )}
                    </td>
                    <td className="table-cell text-center">
                      <button
                        onClick={() => navigate(`/students/${s.id}`)}
                        className="btn-sm btn-ghost text-primary-700 px-3"
                      >
                        상세보기
                      </button>
                    </td>
                  </tr>
                )
              })
            )}
          </tbody>
        </table>
      </div>
    </div>
  )
}
