import { useEffect, useState } from 'react'
import { adminDecide, adminOrders, adminOverview, adminRestaurants, adminUsers, lockUser, unlockUser } from '../services/admin.js'
import { apiError } from '../services/api.js'

export default function Admin() {
  const [status, setStatus] = useState('PENDING')
  const [items, setItems] = useState([])
  const [page, setPage] = useState(null)
  const [overview, setOverview] = useState(null)
  const [users, setUsers] = useState([])
  const [role, setRole] = useState('')
  const [orders, setOrders] = useState([])
  const [orderStatus, setOrderStatus] = useState('')
  const [msg, setMsg] = useState('')

  const loadRestaurants = async (p = 0) => {
    try {
      const { data, page: pg } = await adminRestaurants(status, p, 20)
      setItems(data); setPage(pg)
    } catch { setItems([]) }
  }
  const loadUsers = async () => {
    try {
      const { data } = await adminUsers(role || null, 0, 20)
      setUsers(data)
    } catch { setUsers([]) }
  }
  const loadOrders = async () => {
    try {
      const { data } = await adminOrders(orderStatus || null, 0, 20)
      setOrders(data)
    } catch { setOrders([]) }
  }

  useEffect(() => { adminOverview().then(setOverview).catch(() => setOverview(null)) }, [])
  useEffect(() => { loadRestaurants(0) }, [status])
  useEffect(() => { loadUsers() }, [role])
  useEffect(() => { loadOrders() }, [orderStatus])

  const decide = async (id, next) => {
    setMsg('')
    try { await adminDecide(id, next); await loadRestaurants(0); adminOverview().then(setOverview).catch(() => {}) }
    catch (ex) { setMsg(apiError(ex)) }
  }
  const toggleLock = async (u) => {
    setMsg('')
    try {
      if (u.active) await lockUser(u.id)
      else await unlockUser(u.id)
      await loadUsers()
    } catch (ex) { setMsg(apiError(ex)) }
  }

  return (
    <section>
      <h2>Quản trị</h2>
      {msg && <p className="error">{msg}</p>}
      {overview && (
        <div className="card">
          <b>Tổng quan:</b> {overview.totalUsers} users ({overview.totalCustomers} khách, {overview.totalMerchants} quán) ·
          {' '}{overview.totalOrders} đơn · {overview.pendingRestaurants} quán chờ duyệt
        </div>
      )}
      <h3>Duyệt nhà hàng</h3>
      <select value={status} onChange={(e) => setStatus(e.target.value)}>
        {['PENDING', 'APPROVED', 'REJECTED', 'LOCKED'].map((s) => <option key={s} value={s}>{s}</option>)}
      </select>
      {items.map((r) => (
        <div key={r.id} className="card row">
          <div style={{ flex: 1 }}><b>{r.name}</b><div><small>{r.address} · ★ {r.averageRating} ({r.ratingCount})</small></div></div>
          <button onClick={() => decide(r.id, 'APPROVED')}>Duyệt</button>
          <button onClick={() => decide(r.id, 'REJECTED')}>Từ chối</button>
          <button onClick={() => decide(r.id, 'LOCKED')}>Khóa</button>
        </div>
      ))}
      {items.length === 0 && <p>Không có nhà hàng {status}.</p>}

      <h3>Người dùng</h3>
      <select value={role} onChange={(e) => setRole(e.target.value)}>
        <option value="">Tất cả role</option>
        <option value="CUSTOMER">CUSTOMER</option>
        <option value="MERCHANT">MERCHANT</option>
        <option value="ADMIN">ADMIN</option>
      </select>
      {users.map((u) => (
        <div key={u.id} className="card row">
          <div style={{ flex: 1 }}><b>{u.fullName}</b> ({u.email}) — {u.role} — {u.active ? 'Hoạt động' : 'Bị khóa'}</div>
          <button onClick={() => toggleLock(u)}>{u.active ? 'Khóa' : 'Mở'}</button>
        </div>
      ))}

      <h3>Tất cả đơn hàng</h3>
      <select value={orderStatus} onChange={(e) => setOrderStatus(e.target.value)}>
        <option value="">Tất cả trạng thái</option>
        {['PENDING', 'CONFIRMED', 'PREPARING', 'DELIVERING', 'COMPLETED', 'CANCELLED'].map((s) => <option key={s} value={s}>{s}</option>)}
      </select>
      {orders.map((o) => (
        <div key={o.order_id || o.id} className="card row">
          <span style={{ flex: 1 }}>#{o.order_id ?? o.id} — {o.status} — {Number(o.total_amount ?? o.totalAmount ?? 0).toLocaleString('vi-VN')}đ</span>
        </div>
      ))}
      {orders.length === 0 && <p>Không có đơn.</p>}
    </section>
  )
}
