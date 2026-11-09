import api, { unwrap } from './api.js'

export async function myRestaurants(page = 0, size = 20) {
  const res = await api.get('/merchant/restaurants', { params: { page, size } })
  return unwrap(res)
}
export async function createRestaurant(payload) {
  const res = await api.post('/merchant/restaurants', payload)
  return unwrap(res).data
}
export async function updateRestaurant(id, payload) {
  const res = await api.put(`/merchant/restaurants/${id}`, payload)
  return unwrap(res).data
}
export async function createMenuItem(restaurantId, payload) {
  const res = await api.post(`/merchant/restaurants/${restaurantId}/menu-items`, payload)
  return unwrap(res).data
}
export async function getOwnedMenu(restaurantId, page = 0, size = 50) {
  const res = await api.get(`/merchant/restaurants/${restaurantId}/menu-items`, { params: { page, size } })
  return unwrap(res)
}
export async function updateMenuItem(itemId, payload) {
  const res = await api.put(`/merchant/menu-items/${itemId}`, payload)
  return unwrap(res).data
}
export async function updateAvailability(itemId, available) {
  const res = await api.patch(`/merchant/menu-items/${itemId}/availability`, { available })
  return unwrap(res).data
}
export async function deleteMenuItem(itemId) {
  await api.delete(`/merchant/menu-items/${itemId}`)
}
export async function merchantOrders(restaurantId, status = null, page = 0, size = 20) {
  const params = { page, size }
  if (status) params.status = status
  const res = await api.get(`/merchant/restaurants/${restaurantId}/orders`, { params })
  return unwrap(res)
}
export async function merchantChangeStatus(orderId, status) {
  const res = await api.patch(`/merchant/orders/${orderId}/status`, { status })
  return unwrap(res).data
}
export async function merchantStats(restaurantId) {
  const res = await api.get(`/merchant/restaurants/${restaurantId}/stats`)
  return unwrap(res).data
}
