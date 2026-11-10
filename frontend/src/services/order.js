import api, { unwrap } from './api.js'

export async function placeOrder({ restaurantId, addressId, items, method = 'COD', voucherCode = null }) {
  const res = await api.post('/orders', { restaurantId, addressId, items, method, voucherCode: voucherCode || undefined })
  return unwrap(res).data
}
export async function myOrders(page = 0, size = 20) {
  const res = await api.get('/orders', { params: { page, size } })
  return unwrap(res)
}
export async function getOrder(id) {
  const res = await api.get(`/orders/${id}`)
  return unwrap(res).data
}
export async function cancelOrder(id) {
  const res = await api.post(`/orders/${id}/cancel`)
  return unwrap(res).data
}
export async function paymentCallback({ orderId, paymentRef, success }) {
  const res = await api.post('/payments/callback', { orderId, paymentRef, success })
  return unwrap(res).data
}
export async function refundPayment(orderId) {
  const res = await api.post(`/payments/orders/${orderId}/refund`)
  return unwrap(res).data
}
export async function createReview({ orderId, rating, comment }) {
  const res = await api.post('/reviews', { orderId, rating, comment })
  return unwrap(res).data
}
export async function listAddresses() {
  const res = await api.get('/addresses')
  return unwrap(res).data
}
export async function createAddress(payload) {
  const res = await api.post('/addresses', payload)
  return unwrap(res).data
}
