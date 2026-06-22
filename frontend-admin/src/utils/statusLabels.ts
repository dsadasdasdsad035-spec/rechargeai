/** 状态中文映射（管理端） */

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

export const PRODUCT_STATUS_LABEL: Record<string, string> = {
  ON_SHELF: '已上架',
  OFF_SHELF: '已下架',
}

export const USER_STATUS_LABEL: Record<string, string> = {
  NORMAL: '正常',
  DISABLED: '已封禁',
}

export type ElTagType = 'success' | 'warning' | 'info' | 'danger' | 'primary'

export function orderStatusType(status: string): ElTagType {
  if (status === 'PAID') return 'success'
  if (status === 'WAIT_PAY') return 'warning'
  return 'info'
}

export function paymentStatusType(status: string): ElTagType {
  if (status === 'PAID') return 'success'
  if (status === 'UNPAID') return 'warning'
  return 'info'
}

export function productStatusType(status: string): ElTagType {
  return status === 'ON_SHELF' ? 'success' : 'info'
}

export function userStatusType(status: string): ElTagType {
  return status === 'NORMAL' ? 'success' : 'danger'
}
