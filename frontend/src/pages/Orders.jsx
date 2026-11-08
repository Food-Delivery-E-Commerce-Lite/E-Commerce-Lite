import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { myOrders } from '../services/order.js'

export default function Orders() {
  const [items, setItems] = useState([])
  useEffect(() => { myOrders().then(({ data }) => setItems(data)).catch(() => setItems([])) }, [])
  return (
    <section>
      <h2>Đơn hàng của tôi</h2>
      {items.map((o) => (
        <Link key={o.id} to={`/orders/${o.id}`} className="card row">
          <b>#{o.id}</b><span style={{ flex: 1 }}>{o.status} · {Number(o.totalAmount).toLocaleString('vi-VN')}đ</span>
        </Link>
      ))}
      {items.length === 0 && <p>Chưa có đơn nào.</p>}
    </section>
  )
}
