import { useEffect, useState } from 'react'
import { adminDecide, adminOrders, adminOverview, adminRestaurants, adminUsers, lockUser, unlockUser } from '../services/admin.js'
import { adminVouchers, createAdminVoucher, updateAdminVoucher } from '../services/voucher.js'
import { adminReport, maxOf } from '../services/report.js'
import { refundPayment } from '../services/order.js'
import { resolveImage, withFallback } from '../services/upload.js'
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
  const [vouchers, setVouchers] = useState([])
  const [vform, setVform] = useState({ code: '', type: 'FIXED', value: '', minOrder: '', maxDiscount: '', usageLimit: '', restaurantId: '' })
  const [repFrom, setRepFrom] = useState('')
  const [repTo, setRepTo] = useState('')
  const [report, setReport] = useState(null)

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
  const loadVouchers = async () => {
    try {
      const { data } = await adminVouchers(null, 0, 50)
      setVouchers(data ?? [])
    } catch { setVouchers([]) }
  }

  useEffect(() => { adminOverview().then(setOverview).catch(() => setOverview(null)) }, [])
  useEffect(() => { loadRestaurants(0) }, [status])
  useEffect(() => { loadUsers() }, [role])
  useEffect(() => { loadOrders() }, [orderStatus])
  useEffect(() => { loadVouchers() }, [])

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
  const createVoucher = async (e) => {
    e.preventDefault(); setMsg('')
    try {
      await createAdminVoucher({ code: vform.code, type: vform.type, value: vform.value ? Number(vform.value) : 0, minOrder: vform.minOrder ? Number(vform.minOrder) : 0, maxDiscount: vform.maxDiscount ? Number(vform.maxDiscount) : null, usageLimit: vform.usageLimit ? Number(vform.usageLimit) : null, restaurantId: vform.restaurantId ? Number(vform.restaurantId) : null })
      setVform({ code: '', type: 'FIXED', value: '', minOrder: '', maxDiscount: '', usageLimit: '', restaurantId: '' })
      await loadVouchers(); setMsg('Đã tạo voucher!')
    } catch (ex) { setMsg(apiError(ex)) }
  }
  const toggleVoucher = async (v) => {
    try { await updateAdminVoucher(v.id, { active: !v.active }); setVouchers((p) => p.map((x) => x.id === v.id ? { ...x, active: !v.active } : x)) }
    catch (ex) { setMsg(apiError(ex)) }
  }
  const refund = async (o) => {
    const id = o.order_id ?? o.id
    try { await refundPayment(id); setMsg(`Đã hoàn tiền đơn #${id}`); await loadOrders() }
    catch (ex) { setMsg(apiError(ex, 'Hoàn tiền thất bại')) }
  }
  const loadReport = async () => {
    setMsg('')
    try {
      const r = await adminReport(repFrom || null, repTo || null)
      setReport(r)
    } catch (ex) { setMsg(apiError(ex, 'Tải báo cáo thất bại')) }
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
          {r.imageUrl && <img src={resolveImage(r.imageUrl)} onError={withFallback} alt="" style={{ width: 56, height: 42, objectFit: 'cover', borderRadius: 8 }} />}
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
          <span style={{ flex: 1 }}>#{o.order_id ?? o.id} — {o.status} — {Number(o.total_amount ?? o.totalAmount ?? 0).toLocaleString('vi-VN')}đ · {(o.pay_method ?? o.payMethod) || ''}/{(o.pay_status ?? o.payStatus) || ''}{(o.discount_amount ?? o.discountAmount) > 0 ? ` · giảm ${Number(o.discount_amount ?? o.discountAmount).toLocaleString('vi-VN')}đ (${o.voucher_code ?? o.voucherCode})` : ''}</span>
          {(o.status === 'CANCELLED') && (o.pay_method ?? o.payMethod) === 'ONLINE' && (o.pay_status ?? o.payStatus) === 'PAID' && <button onClick={() => refund(o)}>Hoàn tiền</button>}
          {(o.status === 'CANCELLED') && (o.pay_method ?? o.payMethod) === 'ONLINE' && (o.pay_status ?? o.payStatus) === 'REFUNDED' && <small>Đã hoàn tiền</small>}
        </div>
      ))}
      {orders.length === 0 && <p>Không có đơn.</p>}

      <h3>Voucher toàn sàn / của quán</h3>
      <form onSubmit={createVoucher} style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
        <input placeholder="Mã, vd TET2026" value={vform.code} onChange={(e) => setVform({ ...vform, code: e.target.value.toUpperCase() })} required style={{ width: 130 }} />
        <select value={vform.type} onChange={(e) => setVform({ ...vform, type: e.target.value })}>
          <option value="FIXED">Giảm tiền</option>
          <option value="PERCENT">Giảm %</option>
          <option value="FREESHIP">Freeship</option>
        </select>
        {vform.type !== 'FREESHIP' && <input placeholder={vform.type === 'PERCENT' ? '% (1-100)' : 'Số tiền giảm'} type="number" min="0" value={vform.value} onChange={(e) => setVform({ ...vform, value: e.target.value })} style={{ width: 110 }} />}
        <input placeholder="Đơn tối thiểu" type="number" min="0" value={vform.minOrder} onChange={(e) => setVform({ ...vform, minOrder: e.target.value })} style={{ width: 110 }} />
        {vform.type === 'PERCENT' && <input placeholder="Giảm tối đa" type="number" min="0" value={vform.maxDiscount} onChange={(e) => setVform({ ...vform, maxDiscount: e.target.value })} style={{ width: 110 }} />}
        <input placeholder="Lượt dùng" type="number" min="1" value={vform.usageLimit} onChange={(e) => setVform({ ...vform, usageLimit: e.target.value })} style={{ width: 90 }} />
        <input placeholder="restaurantId (trống = toàn sàn)" type="number" min="1" value={vform.restaurantId} onChange={(e) => setVform({ ...vform, restaurantId: e.target.value })} style={{ width: 180 }} />
        <button type="submit">Tạo voucher</button>
      </form>
      {vouchers.map((v) => (
        <div key={v.id} className="card row">
          <span style={{ flex: 1 }}><b>{v.code}</b> — {v.type} · tối thiểu {Number(v.minOrder).toLocaleString('vi-VN')}đ · đã dùng {v.usedCount}{v.usageLimit ? `/${v.usageLimit}` : ''} · {v.restaurantId ? `quán #${v.restaurantId}` : 'toàn sàn'} · {v.active ? 'Đang bật' : 'Đã tắt'}</span>
          <button onClick={() => toggleVoucher(v)}>{v.active ? 'Tắt' : 'Bật'}</button>
        </div>
      ))}
      {vouchers.length === 0 && <p>Chưa có voucher.</p>}

      <h3>Báo cáo toàn sàn</h3>
      <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', alignItems: 'center' }}>
        <label>Từ <input type="date" value={repFrom} onChange={(e) => setRepFrom(e.target.value)} /></label>
        <label>Đến <input type="date" value={repTo} onChange={(e) => setRepTo(e.target.value)} /></label>
        <button onClick={loadReport}>Xem báo cáo</button>
      </div>
      {report && (
        <div className="card" style={{ marginTop: 8 }}>
          <p>Tổng đơn: <b>{report.totalOrders}</b> · Hoàn thành: <b>{report.completedOrders}</b> · Hủy: <b>{report.cancelledOrders}</b> ({Number(report.cancelRate).toFixed(1)}%)</p>
          <p>Doanh thu: <b>{Number(report.revenue).toLocaleString('vi-VN')}đ</b> · Giảm giá: <b>{Number(report.discount).toLocaleString('vi-VN')}đ</b> · TB/đơn: <b>{Number(report.avgOrderValue).toLocaleString('vi-VN')}đ</b></p>
          <p>Khách: <b>{report.totalCustomers}</b> · Quán: <b>{report.totalMerchants}</b></p>
          <b>Doanh thu theo ngày</b>
          <div style={{ display: 'flex', gap: 4, alignItems: 'flex-end', margin: '8px 0', minHeight: 70 }}>
            {report.daily.map((d) => {
              const m = maxOf(report.daily, 'revenue') || 1
              const h = Math.max(4, Math.round((Number(d.revenue) / m) * 64))
              return (
                <div key={d.date} title={`${d.date}: ${d.orders} đơn, ${Number(d.revenue).toLocaleString('vi-VN')}đ`} style={{ flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 2 }}>
                  <div style={{ width: '100%', height: h, background: '#2196f3', borderRadius: 4 }} />
                  <small>{String(d.date).slice(5)}</small>
                </div>
              )
            })}
          </div>
          <b>Theo trạng thái</b>
          {report.byStatus.map((s) => (
            <div key={s.status}><small>{s.status}: {s.orders} đơn · {Number(s.revenue).toLocaleString('vi-VN')}đ</small></div>
          ))}
          <b style={{ marginTop: 8, display: 'block' }}>Theo thanh toán</b>
          {report.byPayment.map((s) => (
            <div key={`${s.method}-${s.status}`}><small>{s.method}/{s.status}: {s.count} đơn</small></div>
          ))}
          <b style={{ marginTop: 8, display: 'block' }}>Top quán</b>
          {report.topRestaurants.map((t) => (
            <div key={t.restaurantId}><small>{t.name}: {t.orders} đơn · {Number(t.revenue).toLocaleString('vi-VN')}đ · ★ {t.avgRating} ({t.ratingCount})</small></div>
          ))}
          <b style={{ marginTop: 8, display: 'block' }}>Món bán chạy</b>
          {report.topDishes.map((t) => (
            <div key={t.itemId}><small>{t.itemName}: {t.quantity} phần · {Number(t.revenue).toLocaleString('vi-VN')}đ</small></div>
          ))}
          {report.topDishes.length === 0 && <p><small>Chưa có dữ liệu món.</small></p>}
        </div>
      )}
    </section>
  )
}
