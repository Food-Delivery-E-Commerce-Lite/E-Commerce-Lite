import api, { unwrap, apiError } from './api.js'

export async function searchRestaurants({ keyword = '', categoryId = null, minRating = null, sort = 'newest', page = 0, size = 12 } = {}) {
  const res = await api.get('/restaurants', {
    params: {
      keyword: keyword || undefined,
      categoryId: categoryId || undefined,
      minRating: minRating || undefined,
      sort, page, size,
    },
  })
  return unwrap(res)
}
export async function getRestaurant(id) {
  const res = await api.get(`/restaurants/${id}`)
  return unwrap(res).data
}
export async function getMenu(id) {
  const res = await api.get(`/restaurants/${id}/menu`)
  return unwrap(res).data
}
export async function getCategories() {
  const res = await api.get('/categories')
  return unwrap(res).data
}
export async function getReviews(restaurantId, page = 0, size = 10) {
  const res = await api.get(`/restaurants/${restaurantId}/reviews`, { params: { page, size } })
  return unwrap(res)
}

export { apiError }
