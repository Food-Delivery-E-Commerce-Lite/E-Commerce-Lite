import { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import { getMenu, getRestaurant, getReviews } from '../services/restaurant.js'
import { useCart } from '../context/CartContext.jsx'

export default function RestaurantDetail() {
  const { id } = useParams()
  const { addItem } = useCart()
  const [r, setR] = useState(null)
  const [menu, setMenu] = useState([])
  const [reviews, setReviews] = useState([])

  useEffect(() => {
    getRestaurant(id).then(setR).catch(() => setR(null))
    getMenu(id).then(setMenu).catch(() => setMenu([]))
    getReviews(id).then(({ data }) => setReviews(data)).catch(() => setReviews([]))
  }, [id])

  if (!r) return <p>Đang tải nhà hàng...</p>
  return (
    <section>
      <h2>{r.name}</h2>
      <p>{r.description}</p>
      <p>{r.address} · {r.openTime || ''} - {r.closeTime || ''}</p>
      <p>★ {r.averageRating} ({r.ratingCount} đánh giá)</p>
      <h3>Thực đơn</h3>
      <div className="grid">
        {menu.map((m) => (
          <div key={m.id} className="card">
            <h4>{m.name}</h4>
            <p>{m.description}</p>
            <b>{Number(m.price).toLocaleString('vi-VN')}đ</b>
            <div><button onClick={() => addItem(r.id, m, 1)}>Thêm vào giỏ</button></div>
          </div>
        ))}
      </div>
      {menu.length === 0 && <p>Quán chưa có món đang bán.</p>}
      <h3>Đánh giá</h3>
      {reviews.map((rv) => (<div key={rv.id} className="card"><b>★ {rv.rating}</b><p>{rv.comment}</p></div>))}
      {reviews.length === 0 && <p>Chưa có đánh giá.</p>}
    </section>
  )
}
