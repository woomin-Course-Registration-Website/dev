import { useState, useEffect } from 'react'
import useAuthStore from '../../store/authStore'
import { getMe, updateMe, getNotificationSettings, updateNotificationSettings } from '../../api/users'
import { changePassword } from '../../api/auth'

const TABS = ['프로필', '비밀번호', '알림']

const ROLE_LABEL = { TEACHER: '교사', STUDENT: '학생', PARENT: '학부모', ADMIN: '관리자' }

export default function Settings() {
  const { user, login } = useAuthStore()
  const [tab, setTab]   = useState('프로필')

  // 프로필
  const [name,    setName]    = useState('')
  const [email,   setEmail]   = useState('')
  const [role,    setRole]    = useState('')
  const [saving,  setSaving]  = useState(false)
  const [profMsg, setProfMsg] = useState(null)

  // 비밀번호
  const [curPw,  setCurPw]  = useState('')
  const [newPw,  setNewPw]  = useState('')
  const [confPw, setConfPw] = useState('')
  const [pwSaving,  setPwSaving]  = useState(false)
  const [pwMsg,     setPwMsg]     = useState(null)

  // 알림 설정
  const [notif,       setNotif]       = useState({ notifyGrade: true, notifyFeedback: true, notifyCounseling: true })
  const [notifSaving, setNotifSaving] = useState(false)
  const [notifMsg,    setNotifMsg]    = useState(null)

  useEffect(() => {
    getMe().then((data) => {
      setName(data.name)
      setEmail(data.email)
      setRole(data.role)
    }).catch(() => {
      setName(user?.name  || '')
      setEmail(user?.email || '')
      setRole(user?.role   || '')
    })
  }, [user])

  useEffect(() => {
    getNotificationSettings()
      .then((d) => setNotif({
        notifyGrade: d.notifyGrade,
        notifyFeedback: d.notifyFeedback,
        notifyCounseling: d.notifyCounseling,
      }))
      .catch(() => {})
  }, [])

  const handleSaveNotif = async () => {
    setNotifSaving(true)
    setNotifMsg(null)
    try {
      await updateNotificationSettings(notif)
      setNotifMsg({ ok: true, text: '알림 설정이 저장되었습니다.' })
    } catch {
      setNotifMsg({ ok: false, text: '저장에 실패했습니다.' })
    } finally {
      setNotifSaving(false)
    }
  }

  const handleSaveProfile = async (e) => {
    e.preventDefault()
    setSaving(true)
    setProfMsg(null)
    try {
      const updated = await updateMe(name)
      // authStore의 user.name도 갱신
      login({ ...user, name: updated.name }, useAuthStore.getState().accessToken, useAuthStore.getState().refreshToken)
      setProfMsg({ ok: true, text: '프로필이 저장되었습니다.' })
    } catch {
      setProfMsg({ ok: false, text: '저장에 실패했습니다.' })
    } finally {
      setSaving(false)
    }
  }

  const handleChangePassword = async (e) => {
    e.preventDefault()
    setPwMsg(null)
    if (newPw !== confPw) {
      setPwMsg({ ok: false, text: '새 비밀번호가 일치하지 않습니다.' })
      return
    }
    if (newPw.length < 8) {
      setPwMsg({ ok: false, text: '비밀번호는 8자 이상이어야 합니다.' })
      return
    }
    setPwSaving(true)
    try {
      await changePassword(curPw, newPw)
      setPwMsg({ ok: true, text: '비밀번호가 변경되었습니다.' })
      setCurPw(''); setNewPw(''); setConfPw('')
    } catch (err) {
      const msg = err?.response?.data?.message || '비밀번호 변경에 실패했습니다.'
      setPwMsg({ ok: false, text: msg })
    } finally {
      setPwSaving(false)
    }
  }

  return (
    <div className="space-y-6 animate-fade-in max-w-2xl">
      <h1 className="text-2xl font-bold text-gray-900">설정</h1>

      {/* 탭 */}
      <div className="flex gap-1 bg-gray-100 p-1 rounded-xl w-fit">
        {TABS.map((t) => (
          <button key={t} onClick={() => setTab(t)}
            className={`px-5 py-2 rounded-lg text-sm font-medium transition-all ${
              tab === t ? 'bg-white text-gray-900 shadow-card' : 'text-gray-500 hover:text-gray-700'
            }`}
          >{t}</button>
        ))}
      </div>

      {/* 프로필 탭 */}
      {tab === '프로필' && (
        <div className="card p-6">
          <h2 className="font-semibold text-gray-900 mb-5">내 프로필</h2>

          {/* 아바타 */}
          <div className="flex items-center gap-4 mb-6 pb-6 border-b border-gray-100">
            <div className="w-16 h-16 rounded-full bg-primary-700 text-white flex items-center justify-center text-2xl font-bold">
              {name?.[0] || '?'}
            </div>
            <div>
              <p className="font-semibold text-gray-900">{name}</p>
              <p className="text-sm text-gray-400">{ROLE_LABEL[role] || role}</p>
            </div>
          </div>

          <form onSubmit={handleSaveProfile} className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1.5">이름</label>
              <input value={name} onChange={(e) => setName(e.target.value)}
                className="input" placeholder="이름" required />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1.5">이메일</label>
              <input value={email} disabled className="input bg-gray-50 text-gray-400 cursor-not-allowed" />
              <p className="text-xs text-gray-400 mt-1">이메일은 변경할 수 없습니다.</p>
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1.5">역할</label>
              <input value={ROLE_LABEL[role] || role} disabled
                className="input bg-gray-50 text-gray-400 cursor-not-allowed" />
            </div>

            {profMsg && (
              <p className={`text-sm font-medium ${profMsg.ok ? 'text-green-600' : 'text-red-500'}`}>
                {profMsg.text}
              </p>
            )}

            <div className="flex justify-end pt-2">
              <button type="submit" disabled={saving} className="btn-md btn-primary">
                {saving ? '저장 중...' : '저장'}
              </button>
            </div>
          </form>
        </div>
      )}

      {/* 비밀번호 탭 */}
      {tab === '비밀번호' && (
        <div className="card p-6">
          <h2 className="font-semibold text-gray-900 mb-5">비밀번호 변경</h2>
          <form onSubmit={handleChangePassword} className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1.5">현재 비밀번호</label>
              <input type="password" value={curPw} onChange={(e) => setCurPw(e.target.value)}
                className="input" placeholder="현재 비밀번호" required />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1.5">새 비밀번호</label>
              <input type="password" value={newPw} onChange={(e) => setNewPw(e.target.value)}
                className="input" placeholder="8자 이상" required />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1.5">새 비밀번호 확인</label>
              <input type="password" value={confPw} onChange={(e) => setConfPw(e.target.value)}
                className="input" placeholder="비밀번호 재입력" required />
            </div>

            {pwMsg && (
              <p className={`text-sm font-medium ${pwMsg.ok ? 'text-green-600' : 'text-red-500'}`}>
                {pwMsg.text}
              </p>
            )}

            <div className="flex justify-end pt-2">
              <button type="submit" disabled={pwSaving} className="btn-md btn-primary">
                {pwSaving ? '변경 중...' : '비밀번호 변경'}
              </button>
            </div>
          </form>
        </div>
      )}

      {/* 알림 탭 */}
      {tab === '알림' && (
        <div className="card p-6">
          <h2 className="font-semibold text-gray-900 mb-5">알림 수신 설정</h2>
          <div className="space-y-3">
            {[
              { key: 'notifyGrade',      label: '성적 알림',  desc: '성적이 입력·수정되면 알림을 받습니다.' },
              { key: 'notifyFeedback',   label: '피드백 알림', desc: '피드백이 공개되면 알림을 받습니다.' },
              { key: 'notifyCounseling', label: '상담 알림',  desc: '상담 내역이 등록되면 알림을 받습니다.' },
            ].map(({ key, label, desc }) => (
              <label key={key} className="flex items-center justify-between p-3 rounded-lg border border-gray-100 cursor-pointer hover:bg-gray-50">
                <div>
                  <p className="text-sm font-medium text-gray-900">{label}</p>
                  <p className="text-xs text-gray-400">{desc}</p>
                </div>
                <input
                  type="checkbox"
                  checked={notif[key]}
                  onChange={(e) => setNotif((n) => ({ ...n, [key]: e.target.checked }))}
                  className="w-5 h-5 accent-primary-600"
                />
              </label>
            ))}
          </div>
          {notifMsg && (
            <p className={`text-sm font-medium mt-4 ${notifMsg.ok ? 'text-green-600' : 'text-red-500'}`}>
              {notifMsg.text}
            </p>
          )}
          <div className="flex justify-end pt-5">
            <button onClick={handleSaveNotif} disabled={notifSaving} className="btn-md btn-primary">
              {notifSaving ? '저장 중...' : '저장'}
            </button>
          </div>
        </div>
      )}
    </div>
  )
}
