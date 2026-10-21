import { Link, Route, Routes } from 'react-router-dom'
import Home from './pages/Home.jsx'

export default function App() {
  return (
    <>
      <nav>
        <Link to="/">Food Delivery</Link>
        {/* TODO: giỏ hàng, đăng nhập, đơn hàng */}
      </nav>
      <main>
        <Routes>
          <Route path="/" element={<Home />} />
          {/* TODO: /restaurants/:id, /cart, /checkout, /orders, /merchant, /admin */}
        </Routes>
      </main>
    </>
  )
}
