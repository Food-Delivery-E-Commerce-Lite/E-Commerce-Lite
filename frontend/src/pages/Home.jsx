import { useEffect, useState } from 'react'
import api from '../services/api.js'

export default function Home() {
  const [status, setStatus] = useState('đang kiểm tra...')

  useEffect(() => {
    api.get('/health')
      .then((res) => setStatus(res.data.status))
      .catch(() => setStatus('không kết nối được backend'))
  }, [])

  return (
    <section>
      <h1>Danh sách nhà hàng</h1>
      <p>Trạng thái API: <b>{status}</b></p>
      {/* TODO: danh sách nhà hàng, tìm kiếm, lọc, phân trang */}
    </section>
  )
}
