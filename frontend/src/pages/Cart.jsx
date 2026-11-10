import { Link } from 'react-router-dom'
import { useCart } from '../context/CartContext.jsx'

export default function Cart() {
  const { lines, updateQty, removeItem, subtotal, clear } = useCart()
  if (lines.length === 0) return (
    <section>
      <div className="card" style={{ textAlign: 'center', padding: 40, marginTop: 24 }}>
        <div style={{ fontSize: 56 }}>🛒</div>
        <h2>Giỏ hàng trống</h2>
        <p style={{ color: '#6b7280' }}>Bạn chưa chọn món nào. Khám phá quán ngon ngay nhé!</p>
        <Link to="/"><button>Khám phá quán ngon</button></Link>
      </div>
    </section>
  )
  const ship = subtotal >= 200000 ? 0 : 15000
  return (
    <section>
      <div className="section-head"><div><h2>Giỏ hàng</h2><p>{lines.length} món · Tạm tính {subtotal.toLocaleString('vi-VN')}đ</p></div>
        <button className="ghost small" onClick={clear}>Xóa tất cả</button>
      </div>
      {lines.map((l) => (
        <div key={l.itemId} className="card row">
          <div style={{ flex: 1 }}><b>{l.name}</b><div className="price" style={{ fontSize: 14 }}>{Number(l.price).toLocaleString('vi-VN')}đ</div></div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <button className="ghost small" onClick={() => updateQty(l.itemId, Math.max(1, l.quantity - 1))}>−</button>
            <input type="number" min="1" max="50" value={l.quantity} onChange={(e) => updateQty(l.itemId, Number(e.target.value))} style={{ width: 60, textAlign: 'center' }} />
            <button className="ghost small" onClick={() => updateQty(l.itemId, Math.min(50, l.quantity + 1))}>+</button>
          </div>
          <b style={{ minWidth: 90, textAlign: 'right' }}>{Number(l.price * l.quantity).toLocaleString('vi-VN')}đ</b>
          <button className="ghost small" onClick={() => removeItem(l.itemId)}>Xóa</button>
        </div>
      ))}
      <div className="card" style={{ marginTop: 12 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 14 }}><span>Tạm tính</span><b>{subtotal.toLocaleString('vi-VN')}đ</b></div>
        <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 14, marginTop: 4 }}><span>Phí ship</span><b>{ship === 0 ? 'MIỄN PHÍ 🎉' : '15.000đ'}</b></div>
        {ship > 0 && <p style={{ fontSize: 13, color: '#6b7280' }}>Thêm {(200000 - subtotal).toLocaleString('vi-VN')}đ nữa để được miễn ship.</p>}
        <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 17, marginTop: 8 }}><span><b>Tổng</b></span><span className="price">{(subtotal + ship).toLocaleString('vi-VN')}đ</span></div>
        <div style={{ display: 'flex', gap: 8, marginTop: 12 }}>
          <Link to="/" style={{ flex: 1 }}><button className="ghost" style={{ width: '100%' }}>Chọn thêm món</button></Link>
          <Link to="/checkout" style={{ flex: 1 }}><button style={{ width: '100%' }}>Thanh toán →</button></Link>
        </div>
      </div>
    </section>
  )
}
