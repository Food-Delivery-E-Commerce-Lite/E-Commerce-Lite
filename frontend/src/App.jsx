import { Link, Route, Routes, useNavigate } from 'react-router-dom'
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
import ProtectedRoute from './routes/ProtectedRoute.jsx'
import { useAuth } from './context/AuthContext.jsx'
import { useCart } from './context/CartContext.jsx'

function Layout() {
  const { user, logout } = useAuth()
  const { count } = useCart()
  const nav = useNavigate()
  const onLogout = () => { logout(); nav('/') }
  return (
    <>
      <nav>
        <Link to="/">Food Delivery</Link>
        <Link to="/cart">Giỏ hàng ({count})</Link>
        <Link to="/orders">Đơn hàng</Link>
        {user?.role === 'MERCHANT' && <Link to="/merchant">Kênh bán</Link>}
        {user?.role === 'ADMIN' && <Link to="/admin">Quản trị</Link>}
        <span style={{ marginLeft: 'auto' }} />
        {user ? (<><span>Xin chào, {user.fullName || user.email}</span><button onClick={onLogout}>Đăng xuất</button></>) : (<><Link to="/login">Đăng nhập</Link><Link to="/register">Đăng ký</Link></>)}
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
        </Routes>
      </main>
    </>
  )
}

export default function App() { return <Layout /> }
