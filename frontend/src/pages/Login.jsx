import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import useAuthStore from '../store/authStore'

export default function Login() {
  const navigate = useNavigate()
  const login = useAuthStore((s) => s.login)
  const loginDemo = useAuthStore((s) => s.loginDemo)

  const [form, setForm] = useState({ email: '', password: '' })
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const [shake, setShake] = useState(false)

  const handleChange = (e) => {
    setForm((f) => ({ ...f, [e.target.name]: e.target.value }))
    setError('')
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    if (!form.email || !form.password) {
      triggerError('이메일과 비밀번호를 입력해주세요.')
      return
    }
    setLoading(true)
    // 실제 API 연동 전 데모 처리
    await new Promise((r) => setTimeout(r, 800))
    if (form.email === 'teacher@school.kr' && form.password === '1234') {
      login({ id: 1, name: '김선생님', email: form.email, role: 'TEACHER' }, 'token-demo')
      navigate('/dashboard')
    } else {
      setLoading(false)
      triggerError('이메일 또는 비밀번호가 올바르지 않습니다.')
    }
  }

  const handleDemo = () => {
    loginDemo()
    navigate('/dashboard')
  }

  const triggerError = (msg) => {
    setError(msg)
    setShake(true)
    setTimeout(() => setShake(false), 400)
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-primary-900 via-primary-800 to-primary-700 flex items-center justify-center p-4">
      {/* 배경 장식 */}
      <div className="absolute inset-0 overflow-hidden pointer-events-none">
        <div className="absolute -top-24 -right-24 w-96 h-96 bg-white/5 rounded-full" />
        <div className="absolute -bottom-32 -left-20 w-80 h-80 bg-white/5 rounded-full" />
      </div>

      <div className="relative w-full max-w-md animate-fade-in">
        {/* 헤더 */}
        <div className="text-center mb-8">
          <div className="inline-flex items-center justify-center w-14 h-14 bg-white/10 rounded-2xl mb-4">
            <span className="text-2xl font-bold text-white">SM</span>
          </div>
          <h1 className="text-2xl font-bold text-white">SchoolManager</h1>
          <p className="text-primary-200 text-sm mt-1">학생 성적 및 상담 관리 시스템</p>
        </div>

        {/* 카드 */}
        <div className={`bg-white rounded-2xl shadow-modal p-8 ${shake ? 'animate-shake' : ''}`}>
          <h2 className="text-lg font-semibold text-gray-900 mb-6">로그인</h2>

          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1.5">이메일</label>
              <input
                name="email"
                type="email"
                autoComplete="email"
                placeholder="teacher@school.kr"
                value={form.email}
                onChange={handleChange}
                className={error ? 'input-error' : 'input'}
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1.5">비밀번호</label>
              <input
                name="password"
                type="password"
                autoComplete="current-password"
                placeholder="••••••••"
                value={form.password}
                onChange={handleChange}
                className={error ? 'input-error' : 'input'}
              />
            </div>

            {error && (
              <div className="flex items-center gap-2 text-red-600 text-sm bg-red-50 border border-red-200 rounded-lg px-3 py-2.5">
                <svg className="w-4 h-4 flex-shrink-0" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" d="M12 9v3.75m-9.303 3.376c-.866 1.5.217 3.374 1.948 3.374h14.71c1.73 0 2.813-1.874 1.948-3.374L13.949 3.378c-.866-1.5-3.032-1.5-3.898 0L2.697 16.126zM12 15.75h.007v.008H12v-.008z" />
                </svg>
                {error}
              </div>
            )}

            <button
              type="submit"
              disabled={loading}
              className="btn-md btn-primary w-full mt-2 h-11"
            >
              {loading ? (
                <svg className="w-5 h-5 animate-spin" fill="none" viewBox="0 0 24 24">
                  <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                  <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
                </svg>
              ) : '로그인'}
            </button>
          </form>

          <div className="relative my-5">
            <div className="absolute inset-0 flex items-center">
              <div className="w-full border-t border-gray-200" />
            </div>
            <div className="relative flex justify-center">
              <span className="bg-white px-3 text-xs text-gray-400">또는</span>
            </div>
          </div>

          <button
            onClick={handleDemo}
            className="btn-md btn-secondary w-full text-gray-600"
          >
            데모로 시작하기
          </button>

          <p className="text-center text-xs text-gray-400 mt-4">
            데모 계정: teacher@school.kr / 1234
          </p>
        </div>
      </div>
    </div>
  )
}
