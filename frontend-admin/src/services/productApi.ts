import http from './http'

export function listProducts() {
  return http.get('/products')
}

export function toggleShelf(id: number, status: 'ON_SHELF' | 'OFF_SHELF') {
  return http.post(`/products/${id}/shelf`, { status })
}
