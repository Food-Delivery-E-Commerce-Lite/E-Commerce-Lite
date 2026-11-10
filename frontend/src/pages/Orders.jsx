import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { myOrders } from '../services/order.js'

const STATUS_VN = { PENDING: 'Chờ xác nhận', CONFIRMED: 'Đã xác nhận', PREPARING: 'Đang nấu', DELIVERING: 'Đang giao', COMPLETED: 'Hoàn thành', CANCELLED: 'Đã hủy' }

export default function Orders() {
  const [items, setItems] = useState([])
  useEffect(() => { myOrders().then(({ data }) => setItems(data)).catch(() => setItems([])) }, [])
  return (
    <section>
      <div className="section-head"><div><h2>Đơn hàng của tôi</h2><p>{items.length} đơn gần nhất</p></div></div>
      {items.map((o) => (
        <Link key={o.id} to={`/orders/${o.id}`} className="card row hover">
          <b>#{o.id}</b>
          <span style={{ flex: 1 }}><span className="badge pending">{STATUS_VN[o.status] || o.status}</span> <span style={{ marginLeft: 8 }}>{Number(o.totalAmount).toLocaleString('vi-VN')}đ</span></span>
          <span style={{ fontSize: 13, color: '#6b7280' }}>{o.payMethod}/{o.payStatus} →</span>
        </Link>
      ))}
      {items.length === 0 && <div className="card" style={{ textAlign: 'center', padding: 32 }}><div style={{ fontSize: 44 }}>📦</div><p>Chưa có đơn nào. Đặt món đầu tiên ngay!</p><Link to="/"><button>Đặt món ngay</button></Link></div>}
    </section>
  )
}
