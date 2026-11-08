import { useEffect, useState } from 'react'
import { createMenuItem, createRestaurant, merchantChangeStatus, merchantOrders, myRestaurants, updateAvailability, updateRestaurant } from '../services/merchant.js'
import { getCategories } from '../services/restaurant.js'
import { apiError } from '../services/api.js'

const FLOW = ['PENDING', 'CONFIRMED', 'PREPARING', 'DELIVERING', 'COMPLETED']

export default function Merchant() {
  const [shops, setShops] = useState([])
  const [selected, setSelected] = useState(null)
  const [orders, setOrders] = useState([])
  const [filter, setFilter] = useState('PENDING')
  const [cats, setCats] = useState([])
  const [form, setForm] = useState({ name: '', address: '', description: '' })
  const [menuForm, setMenuForm] = useState({ name: '', price: '', description: '', categoryId: '' })
  const [msg, setMsg] = useState('')

  const reload = async () => {
    const { data } = await myRestaurants().catch(() => ({ data: [] }))
    setShops(data)
    if (data.length > 0 && !selected) setSelected(data[0])
  }
  useEffect(() => { reload(); getCategories().then(setCats).catch(() => setCats([])) }, [])

  useEffect(() => {
    if (selected) merchantOrders(selected.id, filter).then(setOrders).catch(() => setOrders([]))
  }, [selected, filter])

  const create = async (e) => {
    e.preventDefault(); setMsg('')
    try { await createRestaurant(form); setForm({ name: '', address: '', description: '' }); await reload() }
    catch (ex) { setMsg(apiError(ex)) }
  }
  const addDish = async (e) => {
    e.preventDefault(); setMsg('')
    try {
      await createMenuItem(selected.id, { name: menuForm.name, price: Number(menuForm.price), description: menuForm.description, categoryId: menuForm.categoryId ? Number(menuForm.categoryId) : null })
      setMenuForm({ name: '', price: '', description: '', categoryId: '' }); setMsg('Đã thêm món!')
    } catch (ex) { setMsg(apiError(ex)) }
  }
  const advance = async (o) => {
    const i = FLOW.indexOf(o.status)
    if (i < 0 || i + 1 >= FLOW.length) return
    try { await merchantChangeStatus(o.id, FLOW[i + 1]); setOrders((p) => p.map((x) => x.id === o.id ? { ...x, status: FLOW[i + 1] } : x)) }
    catch (ex) { setMsg(apiError(ex)) }
  }

  return (
    <section>
      <h2>Kênh người bán</h2>
      {msg && <p>{msg}</p>}
      <div className="card"><h3>Tạo quán mới (duyệt PENDING)</h3>
        <form onSubmit={create} style={{ display: 'grid', gap: 8 }}>
          <input placeholder="Tên quán" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} required />
          <input placeholder="Địa chỉ" value={form.address} onChange={(e) => setForm({ ...form, address: e.target.value })} required />
          <input placeholder="Mô tả" value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} />
          <button type="submit">Tạo quán</button>
        </form>
      </div>
      <h3>Quán của tôi</h3>
      {shops.map((s) => (
        <div key={s.id} className="card row">
          <div style={{ flex: 1 }}><b>{s.name}</b> — {s.status}{s.status === 'PENDING' && s.averageRating !== undefined ? ' (sửa sẽ phải duyệt lại)' : ''}<div><small>{s.address}</small></div></div>
          <button onClick={() => setSelected(s)}>Quản lý</button>
        </div>
      ))}
      {selected && (
        <div className="card"><h3>Đang quản lý: {selected.name}</h3>
          <form onSubmit={addDish} style={{ display: 'grid', gap: 8 }}>
            <b>Thêm món</b>
            <input placeholder="Tên món" value={menuForm.name} onChange={(e) => setMenuForm({ ...menuForm, name: e.target.value })} required />
            <input placeholder="Giá" type="number" value={menuForm.price} onChange={(e) => setMenuForm({ ...menuForm, price: e.target.value })} required />
            <input placeholder="Mô tả" value={menuForm.description} onChange={(e) => setMenuForm({ ...menuForm, description: e.target.value })} />
            <select value={menuForm.categoryId} onChange={(e) => setMenuForm({ ...menuForm, categoryId: e.target.value })}>
              <option value="">-- Danh mục --</option>
              {cats.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
            </select>
            <button type="submit">Thêm món</button>
          </form>
          <h4>Đơn hàng — lọc:</h4>
          <select value={filter} onChange={(e) => setFilter(e.target.value)}>
            {[...FLOW, 'CANCELLED'].map((s) => <option key={s} value={s}>{s}</option>)}
          </select>
          {orders.map((o) => (
            <div key={o.id} className="card row">
              <span style={{ flex: 1 }}>#{o.id} — {o.status} — {Number(o.totalAmount).toLocaleString('vi-VN')}đ</span>
              {!['COMPLETED', 'CANCELLED'].includes(o.status) && <button onClick={() => advance(o)}>Duyệt tiếp</button>}
            </div>
          ))}
          {orders.length === 0 && <p>Không có đơn {filter}.</p>}
        </div>
      )}
    </section>
  )
}
