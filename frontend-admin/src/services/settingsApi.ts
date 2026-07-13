import http from './http'

export interface PaymentSetting {
  usdToCnyRate: number
  updatedAt?: string | null
}

export interface NotificationSetting {
  supportOfflineNotifyEmail: string
  orderPaidNotifyEmail: string
  orderPaidNotifyEnabled: boolean
  updatedAt?: string | null
}

export function getPaymentSetting() {
  return http.get<{ data: PaymentSetting }>('/settings/payment')
}

export function updatePaymentSetting(payload: { usdToCnyRate: number }) {
  return http.put<{ data: PaymentSetting }>('/settings/payment', payload)
}

export function getNotificationSetting() {
  return http.get<{ data: NotificationSetting }>('/settings/notifications')
}

export function updateNotificationSetting(payload: {
  supportOfflineNotifyEmail: string
  orderPaidNotifyEmail: string
  orderPaidNotifyEnabled: boolean
}) {
  return http.put<{ data: NotificationSetting }>('/settings/notifications', payload)
}
