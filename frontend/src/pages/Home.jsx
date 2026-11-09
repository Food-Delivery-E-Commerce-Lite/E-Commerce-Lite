import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import api from '../services/api.js'
import { getCategories, searchRestaurants } from '../services/restaurant.js'

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
    api.get('/health').then((res) => setStatus(res.data.status)).catch(() => setStatus('không kết nối được backend'))
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

  return (
    <section>
      <h1>Danh sách nhà hàng</h1>
      <p>Trạng thái API: <b>{status}</b></p>
      <form onSubmit={(e) => { e.preventDefault(); load(0) }} style={{ display: 'flex', gap: 8, marginBottom: 12, flexWrap: 'wrap' }}>
        <input value={keyword} onChange={(e) => setKeyword(e.target.value)} placeholder="Tìm phở, cơm, trà sữa..." style={{ flex: 2, minWidth: 180 }} />
        <select value={categoryId} onChange={(e) => setCategoryId(e.target.value)}>
          <option value="">Tất cả danh mục</option>
          {cats.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
        </select>
        <select value={minRating} onChange={(e) => setMinRating(e.target.value)}>
          <option value="">Mọi rating</option>
          <option value="4">★ 4+</option>
          <option value="3">★ 3+</option>
        </select>
        <select value={sort} onChange={(e) => setSort(e.target.value)}>
          <option value="newest">Mới nhất</option>
          <option value="rating">Đánh giá cao</option>
          <option value="name">Tên A-Z</option>
        </select>
        <button type="submit">Tìm kiếm</button>
      </form>
      {loading ? <p>Đang tải...</p> : (
        <div className="grid">
          {items.map((r) => (
            <Link key={r.id} to={`/restaurants/${r.id}`} className="card">
              <h3>{r.name}</h3>
              <p>{r.address}</p>
              <small>★ {r.averageRating} ({r.ratingCount}) · {r.status}</small>
            </Link>
          ))}
        </div>
      )}
      {items.length === 0 && !loading && <p>Không tìm thấy nhà hàng phù hợp.</p>}
      <div style={{ marginTop: 16, display: 'flex', gap: 8 }}>
        <button disabled={page <= 0} onClick={() => load(page - 1)}>Trang trước</button>
        <span>Trang {page + 1}/{Math.max(totalPages, 1)}</span>
        <button disabled={page + 1 >= totalPages} onClick={() => load(page + 1)}>Trang sau</button>
      </div>
    </section>
  )
}
