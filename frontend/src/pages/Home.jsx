import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import api from '../services/api.js'
import { getCategories, searchRestaurants } from '../services/restaurant.js'
import { resolveImage, withFallback, FALLBACK_IMG } from '../services/upload.js'

export default function Home() {
  const [status, setStatus] = useState('đang kiểm tra...')
  const [keyword, setKeyword] = useState('')
  const [cats, setCats] = useState([])
  const [categoryId, setCategoryId] = useState('')
  const [minRating, setMinRating] = useState('')
  const [sort, setSort] = useState('newest')
  const [items, setItems] = useState([])
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    api.get('/health').then((res) => setStatus(res.data.status)).catch(() => setStatus('mất kết nối'))
    getCategories().then(setCats).catch(() => setCats([]))
  }, [])

  const load = async (p = 0) => {
    setLoading(true)
    try {
      const { data, page: pg } = await searchRestaurants({
        keyword, page: p, size: 12,
        categoryId: categoryId ? Number(categoryId) : null,
        minRating: minRating ? Number(minRating) : null,
        sort,
      })
      setItems(data); setPage(p); setTotalPages(pg?.totalPages ?? 0)
    } finally { setLoading(false) }
  }
  useEffect(() => { load(0) }, [])

  const online = status === 'UP'

  return (
    <section>
      <div className="hero">
        <h1>Ăn ngon, giao nhanh ⚡</h1>
        <p>Hàng trăm quán ngon quanh bạn · Miễn phí ship đơn từ 200k · Voucher mỗi ngày.</p>
        <span className="api-dot"><span className="dot" style={{ background: online ? '#4ade80' : '#fbbf24' }} /> API: {online ? 'sẵn sàng' : status}</span>
      </div>

      <form className="toolbar" onSubmit={(e) => { e.preventDefault(); load(0) }}>
        <input className="search-input" value={keyword} onChange={(e) => setKeyword(e.target.value)} placeholder="Tìm phở, cơm gà, trà sữa..." />
        <select value={categoryId} onChange={(e) => setCategoryId(e.target.value)}>
          <option value="">Tất cả danh mục</option>
          {cats.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
        </select>
        <select value={minRating} onChange={(e) => setMinRating(e.target.value)}>
          <option value="">Mọi rating</option>
          <option value="4">★ 4.0 trở lên</option>
          <option value="3">★ 3.0 trở lên</option>
        </select>
        <select value={sort} onChange={(e) => setSort(e.target.value)}>
          <option value="newest">Mới nhất</option>
          <option value="rating">Đánh giá cao</option>
          <option value="name">Tên A-Z</option>
        </select>
        <button type="submit">Tìm kiếm</button>
      </form>

      <div className="section-head">
        <div><h2>Quán nổi bật</h2><p>{items.length > 0 ? `${items.length} quán đang mở bán` : 'Khám phá món ngon hôm nay'}</p></div>
      </div>

      {loading ? (
        <div className="grid">{Array.from({ length: 8 }).map((_, i) => <div key={i} className="skeleton" />)}</div>
      ) : (
        <div className="grid">
          {items.map((r) => (
            <Link key={r.id} to={`/restaurants/${r.id}`} className="card hover">
              <img className="cover" src={resolveImage(r.imageUrl) || FALLBACK_IMG} onError={withFallback} alt={r.name} loading="lazy" />
              <h3 className="rest-name">{r.name}</h3>
              <p className="rest-addr">{r.address}</p>
              <div className="meta-row">
                <span className="rating">★ {Number(r.averageRating ?? 0).toFixed(1)} <span style={{ fontWeight: 400 }}>({r.ratingCount ?? 0})</span></span>
                <span className="badge open">Đang mở</span>
                <span style={{ fontSize: 12, color: '#6b7280' }}>· Freeship 200k+</span>
              </div>
            </Link>
          ))}
        </div>
      )}
      {items.length === 0 && !loading && (
        <div className="card" style={{ textAlign: 'center', padding: 32 }}>
          <div style={{ fontSize: 44 }}>🍽️</div>
          <h3>Không tìm thấy quán phù hợp</h3>
          <p style={{ color: '#6b7280' }}>Thử từ khóa khác hoặc bỏ bộ lọc rating nhé.</p>
          <button className="ghost" onClick={() => { setKeyword(''); setCategoryId(''); setMinRating(''); setSort('newest'); load(0) }}>Xóa bộ lọc</button>
        </div>
      )}
      {totalPages > 1 && (
        <div className="pager">
          <button className="ghost small" disabled={page <= 0} onClick={() => load(page - 1)}>← Trước</button>
          <span>Trang {page + 1} / {Math.max(totalPages, 1)}</span>
          <button className="ghost small" disabled={page + 1 >= totalPages} onClick={() => load(page + 1)}>Sau →</button>
        </div>
      )}
    </section>
  )
}
