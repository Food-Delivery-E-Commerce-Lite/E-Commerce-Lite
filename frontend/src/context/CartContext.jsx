import { createContext, useContext, useEffect, useMemo, useState } from 'react'

// Giỏ hàng chỉ cho 1 nhà hàng (đúng rule backend MIXED_RESTAURANT)
const CartContext = createContext(null)
const KEY = 'cart'

export function CartProvider({ children }) {
  const [restaurantId, setRestaurantId] = useState(null)
  const [lines, setLines] = useState(() => {
    try { return JSON.parse(localStorage.getItem(KEY))?.lines || [] } catch { return [] }
  })

  useEffect(() => {
    try {
      const saved = JSON.parse(localStorage.getItem(KEY))
      if (saved) { setLines(saved.lines || []); setRestaurantId(saved.restaurantId || null) }
    } catch { /* ignore */ }
  }, [])

  useEffect(() => {
    localStorage.setItem(KEY, JSON.stringify({ restaurantId, lines }))
  }, [restaurantId, lines])

  const addItem = (restaurant, item, qty = 1) => {
    if (restaurantId && restaurantId !== restaurant) {
      if (!confirm('Giỏ hàng chỉ chứa món của 1 nhà hàng. Xóa giỏ cũ và thêm món mới?')) return
      setLines([{ itemId: item.id, name: item.name, price: item.price, quantity: qty }])
      setRestaurantId(restaurant)
      return
    }
    setRestaurantId(restaurant)
    setLines((prev) => {
      const found = prev.find((l) => l.itemId === item.id)
      if (found) return prev.map((l) => l.itemId === item.id ? { ...l, quantity: Math.min(l.quantity + qty, 50) } : l)
      return [...prev, { itemId: item.id, name: item.name, price: item.price, quantity: qty }]
    })
  }

  const updateQty = (itemId, qty) => {
    if (qty <= 0) return removeItem(itemId)
    setLines((prev) => prev.map((l) => l.itemId === itemId ? { ...l, quantity: Math.min(qty, 50) } : l))
  }
  const removeItem = (itemId) => setLines((prev) => prev.filter((l) => l.itemId !== itemId))
  const clear = () => { setLines([]); setRestaurantId(null); localStorage.removeItem(KEY) }
  const subtotal = lines.reduce((s, l) => s + l.price * l.quantity, 0)
  const count = lines.reduce((s, l) => s + l.quantity, 0)

  const value = useMemo(() => ({ restaurantId, lines, addItem, updateQty, removeItem, clear, subtotal, count }), [restaurantId, lines])
  return <CartContext.Provider value={value}>{children}</CartContext.Provider>
}

export const useCart = () => useContext(CartContext)
