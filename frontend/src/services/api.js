import axios from 'axios'

const api = axios.create({ baseURL: '/api/v1', timeout: 15000, withCredentials: true })

// Access token giữ trong bộ nhớ (không localStorage) để giảm XSS.
// Refresh token nằm trong HttpOnly cookie do backend quản lý.
let memoryToken = null
export function setAccessToken(t) { memoryToken = t }
export function getAccessToken() { return memoryToken }

api.interceptors.request.use((config) => {
  if (memoryToken) config.headers.Authorization = `Bearer ${memoryToken}`
  return config
})

// Tự refresh 1 lần khi gặp 401, rồi retry request gốc
let refreshing = null
api.interceptors.response.use(
  (res) => res,
  async (err) => {
    const original = err.config
    const url = original?.url || ''
    const isAuthCall = url.includes('/auth/login') || url.includes('/auth/register') || url.includes('/auth/refresh')
    if (err.response?.status === 401 && !original?._retried && !isAuthCall) {
      original._retried = true
      try {
        if (!refreshing) {
          refreshing = axios.post('/api/v1/auth/refresh', null, { withCredentials: true })
            .then((r) => {
              const body = r.data
              const payload = body && typeof body === 'object' && 'data' in body ? body.data : body
              return payload?.accessToken
            })
            .finally(() => { refreshing = null })
        }
        const newToken = await refreshing
        if (newToken) {
          setAccessToken(newToken)
          // Đồng bộ cho AuthContext qua event
          window.dispatchEvent(new CustomEvent('token-refreshed', { detail: newToken }))
          original.headers.Authorization = `Bearer ${newToken}`
          return api(original)
        }
      } catch {
        // refresh thất bại -> về login
      }
      setAccessToken(null)
      if (!location.pathname.startsWith('/login')) location.href = '/login'
    }
    return Promise.reject(err)
  },
)

// Backend trả ApiResponse { data, page } -> helper bóc vỏ
export function unwrap(res) {
  const body = res.data
  if (body && typeof body === 'object' && 'data' in body) return { data: body.data, page: body.page }
  return { data: body, page: null }
}

export function apiError(err, fallback = 'Đã xảy ra lỗi') {
  return err.response?.data?.message || fallback
}

export default api
