import { useEffect, useState } from 'react'
import { createMenuItem, createRestaurant, deleteMenuItem, getOwnedMenu, merchantChangeStatus, merchantOrders, merchantStats, myRestaurants, updateAvailability, updateMenuItem, updateRestaurant } from '../services/merchant.js'
import { subscribeMerchant } from '../services/realtime.js'
import { getOrder, refundPayment } from '../services/order.js'
import { createMerchantVoucher, merchantVouchers, updateMerchantVoucher } from '../services/voucher.js'
import { maxOf, merchantReport, merchantReportCsvUrl } from '../services/report.js'
import api from '../services/api.js'
import { getCategories } from '../services/restaurant.js'
import { uploadImage, resolveImage, withFallback } from '../services/upload.js'
import { apiError } from '../services/api.js'

const FLOW = ['PENDING', 'CONFIRMED', 'PREPARING', 'DELIVERING', 'COMPLETED']

export default function Merchant() {
  const [shops, setShops] = useState([])
  const [selected, setSelected] = useState(null)
  const [orders, setOrders] = useState([])
  const [ordersPage, setOrdersPage] = useState(null)
  const [filter, setFilter] = useState('')
  const [cats, setCats] = useState([])
  const [form, setForm] = useState({ name: '', address: '', description: '', openTime: '', closeTime: '', imageUrl: '' })
  const [editForm, setEditForm] = useState(null)
  const [menu, setMenu] = useState([])
  const [menuPage, setMenuPage] = useState(null)
  const [menuForm, setMenuForm] = useState({ name: '', price: '', description: '', categoryId: '', imageUrl: '' })
  const [editingItem, setEditingItem] = useState(null)
  const [stats, setStats] = useState(null)
  const [msg, setMsg] = useState('')
  const [uploading, setUploading] = useState(false)
  const [vouchers, setVouchers] = useState([])
  const [vform, setVform] = useState({ code: '', type: 'FIXED', value: '', minOrder: '', maxDiscount: '', usageLimit: '' })
  const [repFrom, setRepFrom] = useState('')
  const [repTo, setRepTo] = useState('')
  const [report, setReport] = useState(null)

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
    merchantVouchers(selected.id, 0, 50).then(({ data }) => setVouchers(data ?? [])).catch(() => setVouchers([]))
    setEditForm({ name: selected.name, address: selected.address, description: selected.description || '', openTime: selected.openTime || '', closeTime: selected.closeTime || '', imageUrl: selected.imageUrl || '' })
    let unsub = null
    try {
      unsub = subscribeMerchant(selected.id, async (ev) => {
        if (!ev?.orderId) return
        try {
          const full = await getOrder(ev.orderId).catch(() => null)
          if (full && full.restaurantId !== selected.id) return
          if (full) {
            setOrders((p) => {
              const i = p.findIndex((x) => x.id === full.id)
              if (i >= 0) { const c = [...p]; c[i] = full; return c }
              return [full, ...p]
            })
            setMsg(`Đơn #${full.id} có cập nhật: ${full.status} (${full.payStatus})`)
          } else if (ev.status) {
            setOrders((p) => p.map((x) => x.id === ev.orderId ? { ...x, status: ev.status, payStatus: ev.payStatus ?? x.payStatus } : x))
          }
        } catch { /* bỏ qua event lỗi */ }
        merchantStats(selected.id).then(setStats).catch(() => {})
      })
    } catch { /* WS lỗi thì vẫn dùng nút refresh thủ công */ }
    return () => unsub?.()
  }, [selected])

  const onPickFile = async (file, apply) => {
    if (!file) return
    setUploading(true); setMsg('')
    try {
      const stored = await uploadImage(file)
      apply(stored.thumbUrl || stored.url)
      setMsg('Đã tải ảnh lên!')
    } catch (ex) { setMsg(apiError(ex, 'Upload ảnh thất bại')) }
    finally { setUploading(false) }
  }

  const create = async (e) => {
    e.preventDefault(); setMsg('')
    try { await createRestaurant({ ...form, imageUrl: form.imageUrl || null }); setForm({ name: '', address: '', description: '', openTime: '', closeTime: '', imageUrl: '' }); await reload() }
    catch (ex) { setMsg(apiError(ex)) }
  }
  const saveShop = async (e) => {
    e.preventDefault(); setMsg('')
    try {
      const updated = await updateRestaurant(selected.id, { ...editForm, imageUrl: editForm.imageUrl || null })
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
  const refund = async (o) => {
    try { await refundPayment(o.id); setOrders((p) => p.map((x) => x.id === o.id ? { ...x, payStatus: 'REFUNDED' } : x)); setMsg(`Đã hoàn tiền đơn #${o.id}`) }
    catch (ex) { setMsg(apiError(ex, 'Hoàn tiền thất bại')) }
  }
  const createVoucher = async (e) => {
    e.preventDefault(); setMsg('')
    try {
      await createMerchantVoucher({ code: vform.code, type: vform.type, value: vform.value ? Number(vform.value) : 0, minOrder: vform.minOrder ? Number(vform.minOrder) : 0, maxDiscount: vform.maxDiscount ? Number(vform.maxDiscount) : null, usageLimit: vform.usageLimit ? Number(vform.usageLimit) : null, restaurantId: selected.id })
      setVform({ code: '', type: 'FIXED', value: '', minOrder: '', maxDiscount: '', usageLimit: '' })
      const { data } = await merchantVouchers(selected.id, 0, 50).catch(() => ({ data: [] }))
      setVouchers(data ?? []); setMsg('Đã tạo voucher!')
    } catch (ex) { setMsg(apiError(ex)) }
  }
  const toggleVoucher = async (v) => {
    try { await updateMerchantVoucher(v.id, { active: !v.active }); setVouchers((p) => p.map((x) => x.id === v.id ? { ...x, active: !v.active } : x)) }
    catch (ex) { setMsg(apiError(ex)) }
  }
  const loadReport = async () => {
    if (!selected) return
    setMsg('')
    try {
      const r = await merchantReport(selected.id, repFrom || null, repTo || null)
      setReport(r)
    } catch (ex) { setMsg(apiError(ex, 'Tải báo cáo thất bại')) }
  }
  const downloadCsv = async () => {
    if (!selected) return
    try {
      const res = await api.get(merchantReportCsvUrl(selected.id, repFrom || null, repTo || null), { responseType: 'blob' })
      const url = URL.createObjectURL(new Blob([res.data], { type: 'text/csv;charset=utf-8' }))
      const a = document.createElement('a')
      a.href = url; a.download = `bao-cao-quan-${selected.id}.csv`; a.click()
      URL.revokeObjectURL(url)
    } catch (ex) { setMsg(apiError(ex, 'Xuất CSV thất bại')) }
  }

  return (
    <section>
      <div className="section-head"><div><h2>Kênh người bán</h2><p>Quản lý quán, món, đơn và voucher</p></div></div>
      {msg && <p className={String(msg).includes('thất bại') || String(msg).includes('lỗi') ? 'error' : 'success'}>{msg}</p>}
      <div className="card"><h3>Tạo quán mới (duyệt PENDING)</h3>
        <form onSubmit={create} style={{ display: 'grid', gap: 8 }}>
          <input placeholder="Tên quán" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} required />
          <input placeholder="Địa chỉ" value={form.address} onChange={(e) => setForm({ ...form, address: e.target.value })} required />
          <input placeholder="Mô tả" value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} />
          <div style={{ display: 'flex', gap: 8 }}>
            <label>Mở cửa <input type="time" value={form.openTime} onChange={(e) => setForm({ ...form, openTime: e.target.value })} /></label>
            <label>Đóng cửa <input type="time" value={form.closeTime} onChange={(e) => setForm({ ...form, closeTime: e.target.value })} /></label>
          </div>
          <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
            <input type="file" accept="image/*" onChange={(e) => onPickFile(e.target.files[0], (url) => setForm({ ...form, imageUrl: url }))} />
            {form.imageUrl && <img src={resolveImage(form.imageUrl)} onError={withFallback} alt="cover" style={{ width: 80, height: 60, objectFit: 'cover', borderRadius: 8 }} />}
          </div>
          <input placeholder="Hoặc dán link ảnh https://..." value={form.imageUrl} onChange={(e) => setForm({ ...form, imageUrl: e.target.value })} />
          <button type="submit" disabled={uploading}>Tạo quán</button>
        </form>
      </div>
      <h3>Quán của tôi</h3>
      {shops.map((s) => (
        <div key={s.id} className="card row">
          {s.imageUrl && <img src={resolveImage(s.imageUrl)} onError={withFallback} alt="" style={{ width: 56, height: 42, objectFit: 'cover', borderRadius: 8 }} />}
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
            <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
              <input type="file" accept="image/*" onChange={(e) => onPickFile(e.target.files[0], (url) => setEditForm({ ...editForm, imageUrl: url }))} />
              {editForm?.imageUrl && <img src={resolveImage(editForm.imageUrl)} onError={withFallback} alt="cover" style={{ width: 80, height: 60, objectFit: 'cover', borderRadius: 8 }} />}
            </div>
            <input value={editForm?.imageUrl || ''} onChange={(e) => setEditForm({ ...editForm, imageUrl: e.target.value })} placeholder="Link ảnh quán" />
            <button type="submit" disabled={uploading}>Lưu quán</button>
          </form>
          <form onSubmit={addDish} style={{ display: 'grid', gap: 8, marginTop: 12 }}>
            <b>{editingItem ? 'Sửa món' : 'Thêm món'}</b>
            <input placeholder="Tên món" value={menuForm.name} onChange={(e) => setMenuForm({ ...menuForm, name: e.target.value })} required />
            <input placeholder="Giá" type="number" min="1" value={menuForm.price} onChange={(e) => setMenuForm({ ...menuForm, price: e.target.value })} required />
            <input placeholder="Mô tả" value={menuForm.description} onChange={(e) => setMenuForm({ ...menuForm, description: e.target.value })} />
            <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
              <input type="file" accept="image/*" onChange={(e) => onPickFile(e.target.files[0], (url) => setMenuForm({ ...menuForm, imageUrl: url }))} />
              {menuForm.imageUrl && <img src={resolveImage(menuForm.imageUrl)} onError={withFallback} alt="" style={{ width: 80, height: 60, objectFit: 'cover', borderRadius: 8 }} />}
            </div>
            <input placeholder="Hoặc dán link ảnh https://..." value={menuForm.imageUrl} onChange={(e) => setMenuForm({ ...menuForm, imageUrl: e.target.value })} />
            <select value={menuForm.categoryId} onChange={(e) => setMenuForm({ ...menuForm, categoryId: e.target.value })}>
              <option value="">-- Danh mục --</option>
              {cats.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
            </select>
            <div style={{ display: 'flex', gap: 8 }}>
              <button type="submit" disabled={uploading}>{editingItem ? 'Cập nhật' : 'Thêm món'}</button>
              {editingItem && <button type="button" onClick={() => { setEditingItem(null); setMenuForm({ name: '', price: '', description: '', categoryId: '', imageUrl: '' }) }}>Hủy</button>}
            </div>
          </form>
          <h4>Thực đơn ({menuPage?.totalElements ?? menu.length} món)</h4>
          {menu.map((m) => (
            <div key={m.id} className="card row">
              {m.imageUrl && <img src={resolveImage(m.imageUrl)} onError={withFallback} alt="" style={{ width: 56, height: 42, objectFit: 'cover', borderRadius: 8 }} />}
              <div style={{ flex: 1 }}><b>{m.name}</b> — {Number(m.price).toLocaleString('vi-VN')}đ — {m.available ? 'Đang bán' : 'Ẩn'}</div>
              <button onClick={() => toggleAvail(m)}>{m.available ? 'Ẩn' : 'Hiện'}</button>
              <button onClick={() => { setEditingItem(m.id); setMenuForm({ name: m.name, price: m.price, description: m.description || '', categoryId: m.categoryId || '', imageUrl: m.imageUrl || '' }) }}>Sửa</button>
              <button onClick={() => removeDish(m)}>Xóa</button>
            </div>
          ))}
          <h4>Voucher của quán ({vouchers.length})</h4>
          <form onSubmit={createVoucher} style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
            <input placeholder="Mã, vd GIAM20K" value={vform.code} onChange={(e) => setVform({ ...vform, code: e.target.value.toUpperCase() })} required style={{ width: 130 }} />
            <select value={vform.type} onChange={(e) => setVform({ ...vform, type: e.target.value })}>
              <option value="FIXED">Giảm tiền</option>
              <option value="PERCENT">Giảm %</option>
              <option value="FREESHIP">Freeship</option>
            </select>
            {vform.type !== 'FREESHIP' && <input placeholder={vform.type === 'PERCENT' ? '% (1-100)' : 'Số tiền giảm'} type="number" min="0" value={vform.value} onChange={(e) => setVform({ ...vform, value: e.target.value })} style={{ width: 110 }} />}
            <input placeholder="Đơn tối thiểu" type="number" min="0" value={vform.minOrder} onChange={(e) => setVform({ ...vform, minOrder: e.target.value })} style={{ width: 110 }} />
            {vform.type === 'PERCENT' && <input placeholder="Giảm tối đa" type="number" min="0" value={vform.maxDiscount} onChange={(e) => setVform({ ...vform, maxDiscount: e.target.value })} style={{ width: 110 }} />}
            <input placeholder="Lượt dùng" type="number" min="1" value={vform.usageLimit} onChange={(e) => setVform({ ...vform, usageLimit: e.target.value })} style={{ width: 90 }} />
            <button type="submit">Tạo voucher</button>
          </form>
          {vouchers.map((v) => (
            <div key={v.id} className="card row">
              <span style={{ flex: 1 }}><b>{v.code}</b> — {v.type}{v.type === 'PERCENT' ? ` ${v.value}%` : v.type === 'FIXED' ? ` -${Number(v.value).toLocaleString('vi-VN')}đ` : ''} · tối thiểu {Number(v.minOrder).toLocaleString('vi-VN')}đ · đã dùng {v.usedCount}{v.usageLimit ? `/${v.usageLimit}` : ''} · {v.active ? 'Đang bật' : 'Đã tắt'}</span>
              <button onClick={() => toggleVoucher(v)}>{v.active ? 'Tắt' : 'Bật'}</button>
            </div>
          ))}
          <h4>Đơn hàng — lọc:</h4>
          <select value={filter} onChange={(e) => setFilter(e.target.value)}>
            <option value="">Tất cả</option>
            {[...FLOW, 'CANCELLED'].map((s) => <option key={s} value={s}>{s}</option>)}
          </select>
          {orders.map((o) => (
            <div key={o.id} className="card row">
              <span style={{ flex: 1 }}>#{o.id} — {o.status} — {Number(o.totalAmount).toLocaleString('vi-VN')}đ · {o.payMethod}/{o.payStatus}{Number(o.discountAmount ?? 0) > 0 ? ` · giảm ${Number(o.discountAmount).toLocaleString('vi-VN')}đ (${o.voucherCode})` : ''}</span>
              {!['COMPLETED', 'CANCELLED'].includes(o.status) && <button onClick={() => advance(o)}>Duyệt tiếp</button>}
              {['PENDING', 'CONFIRMED'].includes(o.status) && <button onClick={() => cancel(o)}>Hủy</button>}
              {o.status === 'CANCELLED' && o.payMethod === 'ONLINE' && o.payStatus === 'PAID' && <button onClick={() => refund(o)}>Hoàn tiền</button>}
              {o.status === 'CANCELLED' && o.payMethod === 'ONLINE' && o.payStatus === 'REFUNDED' && <small>Đã hoàn tiền</small>}
            </div>
          ))}
          {orders.length === 0 && <p>Không có đơn{filter ? ` ${filter}` : ''}.</p>}
          <h4>Báo cáo nâng cao</h4>
          <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', alignItems: 'center' }}>
            <label>Từ <input type="date" value={repFrom} onChange={(e) => setRepFrom(e.target.value)} /></label>
            <label>Đến <input type="date" value={repTo} onChange={(e) => setRepTo(e.target.value)} /></label>
            <button onClick={loadReport}>Xem báo cáo</button>
            <button onClick={downloadCsv}>Xuất CSV</button>
          </div>
          {report && (
            <div className="card" style={{ marginTop: 8 }}>
              <p>Tổng đơn: <b>{report.totalOrders}</b> · Hoàn thành: <b>{report.completedOrders}</b> · Hủy: <b>{report.cancelledOrders}</b> ({Number(report.cancelRate).toFixed(1)}%)</p>
              <p>Doanh thu: <b>{Number(report.revenue).toLocaleString('vi-VN')}đ</b> · Giảm giá: <b>{Number(report.discount).toLocaleString('vi-VN')}đ</b> · TB/đơn: <b>{Number(report.avgOrderValue).toLocaleString('vi-VN')}đ</b></p>
              <b>Doanh thu theo ngày</b>
              <div style={{ display: 'flex', gap: 4, alignItems: 'flex-end', margin: '8px 0', minHeight: 70 }}>
                {report.daily.map((d) => {
                  const m = maxOf(report.daily, 'revenue') || 1
                  const h = Math.max(4, Math.round((Number(d.revenue) / m) * 64))
                  return (
                    <div key={d.date} title={`${d.date}: ${d.orders} đơn, ${Number(d.revenue).toLocaleString('vi-VN')}đ`} style={{ flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 2 }}>
                      <div style={{ width: '100%', height: h, background: '#4caf50', borderRadius: 4 }} />
                      <small>{String(d.date).slice(5)}</small>
                    </div>
                  )
                })}
              </div>
              <b>Theo trạng thái</b>
              {report.byStatus.map((s) => (
                <div key={s.status}><small>{s.status}: {s.orders} đơn · {Number(s.revenue).toLocaleString('vi-VN')}đ</small></div>
              ))}
              <b style={{ marginTop: 8, display: 'block' }}>Món bán chạy</b>
              {report.topDishes.map((t) => (
                <div key={t.itemId}><small>{t.itemName}: {t.quantity} phần · {Number(t.revenue).toLocaleString('vi-VN')}đ</small></div>
              ))}
              {report.topDishes.length === 0 && <p><small>Chưa có dữ liệu món.</small></p>}
            </div>
          )}
        </div>
      )}
    </section>
  )
}
