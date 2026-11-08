import { useEffect, useState } from 'react'
import api, { unwrap, apiError } from '../services/api.js'

export default function Admin() {
  const [status, setStatus] = useState('PENDING')
  const [items, setItems] = useState([])
  const [msg, setMsg] = useState('')
  const load = async () => {
    try {
      const res = await api.get('/admin/restaurants', { params: { status, page: 0, size: 20 } })
      setItems(unwrap(res).data)
    } catch { setItems([]) }
  }
  useEffect(() => { load() }, [status])
  const decide = async (id, next) => {
    setMsg('')
    try { await api.patch(`/admin/restaurants/${id}/status`, { status: next }); await load() }
    catch (ex) { setMsg(apiError(ex)) }
  }
  return (
    <section>
      <h2>Quản trị — Duyệt nhà hàng</h2>
      {msg && <p className="error">{msg}</p>}
      <select value={status} onChange={(e) => setStatus(e.target.value)}>
        {['PENDING', 'APPROVED', 'REJECTED', 'LOCKED'].map((s) => <option key={s} value={s}>{s}</option>)}
      </select>
      {items.map((r) => (
        <div key={r.id} className="card row">
          <div style={{ flex: 1 }}><b>{r.name}</b><div><small>{r.address}</small></div></div>
          <button onClick={() => decide(r.id, 'APPROVED')}>Duyệt</button>
          <button onClick={() => decide(r.id, 'REJECTED')}>Từ chối</button>
          <button onClick={() => decide(r.id, 'LOCKED')}>Khóa</button>
        </div>
      ))}
      {items.length === 0 && <p>Không có nhà hàng {status}.</p>}
    </section>
  )
}
