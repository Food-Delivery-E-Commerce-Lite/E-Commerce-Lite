import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useCart } from '../context/CartContext.jsx'
import { createAddress, listAddresses, placeOrder } from '../services/order.js'
import { apiError } from '../services/api.js'

export default function Checkout() {
  const { restaurantId, lines, subtotal, clear } = useCart()
  const nav = useNavigate()
  const [addresses, setAddresses] = useState([])
  const [addressId, setAddressId] = useState('')
  const [method, setMethod] = useState('COD')
  const [street, setStreet] = useState('')
  const [err, setErr] = useState('')

  useEffect(() => { listAddresses().then(setAddresses).catch(() => setAddresses([])) }, [])
  const addNew = async () => {
    if (!street.trim()) return
    const a = await createAddress({ street, isDefault: addresses.length === 0 })
    setAddresses((p) => [...p, a]); setAddressId(a.id); setStreet('')
  }
  const submit = async () => {
    setErr('')
    try {
      const order = await placeOrder({ restaurantId, addressId: Number(addressId), items: lines.map((l) => ({ itemId: l.itemId, quantity: l.quantity })), method })
      clear(); nav(`/orders/${order.id}`)
    } catch (ex) { setErr(apiError(ex, 'Đặt hàng thất bại')) }
  }
  if (lines.length === 0) return <p>Giỏ hàng trống.</p>
  return (
    <section>
      <h2>Thanh toán</h2>
      <p>Tạm tính: {subtotal.toLocaleString('vi-VN')}đ + phí ship 15.000đ</p>
      <h3>Địa chỉ giao hàng</h3>
      <select value={addressId} onChange={(e) => setAddressId(e.target.value)}>
        <option value="">-- Chọn địa chỉ --</option>
        {addresses.map((a) => <option key={a.id} value={a.id}>{a.street} {a.district || ''} {a.city || ''}</option>)}
      </select>
      <div style={{ display: 'flex', gap: 8, marginTop: 8 }}>
        <input placeholder="Địa chỉ mới..." value={street} onChange={(e) => setStreet(e.target.value)} style={{ flex: 1 }} />
        <button onClick={addNew}>Thêm</button>
      </div>
      <h3>Thanh toán</h3>
      <select value={method} onChange={(e) => setMethod(e.target.value)}>
        <option value="COD">Tiền mặt (COD)</option>
        <option value="ONLINE">Online (giả lập)</option>
      </select>
      {err && <p className="error">{err}</p>}
      <div style={{ marginTop: 12 }}><button disabled={!addressId} onClick={submit}>Đặt hàng</button></div>
    </section>
  )
}
