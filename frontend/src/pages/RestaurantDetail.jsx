import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { getMenu, getRestaurant, getReviews } from '../services/restaurant.js'
import { resolveImage, withFallback, FALLBACK_IMG } from '../services/upload.js'
import { useCart } from '../context/CartContext.jsx'

export default function RestaurantDetail() {
  const { id } = useParams()
  const { addItem } = useCart()
  const [r, setR] = useState(null)
  const [menu, setMenu] = useState([])
  const [reviews, setReviews] = useState([])
  const [added, setAdded] = useState(null)

  useEffect(() => {
    getRestaurant(id).then(setR).catch(() => setR(null))
    getMenu(id).then(setMenu).catch(() => setMenu([]))
    getReviews(id).then(({ data }) => setReviews(data)).catch(() => setReviews([]))
  }, [id])

  if (!r) return (<div className="grid" style={{ marginTop: 20 }}>{Array.from({ length: 4 }).map((_, i) => <div key={i} className="skeleton" />)}</div>)

  const add = (m) => {
    addItem(r.id, m, 1)
    setAdded(m.id)
    setTimeout(() => setAdded(null), 1200)
  }

  return (
    <section>
      <Link to="/" style={{ fontSize: 13, fontWeight: 700, color: '#ee4d2d', textDecoration: 'none' }}>← Về trang chủ</Link>
      <div className="detail-hero">
        <img className="banner" src={resolveImage(r.imageUrl) || FALLBACK_IMG} onError={withFallback} alt={r.name} />
        <div className="overlay">
          <h2>{r.name}</h2>
          <p>{r.address} · {r.openTime || '--'} - {r.closeTime || '--'}</p>
        </div>
      </div>

      <div className="info-chips">
        <span className="chip">★ {Number(r.averageRating ?? 0).toFixed(1)} · {r.ratingCount ?? 0} đánh giá</span>
        <span className="chip">🛵 Miễn ship đơn ≥ 200k</span>
        <span className="chip">⏱ Giao ~30 phút</span>
      </div>
      {r.description && <p style={{ color: '#4b5563', maxWidth: 720 }}>{r.description}</p>}

      <div className="section-head"><div><h2>Thực đơn</h2><p>{menu.length} món đang bán</p></div></div>
      <div className="grid">
        {menu.map((m) => (
          <div key={m.id} className="card hover dish-card">
            <img className="cover" style={{ height: 150 }} src={resolveImage(m.imageUrl) || FALLBACK_IMG} onError={withFallback} alt={m.name} loading="lazy" />
            <div className="dish-body">
              <h4 style={{ margin: '6px 0 0' }}>{m.name}</h4>
              <p className="desc">{m.description || 'Món ngon của quán'}</p>
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginTop: 8 }}>
                <span className="price">{Number(m.price).toLocaleString('vi-VN')}đ</span>
                <button className="small" onClick={() => add(m)}>{added === m.id ? '✓ Đã thêm' : '+ Thêm'}</button>
              </div>
            </div>
          </div>
        ))}
      </div>
      {menu.length === 0 && <div className="card" style={{ textAlign: 'center' }}><p>Quán chưa có món đang bán.</p></div>}

      <div className="section-head"><div><h2>Đánh giá</h2><p>Cảm nhận từ khách hàng</p></div></div>
      {reviews.map((rv) => (
        <div key={rv.id} className="card" style={{ marginBottom: 10 }}>
          <b>★ {rv.rating}</b>
          <p style={{ margin: '4px 0 0', color: '#374151' }}>{rv.comment || 'Không có nhận xét.'}</p>
        </div>
      ))}
      {reviews.length === 0 && <p style={{ color: '#6b7280' }}>Chưa có đánh giá — hãy là người đầu tiên sau khi đặt món nhé.</p>}
    </section>
  )
}
