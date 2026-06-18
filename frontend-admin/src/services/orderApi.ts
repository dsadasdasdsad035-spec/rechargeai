import http from './http'

export function listOrders(params: Record<string, unknown>) {
  return http.get('/orders', { params })
}

export function getOrderDetail(orderNo: string) {
  return http.get(`/orders/${orderNo}`)
}

export function exportOrdersUrl(params?: Record<string, string>) {
  const qs = params ? '?' + new URLSearchParams(params).toString() : ''
  return `/admin/api/orders/export${qs}`
}
