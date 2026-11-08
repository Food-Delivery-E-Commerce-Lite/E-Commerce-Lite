import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext.jsx'
import { apiError } from '../services/api.js'

export default function Register() {
  const { register } = useAuth()
  const nav = useNavigate()
  const [form, setForm] = useState({ email: '', password: '', fullName: '', phone: '', role: 'CUSTOMER' })
  const [err, setErr] = useState('')
  const set = (k) => (e) => setForm((f) => ({ ...f, [k]: e.target.value }))
  const submit = async (e) => {
    e.preventDefault(); setErr('')
    try { await register({ ...form, email: form.email.trim() }); nav('/', { replace: true }) }
    catch (ex) { setErr(apiError(ex, 'Đăng ký thất bại')) }
  }
  return (
    <section className="form-wrap">
      <h2>Đăng ký</h2>
      <form onSubmit={submit}>
        <input placeholder="Họ tên" value={form.fullName} onChange={set('fullName')} required />
        <input placeholder="Email" value={form.email} onChange={set('email')} required />
        <input placeholder="Mật khẩu (≥8 ký tự)" type="password" value={form.password} onChange={set('password')} required />
        <input placeholder="Số điện thoại" value={form.phone} onChange={set('phone')} />
        <select value={form.role} onChange={set('role')}>
          <option value="CUSTOMER">Khách hàng</option>
          <option value="MERCHANT">Chủ quán (Merchant)</option>
        </select>
        {err && <p className="error">{err}</p>}
        <button type="submit">Tạo tài khoản</button>
      </form>
      <p>Đã có tài khoản? <Link to="/login">Đăng nhập</Link></p>
    </section>
  )
}
