import axios from 'axios'

const api = axios.create({ baseURL: '/api/v1' })

// Gắn JWT vào mọi request
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

// TODO: bắt 401 -> chuyển về trang đăng nhập
export default api
