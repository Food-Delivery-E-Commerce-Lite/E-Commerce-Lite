import axios from 'axios'

const api = axios.create({ baseURL: '/api/v1', timeout: 15000 })

// Gắn JWT vào mọi request
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

// Bắt 401 -> xóa token, chuyển về trang đăng nhập
api.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err.response?.status === 401 && !location.pathname.startsWith('/login')) {
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      location.href = '/login'
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
