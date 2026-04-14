import { useState, useEffect } from 'react'
import { getUsers, createUser, updateUser, deleteUser } from '../../api/users'

const ROLE_LABEL = { TEACHER: '교사', STUDENT: '학생', PARENT: '학부모', ADMIN: '관리자' }
const ROLES = ['TEACHER', 'STUDENT', 'PARENT', 'ADMIN']

const EMPTY_FORM = { email: '', password: '', name: '', role: 'TEACHER' }

export default function AdminUsers() {
  const [users,   setUsers]   = useState([])
  const [loading, setLoading] = useState(true)
  const [search,  setSearch]  = useState('')

  const [modal,   setModal]   = useState(null) // null | 'create' | 'edit'
  const [editing, setEditing] = useState(null) // user object when editing
  const [form,    setForm]    = useState(EMPTY_FORM)
  const [saving,  setSaving]  = useState(false)
  const [error,   setError]   = useState(null)

  const [confirmDel, setConfirmDel] = useState(null) // user id to delete

  const load = async () => {
    setLoading(true)
    try {
      const data = await getUsers()
      setUsers(data || [])
    } catch {
      setUsers([])
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { load() }, [])

  const openCreate = () => {
    setEditing(null)
    setForm(EMPTY_FORM)
    setError(null)
    setModal('create')
  }

  const openEdit = (u) => {
    setEditing(u)
    setForm({ email: u.email, password: '', name: u.name, role: u.role })
    setError(null)
    setModal('edit')
  }

  const closeModal = () => { setModal(null); setEditing(null); setError(null) }

  const handleSubmit = async (e) => {
    e.preventDefault()
    setSaving(true)
    setError(null)
    try {
      if (modal === 'create') {
        await createUser(form)
      } else {
        const body = { name: form.name, role: form.role }
        if (form.password) body.password = form.password
        await updateUser(editing.id, body)
      }
      await load()
      closeModal()
    } catch (err) {
      setError(err?.response?.data?.message || '저장에 실패했습니다.')
    } finally {
      setSaving(false)
    }
  }

  const handleDelete = async () => {
    if (!confirmDel) return
    try {
      await deleteUser(confirmDel)
      setUsers((prev) => prev.filter((u) => u.id !== confirmDel))
    } catch (err) {
      alert(err?.response?.data?.message || '삭제에 실패했습니다.')
    } finally {
      setConfirmDel(null)
    }
  }

  const filtered = users.filter((u) =>
    u.name?.includes(search) || u.email?.includes(search)
  )

  return (
    <div className="space-y-6 animate-fade-in">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-gray-900">사용자 관리</h1>
        <button onClick={openCreate} className="btn-md btn-primary flex items-center gap-2">
          <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" strokeWidth={2} stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" d="M12 4.5v15m7.5-7.5h-15" />
          </svg>
          사용자 추가
        </button>
      </div>

      {/* 검색 */}
      <div className="card px-4 py-3">
        <input
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          placeholder="이름 또는 이메일 검색..."
          className="input"
        />
      </div>

      {/* 테이블 */}
      <div className="card overflow-hidden">
        <table className="w-full text-sm">
          <thead className="bg-gray-50 border-b border-gray-100">
            <tr>
              <th className="text-left px-4 py-3 font-medium text-gray-600">이름</th>
              <th className="text-left px-4 py-3 font-medium text-gray-600">이메일</th>
              <th className="text-left px-4 py-3 font-medium text-gray-600">역할</th>
              <th className="text-right px-4 py-3 font-medium text-gray-600">관리</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-50">
            {loading ? (
              <tr><td colSpan={4} className="text-center py-10 text-gray-400">불러오는 중...</td></tr>
            ) : filtered.length === 0 ? (
              <tr><td colSpan={4} className="text-center py-10 text-gray-400">사용자가 없습니다.</td></tr>
            ) : filtered.map((u) => (
              <tr key={u.id} className="hover:bg-gray-50 transition-colors">
                <td className="px-4 py-3">
                  <div className="flex items-center gap-3">
                    <div className="w-8 h-8 rounded-full bg-primary-700 text-white flex items-center justify-center text-xs font-bold flex-shrink-0">
                      {u.name?.[0] || '?'}
                    </div>
                    <span className="font-medium text-gray-900">{u.name}</span>
                  </div>
                </td>
                <td className="px-4 py-3 text-gray-600">{u.email}</td>
                <td className="px-4 py-3">
                  <span className={`badge ${
                    u.role === 'ADMIN' ? 'badge-red' :
                    u.role === 'TEACHER' ? 'badge-blue' :
                    u.role === 'STUDENT' ? 'badge-green' : 'badge-purple'
                  }`}>
                    {ROLE_LABEL[u.role] || u.role}
                  </span>
                </td>
                <td className="px-4 py-3 text-right">
                  <div className="flex items-center justify-end gap-2">
                    <button
                      onClick={() => openEdit(u)}
                      className="text-xs text-primary-600 hover:text-primary-800 font-medium px-2 py-1 rounded hover:bg-primary-50 transition-colors"
                    >
                      수정
                    </button>
                    <button
                      onClick={() => setConfirmDel(u.id)}
                      className="text-xs text-red-500 hover:text-red-700 font-medium px-2 py-1 rounded hover:bg-red-50 transition-colors"
                    >
                      삭제
                    </button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* 생성/수정 모달 */}
      {modal && (
        <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-2xl shadow-xl w-full max-w-md">
            <div className="flex items-center justify-between px-6 py-4 border-b border-gray-100">
              <h2 className="font-semibold text-gray-900">
                {modal === 'create' ? '사용자 추가' : '사용자 수정'}
              </h2>
              <button onClick={closeModal} className="text-gray-400 hover:text-gray-600">
                <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" strokeWidth={2} stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
                </svg>
              </button>
            </div>
            <form onSubmit={handleSubmit} className="p-6 space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1.5">이메일</label>
                <input
                  type="email"
                  value={form.email}
                  onChange={(e) => setForm({ ...form, email: e.target.value })}
                  className="input"
                  placeholder="이메일"
                  required={modal === 'create'}
                  disabled={modal === 'edit'}
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1.5">
                  비밀번호{modal === 'edit' && <span className="text-gray-400 font-normal"> (변경 시에만 입력)</span>}
                </label>
                <input
                  type="password"
                  value={form.password}
                  onChange={(e) => setForm({ ...form, password: e.target.value })}
                  className="input"
                  placeholder={modal === 'create' ? '비밀번호 (8자 이상)' : '변경하지 않으면 빈칸'}
                  required={modal === 'create'}
                  minLength={modal === 'create' ? 8 : undefined}
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1.5">이름</label>
                <input
                  value={form.name}
                  onChange={(e) => setForm({ ...form, name: e.target.value })}
                  className="input"
                  placeholder="이름"
                  required
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1.5">역할</label>
                <select
                  value={form.role}
                  onChange={(e) => setForm({ ...form, role: e.target.value })}
                  className="input"
                >
                  {ROLES.map((r) => (
                    <option key={r} value={r}>{ROLE_LABEL[r]}</option>
                  ))}
                </select>
              </div>
              {error && <p className="text-sm text-red-500 font-medium">{error}</p>}
              <div className="flex justify-end gap-2 pt-2">
                <button type="button" onClick={closeModal} className="btn-md btn-ghost">취소</button>
                <button type="submit" disabled={saving} className="btn-md btn-primary">
                  {saving ? '저장 중...' : '저장'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* 삭제 확인 모달 */}
      {confirmDel && (
        <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-2xl shadow-xl w-full max-w-sm p-6 text-center">
            <div className="w-12 h-12 bg-red-100 rounded-full flex items-center justify-center mx-auto mb-4">
              <svg className="w-6 h-6 text-red-500" fill="none" viewBox="0 0 24 24" strokeWidth={2} stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" d="M12 9v3.75m-9.303 3.376c-.866 1.5.217 3.374 1.948 3.374h14.71c1.73 0 2.813-1.874 1.948-3.374L13.949 3.378c-.866-1.5-3.032-1.5-3.898 0L2.697 16.126zM12 15.75h.007v.008H12v-.008z" />
              </svg>
            </div>
            <h3 className="font-semibold text-gray-900 mb-2">사용자를 삭제하시겠습니까?</h3>
            <p className="text-sm text-gray-500 mb-6">삭제한 사용자는 복구할 수 없습니다.</p>
            <div className="flex gap-3">
              <button onClick={() => setConfirmDel(null)} className="flex-1 btn-md btn-ghost">취소</button>
              <button onClick={handleDelete} className="flex-1 btn-md bg-red-500 hover:bg-red-600 text-white rounded-xl font-medium transition-colors">삭제</button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
