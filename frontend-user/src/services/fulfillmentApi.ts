import http from './http'

export function submitFulfillmentSupplement(orderNo: string, content: string) {
  return http.post(`/orders/${orderNo}/fulfillment/supplement`, { content })
}

export function confirmFulfillment(orderNo: string) {
  return http.post(`/orders/${orderNo}/fulfillment/confirm`)
}
