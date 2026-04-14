import { useState, useEffect, useCallback } from 'react'
import { getReportPreview, downloadReport } from '../../api/reports'

const REPORT_TYPES = [
  { id: 'grade-summary',    label: '성적 종합 보고서',  desc: '학급 전체 또는 개인별 성적 현황 및 추이',  icon: IconChart   },
  { id: 'student-record',   label: '학생부 보고서',      desc: '출결, 특이사항, 행동 특성 종합',          icon: IconClip    },
  { id: 'feedback-report',  label: '피드백 현황 보고서', desc: '피드백 카테고리별 통계 및 목록',          icon: IconChat    },
  { id: 'counseling-report',label: '상담 이력 보고서',   desc: '상담 일자별, 학생별 상담 내역 종합',      icon: IconCounsel },
]

const CURRENT_YEAR = new Date().getFullYear()

export default function Reports() {
  const [selectedType, setSelectedType] = useState('grade-summary')
  const [year,         setYear]         = useState(String(CURRENT_YEAR))
  const [semester,     setSemester]     = useState('1')
  const [gradeFilter,  setGradeFilter]  = useState('전체')
  const [classFilter,  setClassFilter]  = useState('전체')

  const [preview,    setPreview]    = useState(null)   // { columns, rows, totalCount, generatedAt }
  const [loading,    setLoading]    = useState(false)
  const [generating, setGenerating] = useState(null)   // 'excel' | 'pdf' | null
  const [error,      setError]      = useState(null)

  // 미리보기 로드
  const loadPreview = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const params = {
        grade:    gradeFilter  !== '전체' ? gradeFilter  : undefined,
        classNum: classFilter  !== '전체' ? classFilter  : undefined,
        year:     ['grade-summary'].includes(selectedType) ? year     : undefined,
        semester: ['grade-summary'].includes(selectedType) ? semester : undefined,
      }
      const data = await getReportPreview(selectedType, params)
      setPreview(data)
    } catch {
      setError('데이터를 불러오지 못했습니다.')
      setPreview(null)
    } finally {
      setLoading(false)
    }
  }, [selectedType, year, semester, gradeFilter, classFilter])

  useEffect(() => { loadPreview() }, [loadPreview])

  // 파일 다운로드
  const handleDownload = async (format) => {
    setGenerating(format)
    try {
      const params = {
        grade:    gradeFilter !== '전체' ? gradeFilter  : undefined,
        classNum: classFilter !== '전체' ? classFilter  : undefined,
        year:     ['grade-summary'].includes(selectedType) ? year     : undefined,
        semester: ['grade-summary'].includes(selectedType) ? semester : undefined,
      }
      await downloadReport(selectedType, params, format)
    } catch {
      alert('다운로드 중 오류가 발생했습니다.')
    } finally {
      setGenerating(null)
    }
  }

  const selected = REPORT_TYPES.find((r) => r.id === selectedType)
  const columns  = preview?.columns ?? []
  const rows     = preview?.rows    ?? []

  return (
    <div className="space-y-5 animate-fade-in">
      <h1 className="text-2xl font-bold text-gray-900">보고서 생성</h1>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-5">
        {/* 보고서 종류 선택 */}
        <div className="space-y-3">
          <p className="text-sm font-medium text-gray-600">보고서 종류</p>
          {REPORT_TYPES.map(({ id, label, desc, icon: Icon }) => (
            <button
              key={id}
              onClick={() => setSelectedType(id)}
              className={`w-full text-left card p-4 flex items-start gap-3 transition-all duration-150 hover:shadow-card-hover ${selectedType === id ? 'border-primary-500 ring-2 ring-primary-100' : ''}`}
            >
              <div className={`w-9 h-9 rounded-xl flex items-center justify-center flex-shrink-0 ${selectedType === id ? 'bg-primary-700 text-white' : 'bg-gray-100 text-gray-500'}`}>
                <Icon className="w-5 h-5" />
              </div>
              <div>
                <p className={`text-sm font-semibold ${selectedType === id ? 'text-primary-700' : 'text-gray-900'}`}>{label}</p>
                <p className="text-xs text-gray-400 mt-0.5 leading-snug">{desc}</p>
              </div>
            </button>
          ))}
        </div>

        {/* 옵션 + 미리보기 */}
        <div className="lg:col-span-2 space-y-4">
          {/* 조건 설정 */}
          <div className="card p-5">
            <p className="text-sm font-semibold text-gray-900 mb-4">조건 설정</p>
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
              {/* 성적 보고서에서만 연도/학기 표시 */}
              {selectedType === 'grade-summary' && (
                <>
                  <div>
                    <label className="block text-xs text-gray-500 mb-1">연도</label>
                    <select value={year} onChange={(e) => setYear(e.target.value)} className="input h-9 py-1.5 text-sm">
                      {[CURRENT_YEAR, CURRENT_YEAR - 1, CURRENT_YEAR - 2].map((y) => (
                        <option key={y} value={y}>{y}</option>
                      ))}
                    </select>
                  </div>
                  <div>
                    <label className="block text-xs text-gray-500 mb-1">학기</label>
                    <select value={semester} onChange={(e) => setSemester(e.target.value)} className="input h-9 py-1.5 text-sm">
                      <option value="1">1학기</option>
                      <option value="2">2학기</option>
                    </select>
                  </div>
                </>
              )}
              <div>
                <label className="block text-xs text-gray-500 mb-1">학년</label>
                <select value={gradeFilter} onChange={(e) => setGradeFilter(e.target.value)} className="input h-9 py-1.5 text-sm">
                  <option value="전체">전체</option>
                  {[1, 2, 3].map((g) => <option key={g} value={g}>{g}학년</option>)}
                </select>
              </div>
              <div>
                <label className="block text-xs text-gray-500 mb-1">반</label>
                <select value={classFilter} onChange={(e) => setClassFilter(e.target.value)} className="input h-9 py-1.5 text-sm">
                  <option value="전체">전체</option>
                  {[1, 2, 3, 4, 5, 6].map((c) => <option key={c} value={c}>{c}반</option>)}
                </select>
              </div>
            </div>
          </div>

          {/* 미리보기 */}
          <div className="card overflow-hidden">
            <div className="px-5 py-4 border-b border-gray-100 flex items-center justify-between">
              <div>
                <p className="font-semibold text-gray-900 text-sm">{selected?.label}</p>
                <p className="text-xs text-gray-400 mt-0.5">
                  {gradeFilter !== '전체' ? `${gradeFilter}학년` : '전학년'}
                  {classFilter !== '전체' ? ` ${classFilter}반` : ' 전체'}
                  {selectedType === 'grade-summary' ? ` · ${year}년 ${semester}학기` : ''}
                </p>
              </div>
              <span className="badge badge-blue">미리보기</span>
            </div>

            {/* 테이블 */}
            {loading ? (
              <div className="flex items-center justify-center h-40 text-sm text-gray-400">불러오는 중...</div>
            ) : error ? (
              <div className="flex items-center justify-center h-40 text-sm text-red-400">{error}</div>
            ) : rows.length === 0 ? (
              <div className="flex items-center justify-center h-40 text-sm text-gray-400">해당 조건의 데이터가 없습니다.</div>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-sm">
                  <thead>
                    <tr className="table-header border-b border-gray-100">
                      {columns.map((col) => (
                        <th key={col.key} className={`table-cell ${col.align === 'right' ? 'text-right' : col.align === 'center' ? 'text-center' : 'text-left'}`}>
                          {col.label}
                        </th>
                      ))}
                    </tr>
                  </thead>
                  <tbody>
                    {rows.map((row, i) => (
                      <tr key={i} className="table-row">
                        {columns.map((col) => (
                          <td key={col.key} className={`table-cell ${col.align === 'right' ? 'text-right font-mono' : col.align === 'center' ? 'text-center' : ''}`}>
                            {row[col.key] ?? '-'}
                          </td>
                        ))}
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}

            {/* 하단 액션 */}
            <div className="px-5 py-4 border-t border-gray-100 bg-gray-50 flex items-center justify-between gap-3">
              <p className="text-xs text-gray-400">
                총 {preview?.totalCount ?? 0}명
                {preview?.generatedAt && ` · 기준일: ${preview.generatedAt}`}
              </p>
              <div className="flex gap-2">
                <button
                  onClick={() => handleDownload('excel')}
                  disabled={!!generating || loading || rows.length === 0}
                  className="btn-sm btn-secondary gap-1.5 disabled:opacity-50"
                >
                  {generating === 'excel' ? <Spinner /> : <IconDownload />}
                  Excel
                </button>
                <button
                  onClick={() => handleDownload('pdf')}
                  disabled={!!generating || loading || rows.length === 0}
                  className="btn-sm btn-primary gap-1.5 disabled:opacity-50"
                >
                  {generating === 'pdf' ? <Spinner /> : <IconPdf />}
                  PDF
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}

/* ── 아이콘 & 공통 컴포넌트 ── */
function Spinner() {
  return <svg className="w-4 h-4 animate-spin" fill="none" viewBox="0 0 24 24"><circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"/><path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"/></svg>
}
function IconDownload() {
  return <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" d="M3 16.5v2.25A2.25 2.25 0 005.25 21h13.5A2.25 2.25 0 0021 18.75V16.5M16.5 12L12 16.5m0 0L7.5 12m4.5 4.5V3" /></svg>
}
function IconPdf() {
  return <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" d="M19.5 14.25v-2.625a3.375 3.375 0 00-3.375-3.375h-1.5A1.125 1.125 0 0113.5 7.125v-1.5a3.375 3.375 0 00-3.375-3.375H8.25m2.25 0H5.625c-.621 0-1.125.504-1.125 1.125v17.25c0 .621.504 1.125 1.125 1.125h12.75c.621 0 1.125-.504 1.125-1.125V11.25a9 9 0 00-9-9z" /></svg>
}
function IconChart({ className }) {
  return <svg className={className} fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" d="M3 13.125C3 12.504 3.504 12 4.125 12h2.25c.621 0 1.125.504 1.125 1.125v6.75C7.5 20.496 6.996 21 6.375 21h-2.25A1.125 1.125 0 013 19.875v-6.75zM9.75 8.625c0-.621.504-1.125 1.125-1.125h2.25c.621 0 1.125.504 1.125 1.125v11.25c0 .621-.504 1.125-1.125 1.125h-2.25a1.125 1.125 0 01-1.125-1.125V8.625zM16.5 4.125c0-.621.504-1.125 1.125-1.125h2.25C20.496 3 21 3.504 21 4.125v15.75c0 .621-.504 1.125-1.125 1.125h-2.25a1.125 1.125 0 01-1.125-1.125V4.125z" /></svg>
}
function IconClip({ className }) {
  return <svg className={className} fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" d="M9 12h3.75M9 15h3.75M9 18h3.75m3 .75H18a2.25 2.25 0 002.25-2.25V6.108c0-1.135-.845-2.098-1.976-2.192a48.424 48.424 0 00-1.123-.08m-5.801 0c-.065.21-.1.433-.1.664 0 .414.336.75.75.75h4.5a.75.75 0 00.75-.75 2.25 2.25 0 00-.1-.664m-5.8 0A2.251 2.251 0 0113.5 2.25H15c1.012 0 1.867.668 2.15 1.586m-5.8 0c-.376.023-.75.05-1.124.08C9.095 4.01 8.25 4.973 8.25 6.108V8.25m0 0H4.875c-.621 0-1.125.504-1.125 1.125v11.25c0 .621.504 1.125 1.125 1.125h9.75c.621 0 1.125-.504 1.125-1.125V9.375c0-.621-.504-1.125-1.125-1.125H8.25z" /></svg>
}
function IconChat({ className }) {
  return <svg className={className} fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" d="M7.5 8.25h9m-9 3H12m-9.75 1.51c0 1.6 1.123 2.994 2.707 3.227 1.129.166 2.27.293 3.423.379.35.026.67.21.865.501L12 21l2.755-4.133a1.14 1.14 0 01.865-.501 48.172 48.172 0 003.423-.379c1.584-.233 2.707-1.626 2.707-3.228V6.741c0-1.602-1.123-2.995-2.707-3.228A48.394 48.394 0 0012 3c-2.392 0-4.744.175-7.043.513C3.373 3.746 2.25 5.14 2.25 6.741v6.018z" /></svg>
}
function IconCounsel({ className }) {
  return <svg className={className} fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" d="M6.75 3v2.25M17.25 3v2.25M3 18.75V7.5a2.25 2.25 0 012.25-2.25h13.5A2.25 2.25 0 0121 7.5v11.25m-18 0A2.25 2.25 0 005.25 21h13.5A2.25 2.25 0 0021 18.75m-18 0v-7.5A2.25 2.25 0 015.25 9h13.5A2.25 2.25 0 0121 11.25v7.5" /></svg>
}
