/** 订单与支付状态的中文映射 */

export const ORDER_STATUS_LABEL: Record<string, string> = {
  WAIT_PAY: '待支付',
  PAID: '已支付',
  CLOSED: '已关闭',
}

export const PAYMENT_STATUS_LABEL: Record<string, string> = {
  UNPAID: '未支付',
  PAID: '已支付',
  REFUNDED: '已退款',
}

export type StatusTone = 'primary' | 'success' | 'warning' | 'neutral' | 'danger'

export function orderStatusTone(status: string): StatusTone {
  if (status === 'PAID') return 'success'
  if (status === 'WAIT_PAY') return 'warning'
  if (status === 'CLOSED') return 'neutral'
  return 'neutral'
}

export function paymentStatusTone(status: string): StatusTone {
  if (status === 'PAID') return 'success'
  if (status === 'UNPAID') return 'warning'
  if (status === 'REFUNDED') return 'neutral'
  return 'neutral'
}
