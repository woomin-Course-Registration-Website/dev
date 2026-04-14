import axios from 'axios'

/**
 * axios 기본 인스턴스
 *
 * - 모든 요청에 자동으로 Authorization: Bearer {token} 헤더를 추가합니다.
 * - 401 응답 시 refreshToken으로 재발급을 시도하고 원래 요청을 재전송합니다.
 * - 재발급 실패 시 로그아웃 처리합니다.
 */
const client = axios.create({
  baseURL: '/api',
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

    // 401이고 재시도가 아닌 경우에만 refresh 시도
    if (error.response?.status !== 401 || original._retry) {
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
      const { data } = await axios.post('/api/auth/refresh', { refreshToken })
      const newToken = data.accessToken
      localStorage.setItem('accessToken', newToken)
      localStorage.setItem('refreshToken', data.refreshToken)
      client.defaults.headers.common.Authorization = `Bearer ${newToken}`
      processQueue(null, newToken)
      original.headers.Authorization = `Bearer ${newToken}`
      return client(original)
    } catch (err) {
      processQueue(err, null)
      logout()
      return Promise.reject(err)
    } finally {
      isRefreshing = false
    }
  }
)

function logout() {
  localStorage.removeItem('accessToken')
  localStorage.removeItem('refreshToken')
  localStorage.removeItem('user')
  window.location.href = '/login'
}

export default client
