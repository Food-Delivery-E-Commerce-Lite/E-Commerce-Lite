import { Link, NavLink, Route, Routes, useNavigate } from 'react-router-dom'
import Home from './pages/Home.jsx'
import Login from './pages/Login.jsx'
import Register from './pages/Register.jsx'
import RestaurantDetail from './pages/RestaurantDetail.jsx'
import Cart from './pages/Cart.jsx'
import Checkout from './pages/Checkout.jsx'
import Orders from './pages/Orders.jsx'
import OrderDetail from './pages/OrderDetail.jsx'
import Merchant from './pages/Merchant.jsx'
import Admin from './pages/Admin.jsx'
import ChangePassword from './pages/ChangePassword.jsx'
import ProtectedRoute from './routes/ProtectedRoute.jsx'
import { useAuth } from './context/AuthContext.jsx'
import { useCart } from './context/CartContext.jsx'

function Layout() {
  const { user, logout } = useAuth()
  const { count } = useCart()
  const nav = useNavigate()
  const onLogout = () => { logout(); nav('/') }
  const linkCls = ({ isActive }) => (isActive ? 'active' : '')
  return (
    <>
      <nav>
        <Link to="/" className="brand">🍔 Food Delivery</Link>
        <NavLink to="/" end className={linkCls}>Khám phá</NavLink>
        <NavLink to="/cart" className={linkCls}>Giỏ hàng ({count})</NavLink>
        <NavLink to="/orders" className={linkCls}>Đơn hàng</NavLink>
        {user?.role === 'MERCHANT' && <NavLink to="/merchant" className={linkCls}>Kênh bán</NavLink>}
        {user?.role === 'ADMIN' && <NavLink to="/admin" className={linkCls}>Quản trị</NavLink>}
        <span style={{ marginLeft: 'auto', display: 'flex', alignItems: 'center', gap: 8 }} >
          {user ? (<><span style={{ fontSize: 13, color: '#4b5563', fontWeight: 600 }}>Xin chào, {user.fullName || user.email}</span><button className="ghost small" onClick={onLogout}>Đăng xuất</button></>) : (<><Link to="/login">Đăng nhập</Link><Link to="/register">Đăng ký</Link></>)}
        </span>
      </nav>
      <main>
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />
          <Route path="/restaurants/:id" element={<RestaurantDetail />} />
          <Route path="/cart" element={<Cart />} />
          <Route path="/checkout" element={<ProtectedRoute><Checkout /></ProtectedRoute>} />
          <Route path="/orders" element={<ProtectedRoute><Orders /></ProtectedRoute>} />
          <Route path="/orders/:id" element={<ProtectedRoute><OrderDetail /></ProtectedRoute>} />
          <Route path="/merchant" element={<ProtectedRoute roles={['MERCHANT', 'ADMIN']}><Merchant /></ProtectedRoute>} />
          <Route path="/admin" element={<ProtectedRoute roles={['ADMIN']}><Admin /></ProtectedRoute>} />
          <Route path="/change-password" element={<ProtectedRoute><ChangePassword /></ProtectedRoute>} />
        </Routes>
        <div className="footer">Food Delivery Lite · Giao ngon mỗi ngày · Miễn phí ship đơn từ 200k</div>
      </main>
    </>
  )
}

export default function App() { return <Layout /> }
