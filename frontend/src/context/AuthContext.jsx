import { createContext, useContext, useEffect, useMemo, useState } from 'react'
import api, { unwrap, setAccessToken, getAccessToken } from '../services/api.js'

const AuthContext = createContext(null)
const USER_KEY = 'fd_user'

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    try { return JSON.parse(sessionStorage.getItem(USER_KEY)) || null } catch { return null }
  })
  const [loading, setLoading] = useState(false)

  const saveUser = (u) => {
    setUser(u)
    if (u) sessionStorage.setItem(USER_KEY, JSON.stringify(u))
    else sessionStorage.removeItem(USER_KEY)
  }

  useEffect(() => {
    const onRefresh = (e) => {
      // access token mới đã được api.js lưu, chỉ cần giữ user
    }
    window.addEventListener('token-refreshed', onRefresh)
    return () => window.removeEventListener('token-refreshed', onRefresh)
  }, [])

  useEffect(() => {
    // Thử refresh phiên cũ khi tải lại trang (cookie HttpOnly còn thì backend cấp token mới)
    if (!getAccessToken() && !user) {
      fetch('/api/v1/auth/refresh', { method: 'POST', credentials: 'include' })
        .then((r) => (r.ok ? r.json() : null))
        .then((body) => {
          const payload = body && typeof body === 'object' && 'data' in body ? body.data : body
          if (payload?.accessToken) {
            setAccessToken(payload.accessToken)
            saveUser(payload.user)
          }
        })
        .catch(() => {})
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const login = async (email, password) => {
    setLoading(true)
    try {
      const res = await api.post('/auth/login', { email, password })
      const { data } = unwrap(res)
      setAccessToken(data.accessToken)
      saveUser(data.user)
      return data.user
    } finally { setLoading(false) }
  }

  const register = async (payload) => {
    setLoading(true)
    try {
      const res = await api.post('/auth/register', payload)
      const { data } = unwrap(res)
      setAccessToken(data.accessToken)
      saveUser(data.user)
      return data.user
    } finally { setLoading(false) }
  }

  const logout = async () => {
    try { await api.post('/auth/logout') } catch { /* ignore */ }
    setAccessToken(null)
    saveUser(null)
  }

  const changePassword = async (oldPassword, newPassword) => {
    const res = await api.post('/auth/change-password', { oldPassword, newPassword })
    const { data } = unwrap(res)
    // Đổi mật khẩu đá hết phiên cũ -> yêu cầu đăng nhập lại cho sạch
    setAccessToken(null)
    saveUser(null)
    return data
  }

  const value = useMemo(() => ({ user, loading, login, register, logout, changePassword, setUser: saveUser }), [user, loading])
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export const useAuth = () => useContext(AuthContext)
