import api, { unwrap } from './api.js'

export async function usableVouchers(restaurantId) {
  const res = await api.get('/vouchers/usable', { params: { restaurantId } })
  return unwrap(res).data ?? []
}
export async function previewVoucher({ code, restaurantId, subtotal, deliveryFee }) {
  const res = await api.post('/vouchers/preview', { code, restaurantId, subtotal, deliveryFee })
  return unwrap(res).data
}
export async function merchantVouchers(restaurantId, page = 0, size = 20) {
  const res = await api.get('/merchant/vouchers', { params: { restaurantId, page, size } })
  return unwrap(res)
}
export async function createMerchantVoucher(payload) {
  const res = await api.post('/merchant/vouchers', payload)
  return unwrap(res).data
}
export async function updateMerchantVoucher(id, payload) {
  const res = await api.patch(`/merchant/vouchers/${id}`, payload)
  return unwrap(res).data
}
export async function adminVouchers(restaurantId = null, page = 0, size = 20) {
  const res = await api.get('/admin/vouchers', { params: { restaurantId: restaurantId || undefined, page, size } })
  return unwrap(res)
}
export async function createAdminVoucher(payload) {
  const res = await api.post('/admin/vouchers', payload)
  return unwrap(res).data
}
export async function updateAdminVoucher(id, payload) {
  const res = await api.patch(`/admin/vouchers/${id}`, payload)
  return unwrap(res).data
}
