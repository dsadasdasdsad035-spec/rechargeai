import http from './http'

export function listProducts() {
  return http.get('/products')
}

export function getProduct(id: string | number) {
  return http.get(`/products/${id}`)
}
