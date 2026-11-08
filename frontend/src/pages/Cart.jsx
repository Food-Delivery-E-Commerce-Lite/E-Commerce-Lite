import { Link } from 'react-router-dom'
import { useCart } from '../context/CartContext.jsx'

export default function Cart() {
  const { lines, updateQty, removeItem, subtotal, clear } = useCart()
  if (lines.length === 0) return (<section><h2>Giỏ hàng trống</h2><Link to="/">Về trang chủ</Link></section>)
  return (
    <section>
      <h2>Giỏ hàng</h2>
      {lines.map((l) => (
        <div key={l.itemId} className="card row">
          <div style={{ flex: 1 }}><b>{l.name}</b><div>{Number(l.price).toLocaleString('vi-VN')}đ</div></div>
          <input type="number" min="1" max="50" value={l.quantity} onChange={(e) => updateQty(l.itemId, Number(e.target.value))} style={{ width: 64 }} />
          <button onClick={() => removeItem(l.itemId)}>Xóa</button>
        </div>
      ))}
      <h3>Tạm tính: {subtotal.toLocaleString('vi-VN')}đ</h3>
      <div style={{ display: 'flex', gap: 8 }}>
        <button onClick={clear}>Xóa giỏ</button>
        <Link to="/checkout"><button>Thanh toán</button></Link>
      </div>
    </section>
  )
}
