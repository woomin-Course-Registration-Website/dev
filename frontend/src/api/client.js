import axios from 'axios'
import useAuthStore from '../store/authStore'
import useToastStore from '../store/toastStore'

/**
 * axios 기본 인스턴스
 *
 * - 모든 요청에 자동으로 Authorization: Bearer {token} 헤더를 추가합니다.
 * - 401 응답 시 refreshToken으로 재발급을 시도하고 원래 요청을 재전송합니다.
 * - 재발급 실패 시 로그아웃 처리합니다.
 * - 재발급 성공 시 zustand authStore에도 새 토큰을 반영해 store-localStorage 불일치를 방지합니다.
 * - 비-2xx 응답 (401 refresh 시도 제외)은 자동으로 토스트를 띄웁니다.
 *   호출 측에서 토스트를 띄우고 싶지 않으면 config.silent: true 로 표시하세요.
 */
const API_BASE = import.meta.env.VITE_API_BASE_URL ?? '/api'

/** 응답 body에서 사용자에게 보여줄 메시지를 추출 */
function extractMessage(error) {
  const data = error?.response?.data
  if (typeof data === 'string') return data
  if (data?.message) return data.message
  if (error?.message === 'Network Error') return '네트워크 연결을 확인해주세요.'
  if (error?.code === 'ECONNABORTED') return '요청 시간이 초과되었습니다.'
  return '요청 처리 중 오류가 발생했습니다.'
}

const client = axios.create({
  baseURL: API_BASE,
  headers: { 'Content-Type': 'application/json' },
})

/* ── 요청 인터셉터: accessToken 자동 첨부 ── */
client.interceptors.request.use((config) => {
  const token = localStorage.getItem('accessToken')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

/* ── 응답 인터셉터: 401 시 토큰 재발급 ── */
let isRefreshing = false
let failedQueue = []  // 재발급 대기 중인 요청들

const processQueue = (error, token = null) => {
  failedQueue.forEach((prom) => (error ? prom.reject(error) : prom.resolve(token)))
  failedQueue = []
}

client.interceptors.response.use(
  (res) => res,
  async (error) => {
    const original = error.config
    const status = error.response?.status

    // 401이고 재시도가 아닌 경우에만 refresh 시도 — 토스트는 띄우지 않음
    if (status !== 401 || original?._retry) {
      // 토스트 자동 노출 (silent:true 옵션으로 옵트아웃 가능)
      if (status && !original?.silent) {
        useToastStore.getState().error(extractMessage(error))
      }
      return Promise.reject(error)
    }

    if (isRefreshing) {
      // 이미 재발급 중이면 대기열에 추가
      return new Promise((resolve, reject) => {
        failedQueue.push({ resolve, reject })
      }).then((token) => {
        original.headers.Authorization = `Bearer ${token}`
        return client(original)
      })
    }

    original._retry = true
    isRefreshing = true

    const refreshToken = localStorage.getItem('refreshToken')
    if (!refreshToken) {
      logout()
      return Promise.reject(error)
    }

    try {
      // refresh 응답이 ApiResponse<T>로 한 번 감싸진 경우와 원시 형태 모두 대응
      const { data: payload } = await axios.post(`${API_BASE}/auth/refresh`, { refreshToken })
      const body = payload?.data ?? payload
      const newAccessToken = body.accessToken
      const newRefreshToken = body.refreshToken ?? refreshToken

      // localStorage + Zustand store를 동시에 갱신해 상태 분기를 막는다
      useAuthStore.getState().setAccessToken(newAccessToken)
      localStorage.setItem('refreshToken', newRefreshToken)

      client.defaults.headers.common.Authorization = `Bearer ${newAccessToken}`
      processQueue(null, newAccessToken)
      original.headers.Authorization = `Bearer ${newAccessToken}`
      return client(original)
    } catch (err) {
      processQueue(err, null)
      useToastStore.getState().error('세션이 만료되었습니다. 다시 로그인해주세요.')
      logout()
      return Promise.reject(err)
    } finally {
      isRefreshing = false
    }
  }
)

function logout() {
  // Zustand store까지 함께 비워 헤더/스토어 불일치 방지
  try { useAuthStore.getState().logout() } catch { /* store 미초기화 등 예외 무시 */ }
  localStorage.removeItem('accessToken')
  localStorage.removeItem('refreshToken')
  localStorage.removeItem('user')
  window.location.href = '/login'
}

export default client
