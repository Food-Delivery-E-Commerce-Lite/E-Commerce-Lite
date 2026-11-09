import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext.jsx'
import { apiError } from '../services/api.js'

export default function ChangePassword() {
  const { changePassword, user } = useAuth()
  const nav = useNavigate()
  const [oldPassword, setOld] = useState('')
  const [newPassword, setNew] = useState('')
  const [err, setErr] = useState('')
  const [ok, setOk] = useState('')

  const submit = async (e) => {
    e.preventDefault(); setErr(''); setOk('')
    try {
      await changePassword(oldPassword, newPassword)
      setOk('Đổi mật khẩu thành công, vui lòng đăng nhập lại.')
      setTimeout(() => nav('/login', { replace: true }), 1200)
    } catch (ex) { setErr(apiError(ex, 'Đổi mật khẩu thất bại')) }
  }

  return (
    <section className="form-wrap">
      <h2>Đổi mật khẩu {user?.mustChangePassword ? '(bắt buộc)' : ''}</h2>
      {user?.mustChangePassword && (
        <p className="error">Tài khoản admin mặc định phải đổi mật khẩu trước khi tiếp tục.</p>
      )}
      <form onSubmit={submit}>
        <input type="password" placeholder="Mật khẩu cũ" value={oldPassword} onChange={(e) => setOld(e.target.value)} required />
        <input type="password" placeholder="Mật khẩu mới (≥8 ký tự)" value={newPassword} onChange={(e) => setNew(e.target.value)} required />
        {err && <p className="error">{err}</p>}
        {ok && <p>{ok}</p>}
        <button type="submit">Đổi mật khẩu</button>
      </form>
    </section>
  )
}
