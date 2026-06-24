import http from './http'

export interface PayoutAccount {
  id: number
  accountName: string
  bankName: string
  accountLast4: string
  enabled: boolean
}

export function listPayoutAccounts(enabledOnly = false) {
  return http.get<{ data: PayoutAccount[] }>('/payout-accounts', { params: { enabledOnly } })
}

export function createPayoutAccount(body: {
  accountName: string
  bankName: string
  accountNo: string
}) {
  return http.post('/payout-accounts', body)
}

export function updatePayoutAccount(id: number, body: Partial<{
  accountName: string
  bankName: string
  accountNo: string
  enabled: boolean
}>) {
  return http.put(`/payout-accounts/${id}`, body)
}
