import { useState, useEffect, useCallback } from 'react'
import {
  ResponsiveContainer, LineChart, Line, BarChart, Bar,
  XAxis, YAxis, CartesianGrid, Tooltip,
} from 'recharts'
import {
  getOverview, getStudentSummary, getSubjectDistribution, runEtl,
  chatStatus, chat,
} from '../../api/analytics'

const CATEGORY_LABEL = {
  GRADE: '성적', BEHAVIOR: '행동', ATTENDANCE: '출결', ATTITUDE: '태도', OTHER: '기타',
}

function pct(v) {
  return `${Math.round((v || 0) * 100)}%`
}

export default function Analytics() {
  const [overview, setOverview]   = useState({ students: [], subjects: [] })
  const [studentId, setStudentId] = useState('')
  const [subjectId, setSubjectId] = useState('')
  const [summary, setSummary]     = useState(null)
  const [dist, setDist]           = useState(null)
  const [loading, setLoading]     = useState(true)
  const [etlRunning, setEtlRunning] = useState(false)
  const [etlMsg, setEtlMsg]       = useState('')

  const loadOverview = useCallback(async () => {
    setLoading(true)
    try {
      const ov = await getOverview()
      setOverview(ov || { students: [], subjects: [] })
      if (ov?.students?.length && !studentId) setStudentId(String(ov.students[0].id))
      if (ov?.subjects?.length && !subjectId) setSubjectId(String(ov.subjects[0].id))
    } catch {
      // axios 인터셉터에서 처리
    } finally {
      setLoading(false)
    }
  }, [studentId, subjectId])

  useEffect(() => { loadOverview() }, []) // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    if (!studentId) { setSummary(null); return }
    getStudentSummary(studentId).then(setSummary).catch(() => setSummary(null))
  }, [studentId])

  useEffect(() => {
    if (!subjectId) { setDist(null); return }
    getSubjectDistribution(subjectId).then(setDist).catch(() => setDist(null))
  }, [subjectId])

  const handleEtl = async () => {
    setEtlRunning(true)
    setEtlMsg('')
    try {
      const r = await runEtl()
      setEtlMsg(`적재 완료 — 성적 ${r.grades} · 제출 ${r.submissions} · 피드백 ${r.feedbacks}`)
      await loadOverview()
      if (studentId) getStudentSummary(studentId).then(setSummary).catch(() => {})
      if (subjectId) getSubjectDistribution(subjectId).then(setDist).catch(() => {})
    } catch {
      setEtlMsg('ETL 실행 실패 — 분석 DB 연결을 확인하세요.')
    } finally {
      setEtlRunning(false)
    }
  }

  const trendData = (summary?.gradeTrend || []).map((t) => ({
    term: `${t.year}-${t.semester}`,
    평균: Number(t.avgScore?.toFixed?.(1) ?? t.avgScore),
  }))
  const feedbackData = (summary?.feedbackByCategory || []).map((c) => ({
    category: CATEGORY_LABEL[c.category] || c.category,
    건수: c.count,
  }))
  const distData = (dist?.distribution || []).map((b) => ({ range: b.range, 인원: b.count }))

  return (
    <div className="space-y-6 animate-fade-in">
      {/* 제목 + ETL */}
      <div className="flex items-center justify-between flex-wrap gap-3">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">학습 분석</h1>
          <p className="text-sm text-gray-400 mt-0.5">운영 데이터를 분석 DB로 적재한 학생별·과목별 집계</p>
        </div>
        <div className="flex items-center gap-3">
          {etlMsg && <span className="text-xs text-gray-500">{etlMsg}</span>}
          <button
            onClick={handleEtl}
            disabled={etlRunning}
            className="btn-primary text-sm disabled:opacity-60"
          >
            {etlRunning ? '적재 중...' : 'ETL 실행'}
          </button>
        </div>
      </div>

      {loading ? (
        <div className="card p-10 text-center text-gray-400 text-sm">불러오는 중...</div>
      ) : overview.students.length === 0 ? (
        <div className="card p-10 text-center text-gray-400 text-sm">
          분석 데이터가 없습니다. 상단의 <b className="text-gray-600">ETL 실행</b> 버튼으로 적재하세요.
        </div>
      ) : (
        <>
          {/* ── 학생별 학습 현황 ── */}
          <div className="card p-6">
            <div className="flex items-center justify-between mb-5 flex-wrap gap-3">
              <h2 className="font-semibold text-gray-900">학생별 학습 현황</h2>
              <select
                value={studentId}
                onChange={(e) => setStudentId(e.target.value)}
                className="input text-sm w-48"
              >
                {overview.students.map((s) => (
                  <option key={s.id} value={s.id}>
                    {s.grade}학년 {s.classNum}반 · {s.name}
                  </option>
                ))}
              </select>
            </div>

            {!summary ? (
              <div className="h-40 flex items-center justify-center text-gray-400 text-sm">데이터 없음</div>
            ) : (
              <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                {/* 성적 추이 */}
                <div className="lg:col-span-2">
                  <p className="text-sm font-medium text-gray-700 mb-3">성적 추이 (학기 평균)</p>
                  <ResponsiveContainer width="100%" height={240}>
                    <LineChart data={trendData} margin={{ top: 8, right: 12, left: -12, bottom: 0 }}>
                      <CartesianGrid strokeDasharray="3 3" stroke="#eef2f7" />
                      <XAxis dataKey="term" tick={{ fontSize: 12 }} />
                      <YAxis domain={[0, 100]} tick={{ fontSize: 12 }} />
                      <Tooltip />
                      <Line type="monotone" dataKey="평균" stroke="#4f46e5" strokeWidth={2} dot={{ r: 4 }} />
                    </LineChart>
                  </ResponsiveContainer>
                </div>

                {/* 출석률 / 제출률 KPI */}
                <div className="space-y-4">
                  <KpiTile label="출석률" value={pct(summary.attendance.rate)}
                    sub={`출석 ${summary.attendance.present} · 결석 ${summary.attendance.absent} · 지각 ${summary.attendance.late}`}
                    color="bg-green-50 text-green-700" />
                  <KpiTile label="과제 제출률" value={pct(summary.submission.rate)}
                    sub={`제출 ${summary.submission.submitted} / 전체 ${summary.submission.total}`}
                    color="bg-blue-50 text-blue-700" />
                </div>

                {/* 피드백 분포 */}
                <div className="lg:col-span-3">
                  <p className="text-sm font-medium text-gray-700 mb-3">피드백 분포</p>
                  {feedbackData.length === 0 ? (
                    <div className="h-20 flex items-center justify-center text-gray-400 text-sm">피드백 없음</div>
                  ) : (
                    <ResponsiveContainer width="100%" height={200}>
                      <BarChart data={feedbackData} margin={{ top: 8, right: 12, left: -12, bottom: 0 }}>
                        <CartesianGrid strokeDasharray="3 3" stroke="#eef2f7" />
                        <XAxis dataKey="category" tick={{ fontSize: 12 }} />
                        <YAxis allowDecimals={false} tick={{ fontSize: 12 }} />
                        <Tooltip />
                        <Bar dataKey="건수" fill="#a855f7" radius={[4, 4, 0, 0]} />
                      </BarChart>
                    </ResponsiveContainer>
                  )}
                </div>
              </div>
            )}
          </div>

          {/* ── 과목별 학습 현황 ── */}
          <div className="card p-6">
            <div className="flex items-center justify-between mb-5 flex-wrap gap-3">
              <h2 className="font-semibold text-gray-900">과목별 학습 현황</h2>
              <select
                value={subjectId}
                onChange={(e) => setSubjectId(e.target.value)}
                className="input text-sm w-48"
              >
                {overview.subjects.map((s) => (
                  <option key={s.id} value={s.id}>{s.name}</option>
                ))}
              </select>
            </div>

            {!dist ? (
              <div className="h-40 flex items-center justify-center text-gray-400 text-sm">데이터 없음</div>
            ) : (
              <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                <div className="space-y-4">
                  <KpiTile label="과목 평균" value={dist.average?.toFixed?.(1) ?? dist.average}
                    sub="전체 학생 평균 점수" color="bg-amber-50 text-amber-700" />
                  <KpiTile label="과제 제출률" value={pct(dist.submission.rate)}
                    sub={`제출 ${dist.submission.submitted} / 전체 ${dist.submission.total}`}
                    color="bg-blue-50 text-blue-700" />
                </div>
                <div className="lg:col-span-2">
                  <p className="text-sm font-medium text-gray-700 mb-3">점수 분포</p>
                  <ResponsiveContainer width="100%" height={240}>
                    <BarChart data={distData} margin={{ top: 8, right: 12, left: -12, bottom: 0 }}>
                      <CartesianGrid strokeDasharray="3 3" stroke="#eef2f7" />
                      <XAxis dataKey="range" tick={{ fontSize: 12 }} />
                      <YAxis allowDecimals={false} tick={{ fontSize: 12 }} />
                      <Tooltip />
                      <Bar dataKey="인원" fill="#4f46e5" radius={[4, 4, 0, 0]} />
                    </BarChart>
                  </ResponsiveContainer>
                </div>
              </div>
            )}
          </div>

          {/* ── AI 챗봇 (선택) ── */}
          <ChatPanel studentId={studentId} students={overview.students} />
        </>
      )}
    </div>
  )
}

function ChatPanel({ studentId, students }) {
  const [enabled, setEnabled] = useState(null)
  const [messages, setMessages] = useState([])
  const [input, setInput] = useState('')
  const [scope, setScope] = useState('all') // 'all' | 'student'
  const [sending, setSending] = useState(false)

  useEffect(() => {
    chatStatus().then(setEnabled).catch(() => setEnabled(false))
  }, [])

  const send = async (e) => {
    e.preventDefault()
    const q = input.trim()
    if (!q || sending) return
    setInput('')
    setMessages((m) => [...m, { role: 'user', text: q }])
    setSending(true)
    try {
      const sid = scope === 'student' && studentId ? studentId : undefined
      const r = await chat(q, sid)
      setMessages((m) => [...m, { role: 'assistant', text: r.answer }])
    } catch {
      setMessages((m) => [...m, { role: 'assistant', text: '오류가 발생했습니다. 잠시 후 다시 시도하세요.' }])
    } finally {
      setSending(false)
    }
  }

  if (enabled === null) return null
  if (enabled === false) {
    return (
      <div className="card p-6">
        <h2 className="font-semibold text-gray-900 mb-2">AI 학습 분석 챗봇</h2>
        <p className="text-sm text-gray-400">
          비활성화됨 — 서버에 <code className="text-gray-600">LLM_API_KEY</code>를 설정하면 사용할 수 있습니다.
        </p>
      </div>
    )
  }

  const selected = students.find((s) => String(s.id) === String(studentId))

  return (
    <div className="card p-6">
      <div className="flex items-center justify-between mb-4 flex-wrap gap-3">
        <h2 className="font-semibold text-gray-900">AI 학습 분석 챗봇</h2>
        <select value={scope} onChange={(e) => setScope(e.target.value)} className="input text-sm w-auto">
          <option value="all">전체 데이터 기준</option>
          <option value="student" disabled={!selected}>
            {selected ? `${selected.name} 한정` : '학생 한정(선택 필요)'}
          </option>
        </select>
      </div>

      <div className="space-y-3 mb-4 max-h-72 overflow-y-auto">
        {messages.length === 0 && (
          <p className="text-sm text-gray-400">예: "이 학생의 최근 성적 추이는?", "수학 과목 제출률은 어때?"</p>
        )}
        {messages.map((m, i) => (
          <div key={i} className={m.role === 'user' ? 'text-right' : 'text-left'}>
            <span className={`inline-block px-3 py-2 rounded-xl text-sm max-w-[85%] whitespace-pre-wrap ${
              m.role === 'user' ? 'bg-primary-600 text-white' : 'bg-gray-100 text-gray-800'
            }`}>
              {m.text}
            </span>
          </div>
        ))}
        {sending && <p className="text-sm text-gray-400">생각 중...</p>}
      </div>

      <form onSubmit={send} className="flex gap-2">
        <input
          value={input}
          onChange={(e) => setInput(e.target.value)}
          placeholder="학습 현황에 대해 질문하세요"
          className="input flex-1 text-sm"
        />
        <button type="submit" disabled={sending} className="btn-primary text-sm disabled:opacity-60">
          전송
        </button>
      </form>
    </div>
  )
}

function KpiTile({ label, value, sub, color }) {
  return (
    <div className={`rounded-xl p-4 ${color}`}>
      <p className="text-xs font-medium opacity-80">{label}</p>
      <p className="text-2xl font-bold leading-tight mt-1">{value}</p>
      <p className="text-xs opacity-70 mt-1">{sub}</p>
    </div>
  )
}
