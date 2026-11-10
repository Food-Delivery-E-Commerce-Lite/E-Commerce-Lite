import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useCart } from '../context/CartContext.jsx'
import { createAddress, listAddresses, paymentCallback, placeOrder } from '../services/order.js'
import { previewVoucher, usableVouchers } from '../services/voucher.js'
import { apiError } from '../services/api.js'

export default function Checkout() {
  const { restaurantId, lines, subtotal, clear } = useCart()
  const nav = useNavigate()
  const [addresses, setAddresses] = useState([])
  const [addressId, setAddressId] = useState('')
  const [method, setMethod] = useState('COD')
  const [street, setStreet] = useState('')
  const [err, setErr] = useState('')
  const [code, setCode] = useState('')
  const [vlist, setVlist] = useState([])
  const [preview, setPreview] = useState(null)
  const [placing, setPlacing] = useState(false)

  useEffect(() => { listAddresses().then(setAddresses).catch(() => setAddresses([])) }, [])
  useEffect(() => {
    if (!restaurantId) return
    usableVouchers(restaurantId).then(setVlist).catch(() => setVlist([]))
  }, [restaurantId])
  const addNew = async () => {
    if (!street.trim()) return
    try {
      const a = await createAddress({ street, isDefault: addresses.length === 0 })
      setAddresses((p) => [...p, a]); setAddressId(a.id); setStreet('')
    } catch (ex) { setErr(apiError(ex, 'Thêm địa chỉ thất bại')) }
  }
  const shipFee = subtotal >= 200000 ? 0 : 15000
  const applyCode = async (c) => {
    const v = (c ?? code).trim()
    if (!v) { setPreview(null); return }
    try {
      const p = await previewVoucher({ code: v, restaurantId, subtotal, deliveryFee: shipFee })
      setPreview(p); setErr(p.usable ? '' : (p.message || 'Mã không dùng được'))
    } catch (ex) { setPreview(null); setErr(apiError(ex, 'Mã không dùng được')) }
  }
  const discount = preview?.usable ? Number(preview.discount || 0) : 0
  const feeAfter = preview?.usable ? Number(preview.deliveryFeeAfter ?? shipFee) : shipFee
  const total = preview?.usable ? Number(preview.totalAfter) : subtotal + shipFee
  const submit = async () => {
    setErr(''); setPlacing(true)
    try {
      const order = await placeOrder({ restaurantId, addressId: Number(addressId), items: lines.map((l) => ({ itemId: l.itemId, quantity: l.quantity })), method, voucherCode: preview?.usable ? preview.code : (code.trim() || null) })
      if (method === 'ONLINE') {
        const ref = `MOCK-${order.id}-${Date.now()}`
        try { await paymentCallback({ orderId: order.id, paymentRef: ref, success: true }) } catch { /* vẫn qua trang chi tiết */ }
      }
      clear(); nav(`/orders/${order.id}`)
    } catch (ex) { setErr(apiError(ex, 'Đặt hàng thất bại')) }
    finally { setPlacing(false) }
  }
  if (lines.length === 0) return (<div className="card" style={{ marginTop: 24, textAlign: 'center', padding: 32 }}><h2>Giỏ hàng trống</h2><p style={{ color: '#6b7280' }}>Quay lại chọn món trước khi thanh toán nhé.</p></div>)
  return (
    <section>
      <div className="section-head"><div><h2>Thanh toán</h2><p>{lines.length} món · Giao nhanh trong 30 phút</p></div></div>
      {err && <p className="error">{err}</p>}
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 340px', gap: 16, alignItems: 'start' }} className="checkout-grid">
        <div style={{ display: 'grid', gap: 12 }}>
          <div className="card">
            <b>📍 Địa chỉ giao hàng</b>
            <select value={addressId} onChange={(e) => setAddressId(e.target.value)} style={{ marginTop: 8 }}>
              <option value="">-- Chọn địa chỉ --</option>
              {addresses.map((a) => <option key={a.id} value={a.id}>{a.street} {a.district || ''} {a.city || ''}</option>)}
            </select>
            <div style={{ display: 'flex', gap: 8, marginTop: 8 }}>
              <input placeholder="Thêm địa chỉ mới, vd 12 Nguyễn Huệ, Q1..." value={street} onChange={(e) => setStreet(e.target.value)} style={{ flex: 1 }} />
              <button className="ghost" onClick={addNew}>Thêm</button>
            </div>
          </div>
          <div className="card">
            <b>🎟 Mã giảm giá</b>
            <div style={{ display: 'flex', gap: 8, marginTop: 8 }}>
              <input placeholder="Nhập mã, vd SALE10..." value={code} onChange={(e) => setCode(e.target.value.toUpperCase())} style={{ flex: 1 }} />
              <button className="ghost" onClick={() => applyCode()}>Áp dụng</button>
              {preview && <button className="ghost" onClick={() => { setPreview(null); setCode(''); setErr('') }}>Xóa</button>}
            </div>
            {vlist.length > 0 && (
              <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', marginTop: 10 }}>
                {vlist.map((v) => (
                  <button key={v.id} className="ghost small" onClick={() => { setCode(v.code); applyCode(v.code) }} title={v.type} style={{ borderStyle: 'dashed' }}>
                    {v.code} · {v.type === 'PERCENT' ? `-${v.value}%` : v.type === 'FIXED' ? `-${Number(v.value).toLocaleString('vi-VN')}đ` : 'Freeship'}
                  </button>
                ))}
              </div>
            )}
            {preview?.usable && <p className="success" style={{ marginTop: 10 }}>Đã áp {preview.code}: giảm <b>{discount.toLocaleString('vi-VN')}đ</b> · Ship còn <b>{feeAfter === 0 ? 'MIỄN PHÍ' : `${feeAfter.toLocaleString('vi-VN')}đ`}</b></p>}
          </div>
          <div className="card">
            <b>💳 Phương thức thanh toán</b>
            <div style={{ display: 'flex', gap: 8, marginTop: 8 }}>
              {['COD', 'ONLINE'].map((m) => (
                <button key={m} className={method === m ? '' : 'ghost'} style={{ flex: 1 }} onClick={() => setMethod(m)}>
                  {m === 'COD' ? '💵 Tiền mặt' : '⚡ Online'}
                </button>
              ))}
            </div>
            <p style={{ fontSize: 13, color: '#6b7280', margin: '8px 0 0' }}>{method === 'COD' ? 'Thanh toán khi nhận hàng.' : 'Thanh toán online giả lập, tự callback ngay sau khi đặt.'}</p>
          </div>
        </div>
        <div className="card" style={{ position: 'sticky', top: 76 }}>
          <b>Tóm tắt đơn</b>
          <div style={{ display: 'grid', gap: 4, marginTop: 8, fontSize: 14 }}>
            <div style={{ display: 'flex', justifyContent: 'space-between' }}><span>Tạm tính</span><span>{subtotal.toLocaleString('vi-VN')}đ</span></div>
            <div style={{ display: 'flex', justifyContent: 'space-between' }}><span>Phí ship</span><span>{shipFee === 0 ? 'MIỄN PHÍ' : '15.000đ'}</span></div>
            {discount > 0 && <div style={{ display: 'flex', justifyContent: 'space-between', color: '#16a34a' }}><span>Giảm giá</span><span>−{discount.toLocaleString('vi-VN')}đ</span></div>}
            {feeAfter !== shipFee && <div style={{ display: 'flex', justifyContent: 'space-between', color: '#16a34a' }}><span>Ship sau giảm</span><span>{feeAfter === 0 ? 'MIỄN PHÍ' : `${feeAfter.toLocaleString('vi-VN')}đ`}</span></div>}
            <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 18, borderTop: '1px dashed #eee', paddingTop: 8, marginTop: 4 }}><b>Tổng trả</b><span className="price">{total.toLocaleString('vi-VN')}đ</span></div>
          </div>
          <button disabled={!addressId || placing} onClick={submit} style={{ width: '100%', marginTop: 12 }}>{placing ? 'Đang đặt...' : `Đặt hàng · ${total.toLocaleString('vi-VN')}đ`}</button>
          {!addressId && <p style={{ fontSize: 12, color: '#92400e' }}>Vui lòng chọn địa chỉ giao hàng.</p>}
        </div>
      </div>
      <style>{`@media (max-width: 860px){ .checkout-grid{ grid-template-columns: 1fr !important; } }`}</style>
    </section>
  )
}
