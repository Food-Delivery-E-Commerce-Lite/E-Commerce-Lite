import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import api from '../services/api.js'
import { searchRestaurants } from '../services/restaurant.js'

export default function Home() {
  const [status, setStatus] = useState('đang kiểm tra...')
  const [keyword, setKeyword] = useState('')
  const [items, setItems] = useState([])
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    api.get('/health').then((res) => setStatus(res.data.status)).catch(() => setStatus('không kết nối được backend'))
  }, [])

  const load = async (p = 0, kw = keyword) => {
    setLoading(true)
    try {
      const { data, page: pg } = await searchRestaurants({ keyword: kw, page: p, size: 12 })
      setItems(data); setPage(p); setTotalPages(pg?.totalPages ?? 0)
    } finally { setLoading(false) }
  }
  useEffect(() => { load(0, '') }, [])

  return (
    <section>
      <h1>Danh sách nhà hàng</h1>
      <p>Trạng thái API: <b>{status}</b></p>
      <form onSubmit={(e) => { e.preventDefault(); load(0) }} style={{ display: 'flex', gap: 8, marginBottom: 16 }}>
        <input value={keyword} onChange={(e) => setKeyword(e.target.value)} placeholder="Tìm phở, cơm, trà sữa..." style={{ flex: 1 }} />
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
      {items.length === 0 && !loading && <p>Chưa có nhà hàng nào được duyệt.</p>}
      <div style={{ marginTop: 16, display: 'flex', gap: 8 }}>
        <button disabled={page <= 0} onClick={() => load(page - 1)}>Trang trước</button>
        <span>Trang {page + 1}/{Math.max(totalPages, 1)}</span>
        <button disabled={page + 1 >= totalPages} onClick={() => load(page + 1)}>Trang sau</button>
      </div>
    </section>
  )
}
