import http from './http'

export function createOrder(productId: number, fields: Record<string, string>) {
  return http.post('/orders', { productId, fields })
}

export function listOrders(params: Record<string, unknown>) {
  return http.get('/orders', { params })
}

export function getOrderDetail(orderNo: string) {
  return http.get(`/orders/${orderNo}`)
}

export function payOrder(orderNo: string, channel: string) {
  return http.post(`/orders/${orderNo}/pay`, { channel })
}
