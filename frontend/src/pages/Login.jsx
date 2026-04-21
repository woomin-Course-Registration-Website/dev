import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import useAuthStore from '../store/authStore'
import { login as loginApi, resetPassword } from '../api/auth'

export default function Login() {
  const navigate  = useNavigate()
  const loginStore = useAuthStore((s) => s.login)

  const [form, setForm]     = useState({ email: '', password: '' })
  const [error, setError]   = useState('')
  const [loading, setLoading] = useState(false)
  const [shake, setShake]   = useState(false)

  const [resetModal, setResetModal]   = useState(false)
  const [resetEmail, setResetEmail]   = useState('')
  const [resetMsg,   setResetMsg]     = useState(null)
  const [resetLoading, setResetLoading] = useState(false)

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
    try {
      const data = await loginApi(form.email, form.password)
      // accessToken, refreshToken, user 저장
      loginStore(data.user, data.accessToken, data.refreshToken)
      navigate('/dashboard')
    } catch (err) {
      const msg = err.response?.data?.error || '이메일 또는 비밀번호가 올바르지 않습니다.'
      triggerError(msg)
    } finally {
      setLoading(false)
    }
  }

  const triggerError = (msg) => {
    setError(msg)
    setShake(true)
    setTimeout(() => setShake(false), 400)
  }

  const handleResetPassword = async (e) => {
    e.preventDefault()
    setResetLoading(true)
    setResetMsg(null)
    try {
      await resetPassword(resetEmail)
      setResetMsg({ ok: true, text: '임시 비밀번호가 이메일로 발송되었습니다.' })
    } catch (err) {
      const msg = err?.response?.data?.message || '해당 이메일로 등록된 계정이 없습니다.'
      setResetMsg({ ok: false, text: msg })
    } finally {
      setResetLoading(false)
    }
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

            <div className="flex justify-end">
              <button
                type="button"
                onClick={() => { setResetModal(true); setResetEmail(''); setResetMsg(null) }}
                className="text-xs text-primary-600 hover:text-primary-700 font-medium"
              >
                비밀번호를 잊으셨나요?
              </button>
            </div>

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

          <Link
            to="/register"
            className="btn-md btn-secondary w-full text-gray-600 flex items-center justify-center"
          >
            회원가입
          </Link>
        </div>
      </div>

      {/* 비밀번호 재설정 모달 */}
      {resetModal && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-2xl shadow-xl w-full max-w-sm p-6">
            <h3 className="font-semibold text-gray-900 mb-1">비밀번호 재설정</h3>
            <p className="text-sm text-gray-500 mb-4">가입한 이메일을 입력하면 임시 비밀번호를 발송합니다.</p>
            <form onSubmit={handleResetPassword} className="space-y-3">
              <input
                type="email"
                value={resetEmail}
                onChange={(e) => setResetEmail(e.target.value)}
                className="input"
                placeholder="이메일 주소"
                required
              />
              {resetMsg && (
                <p className={`text-sm font-medium ${resetMsg.ok ? 'text-green-600' : 'text-red-500'}`}>
                  {resetMsg.text}
                </p>
              )}
              <div className="flex gap-2 pt-1">
                <button
                  type="button"
                  onClick={() => setResetModal(false)}
                  className="flex-1 btn-md btn-ghost"
                >
                  닫기
                </button>
                <button
                  type="submit"
                  disabled={resetLoading}
                  className="flex-1 btn-md btn-primary"
                >
                  {resetLoading ? '발송 중...' : '임시 비밀번호 발송'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  )
}
