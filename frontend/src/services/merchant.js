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
export async function updateAvailability(itemId, available) {
  const res = await api.patch(`/merchant/menu-items/${itemId}/availability`, { available })
  return unwrap(res).data
}
export async function merchantOrders(restaurantId, status = 'PENDING') {
  const res = await api.get(`/merchant/restaurants/${restaurantId}/orders`, { params: { status } })
  return unwrap(res).data
}
export async function merchantChangeStatus(orderId, status) {
  const res = await api.patch(`/merchant/orders/${orderId}/status`, { status })
  return unwrap(res).data
}
