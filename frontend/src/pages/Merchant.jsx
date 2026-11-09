import { useEffect, useState } from 'react'
import { createMenuItem, createRestaurant, deleteMenuItem, getOwnedMenu, merchantChangeStatus, merchantOrders, merchantStats, myRestaurants, updateAvailability, updateMenuItem, updateRestaurant } from '../services/merchant.js'
import { getCategories } from '../services/restaurant.js'
import { apiError } from '../services/api.js'

const FLOW = ['PENDING', 'CONFIRMED', 'PREPARING', 'DELIVERING', 'COMPLETED']

export default function Merchant() {
  const [shops, setShops] = useState([])
  const [selected, setSelected] = useState(null)
  const [orders, setOrders] = useState([])
  const [ordersPage, setOrdersPage] = useState(null)
  const [filter, setFilter] = useState('')
  const [cats, setCats] = useState([])
  const [form, setForm] = useState({ name: '', address: '', description: '', openTime: '', closeTime: '' })
  const [editForm, setEditForm] = useState(null)
  const [menu, setMenu] = useState([])
  const [menuPage, setMenuPage] = useState(null)
  const [menuForm, setMenuForm] = useState({ name: '', price: '', description: '', categoryId: '', imageUrl: '' })
  const [editingItem, setEditingItem] = useState(null)
  const [stats, setStats] = useState(null)
  const [msg, setMsg] = useState('')

  const reload = async () => {
    const { data } = await myRestaurants().catch(() => ({ data: [] }))
    setShops(data)
    if (data.length > 0 && !selected) setSelected(data[0])
  }
  useEffect(() => { reload(); getCategories().then(setCats).catch(() => setCats([])) }, [])

  useEffect(() => {
    if (!selected) return
    merchantOrders(selected.id, filter || null).then(({ data, page }) => { setOrders(data); setOrdersPage(page) }).catch(() => setOrders([]))
    getOwnedMenu(selected.id, 0, 50).then(({ data, page }) => { setMenu(data); setMenuPage(page) }).catch(() => setMenu([]))
    merchantStats(selected.id).then(setStats).catch(() => setStats(null))
    setEditForm({ name: selected.name, address: selected.address, description: selected.description || '', openTime: selected.openTime || '', closeTime: selected.closeTime || '' })
  }, [selected])

  const create = async (e) => {
    e.preventDefault(); setMsg('')
    try { await createRestaurant(form); setForm({ name: '', address: '', description: '', openTime: '', closeTime: '' }); await reload() }
    catch (ex) { setMsg(apiError(ex)) }
  }
  const saveShop = async (e) => {
    e.preventDefault(); setMsg('')
    try {
      const updated = await updateRestaurant(selected.id, editForm)
      setSelected(updated); setMsg('Đã lưu. Quán đã duyệt sẽ phải duyệt lại.')
      await reload()
    } catch (ex) { setMsg(apiError(ex)) }
  }
  const addDish = async (e) => {
    e.preventDefault(); setMsg('')
    try {
      if (editingItem) {
        await updateMenuItem(editingItem, { name: menuForm.name, price: Number(menuForm.price), description: menuForm.description, imageUrl: menuForm.imageUrl || null, categoryId: menuForm.categoryId ? Number(menuForm.categoryId) : null })
        setEditingItem(null); setMsg('Đã cập nhật món!')
      } else {
        await createMenuItem(selected.id, { name: menuForm.name, price: Number(menuForm.price), description: menuForm.description, imageUrl: menuForm.imageUrl || null, categoryId: menuForm.categoryId ? Number(menuForm.categoryId) : null })
        setMsg('Đã thêm món!')
      }
      setMenuForm({ name: '', price: '', description: '', categoryId: '', imageUrl: '' })
      const { data } = await getOwnedMenu(selected.id, 0, 50).catch(() => ({ data: [] }))
      setMenu(data)
    } catch (ex) { setMsg(apiError(ex)) }
  }
  const toggleAvail = async (m) => {
    try { await updateAvailability(m.id, !m.available); setMenu((p) => p.map((x) => x.id === m.id ? { ...x, available: !m.available } : x)) }
    catch (ex) { setMsg(apiError(ex)) }
  }
  const removeDish = async (m) => {
    if (!confirm(`Xóa món "${m.name}"?`)) return
    try { await deleteMenuItem(m.id); setMenu((p) => p.filter((x) => x.id !== m.id)) }
    catch (ex) { setMsg(apiError(ex)) }
  }
  const advance = async (o) => {
    const i = FLOW.indexOf(o.status)
    if (i < 0 || i + 1 >= FLOW.length) return
    try { await merchantChangeStatus(o.id, FLOW[i + 1]); setOrders((p) => p.map((x) => x.id === o.id ? { ...x, status: FLOW[i + 1] } : x)) }
    catch (ex) { setMsg(apiError(ex)) }
  }
  const cancel = async (o) => {
    try { await merchantChangeStatus(o.id, 'CANCELLED'); setOrders((p) => p.map((x) => x.id === o.id ? { ...x, status: 'CANCELLED' } : x)) }
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
          <div style={{ display: 'flex', gap: 8 }}>
            <label>Mở cửa <input type="time" value={form.openTime} onChange={(e) => setForm({ ...form, openTime: e.target.value })} /></label>
            <label>Đóng cửa <input type="time" value={form.closeTime} onChange={(e) => setForm({ ...form, closeTime: e.target.value })} /></label>
          </div>
          <button type="submit">Tạo quán</button>
        </form>
      </div>
      <h3>Quán của tôi</h3>
      {shops.map((s) => (
        <div key={s.id} className="card row">
          <div style={{ flex: 1 }}><b>{s.name}</b> — {s.status}<div><small>{s.address} · {s.openTime || '--'} - {s.closeTime || '--'}</small></div></div>
          <button onClick={() => setSelected(s)}>Quản lý</button>
        </div>
      ))}
      {selected && (
        <div className="card"><h3>Đang quản lý: {selected.name}</h3>
          {stats && <p>Tổng đơn: <b>{stats.totalOrders}</b> · Doanh thu: <b>{Number(stats.revenue).toLocaleString('vi-VN')}đ</b> · Chờ: {stats.pending} · Hoàn thành: {stats.completed} · ★ {stats.avgRating} ({stats.ratingCount})</p>}
          <form onSubmit={saveShop} style={{ display: 'grid', gap: 8 }}>
            <b>Sửa quán (quán đã duyệt sẽ về PENDING duyệt lại)</b>
            <input value={editForm?.name || ''} onChange={(e) => setEditForm({ ...editForm, name: e.target.value })} required />
            <input value={editForm?.address || ''} onChange={(e) => setEditForm({ ...editForm, address: e.target.value })} required />
            <input value={editForm?.description || ''} onChange={(e) => setEditForm({ ...editForm, description: e.target.value })} placeholder="Mô tả" />
            <div style={{ display: 'flex', gap: 8 }}>
              <label>Mở <input type="time" value={editForm?.openTime || ''} onChange={(e) => setEditForm({ ...editForm, openTime: e.target.value })} /></label>
              <label>Đóng <input type="time" value={editForm?.closeTime || ''} onChange={(e) => setEditForm({ ...editForm, closeTime: e.target.value })} /></label>
            </div>
            <button type="submit">Lưu quán</button>
          </form>
          <form onSubmit={addDish} style={{ display: 'grid', gap: 8, marginTop: 12 }}>
            <b>{editingItem ? 'Sửa món' : 'Thêm món'}</b>
            <input placeholder="Tên món" value={menuForm.name} onChange={(e) => setMenuForm({ ...menuForm, name: e.target.value })} required />
            <input placeholder="Giá" type="number" min="1" value={menuForm.price} onChange={(e) => setMenuForm({ ...menuForm, price: e.target.value })} required />
            <input placeholder="Mô tả" value={menuForm.description} onChange={(e) => setMenuForm({ ...menuForm, description: e.target.value })} />
            <input placeholder="Link ảnh (tùy chọn)" value={menuForm.imageUrl} onChange={(e) => setMenuForm({ ...menuForm, imageUrl: e.target.value })} />
            <select value={menuForm.categoryId} onChange={(e) => setMenuForm({ ...menuForm, categoryId: e.target.value })}>
              <option value="">-- Danh mục --</option>
              {cats.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
            </select>
            <div style={{ display: 'flex', gap: 8 }}>
              <button type="submit">{editingItem ? 'Cập nhật' : 'Thêm món'}</button>
              {editingItem && <button type="button" onClick={() => { setEditingItem(null); setMenuForm({ name: '', price: '', description: '', categoryId: '', imageUrl: '' }) }}>Hủy</button>}
            </div>
          </form>
          <h4>Thực đơn ({menuPage?.totalElements ?? menu.length} món)</h4>
          {menu.map((m) => (
            <div key={m.id} className="card row">
              <div style={{ flex: 1 }}><b>{m.name}</b> — {Number(m.price).toLocaleString('vi-VN')}đ — {m.available ? 'Đang bán' : 'Ẩn'}</div>
              <button onClick={() => toggleAvail(m)}>{m.available ? 'Ẩn' : 'Hiện'}</button>
              <button onClick={() => { setEditingItem(m.id); setMenuForm({ name: m.name, price: m.price, description: m.description || '', categoryId: m.categoryId || '', imageUrl: m.imageUrl || '' }) }}>Sửa</button>
              <button onClick={() => removeDish(m)}>Xóa</button>
            </div>
          ))}
          <h4>Đơn hàng — lọc:</h4>
          <select value={filter} onChange={(e) => setFilter(e.target.value)}>
            <option value="">Tất cả</option>
            {[...FLOW, 'CANCELLED'].map((s) => <option key={s} value={s}>{s}</option>)}
          </select>
          {orders.map((o) => (
            <div key={o.id} className="card row">
              <span style={{ flex: 1 }}>#{o.id} — {o.status} — {Number(o.totalAmount).toLocaleString('vi-VN')}đ · {o.payMethod}/{o.payStatus}</span>
              {!['COMPLETED', 'CANCELLED'].includes(o.status) && <button onClick={() => advance(o)}>Duyệt tiếp</button>}
              {['PENDING', 'CONFIRMED'].includes(o.status) && <button onClick={() => cancel(o)}>Hủy</button>}
            </div>
          ))}
          {orders.length === 0 && <p>Không có đơn{filter ? ` ${filter}` : ''}.</p>}
        </div>
      )}
    </section>
  )
}
