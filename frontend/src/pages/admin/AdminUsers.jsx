import { useEffect, useMemo, useState } from 'react'
import { createUser, deleteUser, getUsers, updateUser } from '../../api/users'
import useAuthStore from '../../store/authStore'

const ROLE_LABEL = { TEACHER: '교사', STUDENT: '학생', PARENT: '학부모', ADMIN: '관리자' }
const ROLES = ['TEACHER', 'STUDENT', 'PARENT', 'ADMIN']
const EMPTY_FORM = { email: '', password: '', name: '', role: 'TEACHER' }

const ROLE_BADGE = {
  TEACHER: 'badge-brand',
  STUDENT: 'badge-blue',
  PARENT:  'badge-amber',
  ADMIN:   'badge-purple',
}

function errorMessage(err, fallback) {
  return err?.response?.data?.error || err?.response?.data?.message || fallback
}

export default function AdminUsers() {
  const currentUser = useAuthStore((s) => s.user)
  const [users, setUsers] = useState([])
  const [loading, setLoading] = useState(true)
  const [search, setSearch] = useState('')
  const [modal, setModal] = useState(null)
  const [editing, setEditing] = useState(null)
  const [form, setForm] = useState(EMPTY_FORM)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [confirmDel, setConfirmDel] = useState(null)

  const load = async () => {
    setLoading(true)
    try {
      setUsers(await getUsers())
    } catch (err) {
      setError(errorMessage(err, '사용자 목록을 불러오지 못했습니다.'))
      setUsers([])
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { load() }, [])

  const filtered = useMemo(() => {
    const q = search.trim().toLowerCase()
    if (!q) return users
    return users.filter((u) =>
      u.name?.toLowerCase().includes(q) ||
      u.email?.toLowerCase().includes(q) ||
      ROLE_LABEL[u.role]?.includes(search.trim())
    )
  }, [search, users])

  const openCreate = () => {
    setEditing(null)
    setForm(EMPTY_FORM)
    setError('')
    setModal('create')
  }

  const openEdit = (user) => {
    setEditing(user)
    setForm({ email: user.email, password: '', name: user.name, role: user.role })
    setError('')
    setModal('edit')
  }

  const closeModal = () => {
    setModal(null)
    setEditing(null)
    setError('')
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    setSaving(true)
    setError('')
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
      setError(errorMessage(err, '저장에 실패했습니다.'))
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
      setError(errorMessage(err, '삭제에 실패했습니다.'))
    } finally {
      setConfirmDel(null)
    }
  }

  if (currentUser?.role !== 'ADMIN') {
    return (
      <div className="card p-6 text-sm text-gray-600">
        관리자 권한이 필요한 페이지입니다.
      </div>
    )
  }

  return (
    <div className="space-y-6 animate-fade-in">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-gray-900">사용자 관리</h1>
        <button onClick={openCreate} className="btn-md btn-primary flex items-center gap-2">
          <span className="text-lg leading-none">+</span>
          사용자 추가
        </button>
      </div>

      <div className="card px-4 py-3">
        <input
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          placeholder="이름, 이메일, 역할 검색"
          className="input"
        />
      </div>

      {error && <div className="card p-4 text-sm text-red-600 bg-red-50 border border-red-200">{error}</div>}

      <div className="card overflow-hidden">
        <table className="w-full text-sm">
          <thead>
            <tr className="table-header">
              <th className="table-cell text-left font-medium">이름</th>
              <th className="table-cell text-left font-medium">이메일</th>
              <th className="table-cell text-left font-medium">역할</th>
              <th className="table-cell text-right font-medium">관리</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr><td colSpan={4} className="text-center py-10 text-gray-400">불러오는 중...</td></tr>
            ) : filtered.length === 0 ? (
              <tr><td colSpan={4} className="text-center py-10 text-gray-400">사용자가 없습니다.</td></tr>
            ) : filtered.map((u) => {
              const isSelf = u.id === currentUser?.id || u.email === currentUser?.email
              return (
                <tr key={u.id} className="table-row">
                  <td className="table-cell font-medium text-gray-900">{u.name}</td>
                  <td className="table-cell text-gray-600">{u.email}</td>
                  <td className="table-cell">
                    <span className={`badge ${ROLE_BADGE[u.role] ?? 'badge-gray'}`}>
                      {ROLE_LABEL[u.role] || u.role}
                    </span>
                  </td>
                  <td className="table-cell text-right">
                    <button onClick={() => openEdit(u)} className="btn-sm btn-ghost text-brand-700 mr-2">
                      수정
                    </button>
                    <button
                      onClick={() => setConfirmDel(u.id)}
                      disabled={isSelf}
                      className="btn-sm btn-ghost text-red-600 disabled:text-gray-300 disabled:cursor-not-allowed"
                    >
                      삭제
                    </button>
                  </td>
                </tr>
              )
            })}
          </tbody>
        </table>
      </div>

      {modal && (
        <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-2xl shadow-soft-lg w-full max-w-md">
            <div className="flex items-center justify-between px-6 py-4 border-b border-gray-100">
              <h2 className="font-semibold text-gray-900">
                {modal === 'create' ? '사용자 추가' : '사용자 수정'}
              </h2>
              <button onClick={closeModal} className="text-gray-400 hover:text-gray-600 transition-colors">닫기</button>
            </div>
            <form onSubmit={handleSubmit} className="p-6 space-y-4">
              <label className="block text-sm font-medium text-gray-700">
                이메일
                <input
                  type="email"
                  value={form.email}
                  onChange={(e) => setForm({ ...form, email: e.target.value })}
                  className="input mt-1.5"
                  required={modal === 'create'}
                  disabled={modal === 'edit'}
                />
              </label>
              <label className="block text-sm font-medium text-gray-700">
                비밀번호
                <input
                  type="password"
                  value={form.password}
                  onChange={(e) => setForm({ ...form, password: e.target.value })}
                  className="input mt-1.5"
                  placeholder={modal === 'create' ? '8자 이상' : '변경할 때만 입력'}
                  required={modal === 'create'}
                  minLength={8}
                />
              </label>
              <label className="block text-sm font-medium text-gray-700">
                이름
                <input
                  value={form.name}
                  onChange={(e) => setForm({ ...form, name: e.target.value })}
                  className="input mt-1.5"
                  required
                />
              </label>
              <label className="block text-sm font-medium text-gray-700">
                역할
                <select
                  value={form.role}
                  onChange={(e) => setForm({ ...form, role: e.target.value })}
                  className="input mt-1.5"
                >
                  {ROLES.map((role) => (
                    <option key={role} value={role}>{ROLE_LABEL[role]}</option>
                  ))}
                </select>
              </label>
              {error && <p className="text-sm text-red-500 font-medium">{error}</p>}
              <div className="flex justify-end gap-2 pt-2">
                <button type="button" onClick={closeModal} className="btn-md btn-secondary">취소</button>
                <button type="submit" disabled={saving} className="btn-md btn-primary">
                  {saving ? '저장 중...' : '저장'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {confirmDel && (
        <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-2xl shadow-soft-lg w-full max-w-sm p-6 text-center">
            <h3 className="font-semibold text-gray-900 mb-2">사용자를 삭제할까요?</h3>
            <p className="text-sm text-gray-500 mb-6">삭제한 계정은 되돌릴 수 없습니다.</p>
            <div className="flex gap-3">
              <button onClick={() => setConfirmDel(null)} className="flex-1 btn-md btn-secondary">취소</button>
              <button onClick={handleDelete} className="flex-1 btn-md btn-danger">
                삭제
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
