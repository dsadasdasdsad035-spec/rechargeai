import http from './http'

export interface RefundRequest {
  refundNo: string
  orderNo: string
  amount: number
  status: string
  applyReason: string
  reviewComment: string | null
  createdAt: string
  updatedAt: string
}

export function applyRefund(orderNo: string, applyReason: string) {
  return http.post<{ data: RefundRequest }>(`/orders/${orderNo}/refunds`, { applyReason })
}

export function getLatestRefund(orderNo: string) {
  return http.get<{ data: RefundRequest | null }>(`/orders/${orderNo}/refunds`)
}
