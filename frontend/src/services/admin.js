import api, { unwrap } from './api.js'

export async function adminRestaurants(status = 'PENDING', page = 0, size = 20) {
  const res = await api.get('/admin/restaurants', { params: { status, page, size } })
  return unwrap(res)
}
export async function adminDecide(id, status) {
  const res = await api.patch(`/admin/restaurants/${id}/status`, { status })
  return unwrap(res).data
}
export async function adminOverview() {
  const res = await api.get('/admin/overview')
  return unwrap(res).data
}
export async function adminUsers(role = null, page = 0, size = 20) {
  const res = await api.get('/admin/users', { params: { role: role || undefined, page, size } })
  return unwrap(res)
}
export async function lockUser(id) {
  const res = await api.patch(`/admin/users/${id}/lock`)
  return unwrap(res).data
}
export async function unlockUser(id) {
  const res = await api.patch(`/admin/users/${id}/unlock`)
  return unwrap(res).data
}
export async function adminOrders(status = null, page = 0, size = 20) {
  const res = await api.get('/admin/orders', { params: { status: status || undefined, page, size } })
  return unwrap(res)
}
