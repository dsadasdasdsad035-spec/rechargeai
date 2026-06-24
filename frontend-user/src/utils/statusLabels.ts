/** 订单与支付状态的中文映射 */

export const ORDER_STATUS_LABEL: Record<string, string> = {
  WAIT_PAY: '待支付',
  PAID: '已支付',
  FULFILLING: '履约中',
  SUCCESS: '已完成',
  FAILED: '履约失败',
  CLOSED: '已关闭',
}

export const PAYMENT_STATUS_LABEL: Record<string, string> = {
  UNPAID: '未支付',
  PAYING: '支付中',
  PAID: '已支付',
  REFUNDED: '已退款',
}

export const FULFILLMENT_STATUS_LABEL: Record<string, string> = {
  NOT_STARTED: '未开始',
  PENDING: '待处理',
  PROCESSING: '处理中',
  WAIT_USER: '等待您操作',
  WAIT_ADMIN: '等待客服',
  SUCCESS: '履约成功',
  FAILED: '履约失败',
}

export const REFUND_STATUS_LABEL: Record<string, string> = {
  PENDING: '待审核',
  APPROVED: '审核通过',
  REJECTED: '已驳回',
  REFUNDING: '退款中',
  COMPLETED: '已退款',
  CHANNEL_REFUND_FAILED: '渠道退款失败',
}

export type StatusTone = 'primary' | 'success' | 'warning' | 'neutral' | 'danger'

export function orderStatusTone(status: string): StatusTone {
  if (status === 'SUCCESS' || status === 'PAID') return 'success'
  if (status === 'FULFILLING') return 'primary'
  if (status === 'WAIT_PAY') return 'warning'
  if (status === 'FAILED') return 'danger'
  if (status === 'CLOSED') return 'neutral'
  return 'neutral'
}

export function paymentStatusTone(status: string): StatusTone {
  if (status === 'PAID') return 'success'
  if (status === 'UNPAID') return 'warning'
  if (status === 'REFUNDED') return 'neutral'
  return 'neutral'
}
