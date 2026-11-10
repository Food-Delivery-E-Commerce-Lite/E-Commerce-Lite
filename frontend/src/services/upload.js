import api, { unwrap } from './api.js'

export async function uploadImage(file) {
  const form = new FormData()
  form.append('file', file)
  const res = await api.post('/uploads', form, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
  return unwrap(res).data
}

// Chuẩn hóa URL ảnh trả về từ backend (/uploads/...) cho cả dev lẫn prod
export function resolveImage(url) {
  if (!url) return null
  if (url.startsWith('http://') || url.startsWith('https://')) return url
  return url
}

export const FALLBACK_IMG =
  'data:image/svg+xml;utf8,' +
  encodeURIComponent(
    `<svg xmlns="http://www.w3.org/2000/svg" width="480" height="300"><rect width="100%" height="100%" fill="#eee"/><text x="50%" y="50%" dominant-baseline="middle" text-anchor="middle" fill="#999" font-size="20">Không có ảnh</text></svg>`,
  )

export function withFallback(e) {
  const el = e.currentTarget
  if (el.dataset.fbk) return
  el.dataset.fbk = '1'
  el.src = FALLBACK_IMG
}
