import { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import api, { unwrap } from '../services/api.js'
import { cancelOrder, createReview, getOrder } from '../services/order.js'
import { apiError } from '../services/api.js'

export default function OrderDetail() {
  const { id } = useParams()
  const [o, setO] = useState(null)
  const [history, setHistory] = useState([])
  const [rating, setRating] = useState(5)
  const [comment, setComment] = useState('')
  const [msg, setMsg] = useState('')
  useEffect(() => {
    getOrder(id).then(setO).catch(() => setO(null))
    api.get(`/orders/${id}/history`).then((r) => setHistory(unwrap(r).data ?? [])).catch(() => setHistory([]))
  }, [id])
  if (!o) return <p>Đang tải đơn...</p>
  const cancel = async () => { try { setO(await cancelOrder(id)) } catch (ex) { setMsg(apiError(ex)) } }
  const review = async () => {
    setMsg('')
    try { await createReview({ orderId: Number(id), rating: Number(rating), comment }); setMsg('Cảm ơn bạn đã đánh giá!') }
    catch (ex) { setMsg(apiError(ex)) }
  }
  return (
    <section>
      <h2>Đơn #{o.id} — {o.status}</h2>
      <p>Tổng: {Number(o.totalAmount).toLocaleString('vi-VN')}đ · {o.payMethod} / {o.payStatus}</p>
      {o.items.map((i) => (<div key={i.itemId} className="card row"><span style={{ flex: 1 }}>{i.itemName} x{i.quantity}</span><b>{Number(i.lineTotal).toLocaleString('vi-VN')}đ</b></div>))}
      {(o.status === 'PENDING' || o.status === 'CONFIRMED') && <button onClick={cancel}>Hủy đơn</button>}
      {o.status === 'COMPLETED' && (
        <div className="card"><h3>Đánh giá</h3>
          <select value={rating} onChange={(e) => setRating(e.target.value)}>{[5, 4, 3, 2, 1].map((v) => <option key={v} value={v}>{v} sao</option>)}</select>
          <input placeholder="Nhận xét..." value={comment} onChange={(e) => setComment(e.target.value)} style={{ width: '100%', marginTop: 8 }} />
          <button onClick={review} style={{ marginTop: 8 }}>Gửi đánh giá</button>
        </div>
      )}
      {msg && <p>{msg}</p>}
      <h3>Lịch sử trạng thái</h3>
      {history.map((h) => (<div key={h.id}><small>{h.fromStatus || '—'} → {h.toStatus} lúc {h.changedAt}</small></div>))}
    </section>
  )
}
