import api, { unwrap } from './api.js'

export async function merchantReport(restaurantId, from = null, to = null) {
  const res = await api.get(`/merchant/restaurants/${restaurantId}/report`, {
    params: { from: from || undefined, to: to || undefined },
  })
  return unwrap(res).data
}

export function merchantReportCsvUrl(restaurantId, from = null, to = null) {
  const q = new URLSearchParams()
  if (from) q.set('from', from)
  if (to) q.set('to', to)
  const qs = q.toString()
  return `/merchant/restaurants/${restaurantId}/report.csv${qs ? `?${qs}` : ''}`
}

export async function adminReport(from = null, to = null) {
  const res = await api.get('/admin/report', {
    params: { from: from || undefined, to: to || undefined },
  })
  return unwrap(res).data
}

/** Vẽ biểu đồ cột mini bằng div, không cần thư viện ngoài. */
export function maxOf(points, key) {
  return points.reduce((m, p) => Math.max(m, Number(p[key] ?? 0)), 0)
}
