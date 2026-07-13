import http from './http'

export interface FinanceOverview {
  settledRevenue: number
  /** 退款单 COMPLETED，原路退回用户 */
  userRefunded: number
  availableBalance: number
  frozenForWithdrawal: number
  totalWithdrawn: number
}

export interface LedgerEntry {
  entryNo: string
  entryType: string
  amount: number
  balanceAfter: number
  refType: string
  refId: string
  orderId: number | null
  createdAt: string
}

export function getFinanceOverview() {
  return http.get<{ data: FinanceOverview }>('/finance/overview')
}

export function listLedger(params: Record<string, unknown>) {
  return http.get<{ data: { items: LedgerEntry[]; total: number } }>('/finance/ledger', { params })
}
