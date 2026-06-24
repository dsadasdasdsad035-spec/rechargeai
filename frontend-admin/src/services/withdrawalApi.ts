import http from './http'

export interface WithdrawalSummary {
  withdrawalNo: string
  amount: number
  actualAmount: number | null
  status: string
  payoutAccountId: number
  payoutAccountSummary: string
  applicantAdminId: number
  applicantName: string
  appliedAt: string
}

export interface WithdrawalDetail extends WithdrawalSummary {
  applicantAdminId: number
  approverAdminId: number | null
  approverName: string | null
  payoutConfirmerId: number | null
  payoutConfirmerName: string | null
  externalVoucherNo: string | null
  remark: string | null
  rejectReason: string | null
  varianceReason: string | null
  approvedAt: string | null
  paidAt: string | null
}

export function listWithdrawals(params: Record<string, unknown>) {
  return http.get<{ data: { items: WithdrawalSummary[]; total: number } }>('/withdrawals', { params })
}

export function createWithdrawal(body: { amount: number; payoutAccountId: number; remark?: string }) {
  return http.post<{ data: WithdrawalDetail }>('/withdrawals', body)
}

export function approveWithdrawal(withdrawalNo: string) {
  return http.post(`/withdrawals/${withdrawalNo}/approve`)
}

export function rejectWithdrawal(withdrawalNo: string, reason: string) {
  return http.post(`/withdrawals/${withdrawalNo}/reject`, { reason })
}

export function cancelWithdrawal(withdrawalNo: string) {
  return http.post(`/withdrawals/${withdrawalNo}/cancel`)
}

export function confirmPayout(withdrawalNo: string, body: {
  actualAmount: number
  paidAt: string
  externalVoucherNo: string
  varianceReason?: string
}) {
  return http.post(`/withdrawals/${withdrawalNo}/confirm-payout`, body)
}

export function exportWithdrawals(status?: string) {
  return http.get('/withdrawals/export', { params: { status }, responseType: 'blob' })
}
