import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import api, { unwrap, apiError } from '../services/api.js'
import { cancelOrder, createReview, getOrder, refundPayment } from '../services/order.js'
import { subscribeOrder } from '../services/realtime.js'

const TERMINAL = ['COMPLETED', 'CANCELLED']
const FLOW = ['PENDING', 'CONFIRMED', 'PREPARING', 'DELIVERING', 'COMPLETED']
const STATUS_VN = { PENDING: 'Chờ xác nhận', CONFIRMED: 'Đã xác nhận', PREPARING: 'Đang nấu', DELIVERING: 'Đang giao', COMPLETED: 'Hoàn thành', CANCELLED: 'Đã hủy' }

export default function OrderDetail() {
  const { id } = useParams()
  const [o, setO] = useState(null)
  const [history, setHistory] = useState([])
  const [rating, setRating] = useState(5)
  const [comment, setComment] = useState('')
  const [msg, setMsg] = useState('')
  const [live, setLive] = useState(false)

  const reload = async (silent = false) => {
    try {
      const fresh = await getOrder(id)
      setO(fresh)
      const r = await api.get(`/orders/${id}/history`)
      setHistory(unwrap(r).data ?? [])
    } catch (ex) { if (!silent) setMsg(apiError(ex)) }
  }

  useEffect(() => {
    reload()
    let unsub = null
    try {
      unsub = subscribeOrder(id, () => { setLive(true); reload(true) })
      const t = setTimeout(() => setLive(true), 1500)
      return () => { clearTimeout(t); unsub?.() }
    } catch { setLive(false) }
    return () => unsub?.()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id])

  useEffect(() => {
    if (!o || TERMINAL.includes(o.status)) return
    const t = setInterval(() => reload(true), 15000)
    return () => clearInterval(t)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id, o?.status])

  if (!o) return (<div className="skeleton" style={{ marginTop: 20, minHeight: 300 }} />)
  const cancel = async () => { try { setO(await cancelOrder(id)) } catch (ex) { setMsg(apiError(ex)) } }
  const refund = async () => {
    setMsg('')
    try { await refundPayment(id); await reload(); setMsg('Đã hoàn tiền ONLINE!') }
    catch (ex) { setMsg(apiError(ex, 'Hoàn tiền thất bại')) }
  }
  const review = async () => {
    setMsg('')
    try { await createReview({ orderId: Number(id), rating: Number(rating), comment }); setMsg('Cảm ơn bạn đã đánh giá!') }
    catch (ex) { setMsg(apiError(ex)) }
  }
  const stepIdx = FLOW.indexOf(o.status)
  return (
    <section>
      <Link to="/orders" style={{ fontSize: 13, fontWeight: 700, color: '#ee4d2d', textDecoration: 'none' }}>← Đơn của tôi</Link>
      <div className="card" style={{ marginTop: 10 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 8 }}>
          <h2 style={{ margin: 0 }}>Đơn #{o.id}</h2>
          <span className="badge pending">{STATUS_VN[o.status] || o.status}</span>
        </div>
        <p style={{ fontSize: 13, color: '#6b7280', margin: '4px 0' }}>{live ? '⚡ Cập nhật realtime' : 'Đang kết nối realtime...'}</p>
        {!TERMINAL.includes(o.status) && stepIdx >= 0 && (
          <div style={{ display: 'flex', gap: 4, margin: '12px 0' }}>
            {FLOW.map((s, i) => (
              <div key={s} style={{ flex: 1, textAlign: 'center' }}>
                <div style={{ height: 6, borderRadius: 99, background: i <= stepIdx ? '#ee4d2d' : '#f3f4f6' }} />
                <small style={{ fontSize: 10, color: i <= stepIdx ? '#c93a1f' : '#9ca3af', fontWeight: 700 }}>{STATUS_VN[s]}</small>
              </div>
            ))}
          </div>
        )}
        <div className="stat-grid">
          <div className="stat"><small>Tạm tính</small><b style={{ fontSize: 16 }}>{Number(o.subtotal ?? 0).toLocaleString('vi-VN')}đ</b></div>
          <div className="stat"><small>Phí ship</small><b style={{ fontSize: 16 }}>{Number(o.deliveryFee ?? 0).toLocaleString('vi-VN')}đ</b></div>
          <div className="stat"><small>Giảm giá</small><b style={{ fontSize: 16, color: '#16a34a' }}>{Number(o.discountAmount ?? 0) > 0 ? `−${Number(o.discountAmount).toLocaleString('vi-VN')}đ` : '0đ'}</b>{o.voucherCode && <small> · {o.voucherCode}</small>}</div>
          <div className="stat"><small>Tổng trả</small><b className="price">{Number(o.totalAmount).toLocaleString('vi-VN')}đ</b><small> · {o.payMethod}/{o.payStatus}</small></div>
        </div>
        {o.status === 'CANCELLED' && o.payMethod === 'ONLINE' && o.payStatus === 'REFUNDED' && <p className="success">Đã hoàn tiền online cho đơn hủy.</p>}
        {msg && <p className={msg.includes('Cảm ơn') || msg.includes('hoàn tiền') ? 'success' : 'error'}>{msg}</p>}
        <div style={{ display: 'flex', gap: 8, marginTop: 8 }}>
          {(o.status === 'PENDING' || o.status === 'CONFIRMED') && <button onClick={cancel}>Hủy đơn</button>}
          {o.status === 'CANCELLED' && o.payMethod === 'ONLINE' && o.payStatus === 'PAID' && <button onClick={refund}>Hoàn tiền ONLINE</button>}
        </div>
      </div>
      <div className="card" style={{ marginTop: 12 }}>
        <b>Món đã đặt ({o.items.length})</b>
        {o.items.map((i) => (<div key={i.itemId} className="card row" style={{ boxShadow: 'none' }}><span style={{ flex: 1 }}>{i.itemName} × {i.quantity}</span><b>{Number(i.lineTotal).toLocaleString('vi-VN')}đ</b></div>))}
      </div>
      {o.status === 'COMPLETED' && (
        <div className="card" style={{ marginTop: 12 }}><h3 style={{ marginTop: 0 }}>Đánh giá món ăn ⭐</h3>
          <div style={{ display: 'flex', gap: 6 }}>
            {[5, 4, 3, 2, 1].map((v) => (
              <button key={v} className={rating === v ? '' : 'ghost'} onClick={() => setRating(v)}>★ {v}</button>
            ))}
          </div>
          <input placeholder="Nhận xét về món ăn, quán..." value={comment} onChange={(e) => setComment(e.target.value)} style={{ width: '100%', marginTop: 8 }} />
          <button onClick={review} style={{ marginTop: 8 }}>Gửi đánh giá</button>
        </div>
      )}
      <div className="card" style={{ marginTop: 12 }}>
        <b>Lịch sử trạng thái</b>
        {history.map((h) => (<div key={h.id} style={{ fontSize: 13, color: '#4b5563', padding: '4px 0', borderBottom: '1px dashed #f3f4f6' }}><small>{h.fromStatus || '—'} → <b>{h.toStatus}</b> · {h.changedAt}</small></div>))}
        {history.length === 0 && <p style={{ color: '#9ca3af', fontSize: 13 }}>Chưa có lịch sử.</p>}
      </div>
    </section>
  )
}
