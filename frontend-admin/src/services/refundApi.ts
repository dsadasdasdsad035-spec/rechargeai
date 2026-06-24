import http from './http'

export interface AdminRefundSummary {
  refundNo: string
  orderNo: string
  userId: number
  amount: number
  status: string
  applyReason: string
  createdAt: string
}

export interface AdminRefundDetail extends AdminRefundSummary {
  productName: string
  paymentChannel: string
  reviewComment: string | null
  reviewerAdminId: number | null
  reviewerName: string | null
  channelRefundNo: string | null
  updatedAt: string
}

export function listRefunds(params: Record<string, unknown>) {
  return http.get<{ data: { items: AdminRefundSummary[]; total: number } }>('/refunds', { params })
}

export function getRefundDetail(refundNo: string) {
  return http.get<{ data: AdminRefundDetail }>(`/refunds/${refundNo}`)
}

export function approveRefund(refundNo: string, reviewComment?: string) {
  return http.post<{ data: AdminRefundDetail }>(`/refunds/${refundNo}/approve`, { reviewComment })
}

export function rejectRefund(refundNo: string, reviewComment: string) {
  return http.post<{ data: AdminRefundDetail }>(`/refunds/${refundNo}/reject`, { reviewComment })
}

export function retryChannelRefund(refundNo: string) {
  return http.post<{ data: AdminRefundDetail }>(`/refunds/${refundNo}/retry-channel`)
}
