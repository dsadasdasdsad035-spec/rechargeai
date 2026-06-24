/** 状态中文映射（管理端） */

export const ORDER_STATUS_LABEL: Record<string, string> = {
  WAIT_PAY: '待支付',
  PAID: '已支付',
  CLOSED: '已关闭',
  FULFILLING: '履约中',
  SUCCESS: '已完成',
  FAILED: '履约失败',
  REFUNDING: '退款中',
  REFUNDED: '已退款',
}

export const PAYMENT_STATUS_LABEL: Record<string, string> = {
  UNPAID: '未支付',
  PAID: '已支付',
  REFUNDING: '退款中',
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
  if (status === 'PAID' || status === 'SUCCESS') return 'success'
  if (status === 'WAIT_PAY') return 'warning'
  if (status === 'FAILED') return 'danger'
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

export const REFUND_STATUS_LABEL: Record<string, string> = {
  PENDING: '待审核',
  APPROVED: '审核通过',
  REJECTED: '已驳回',
  REFUNDING: '退款中',
  COMPLETED: '已退款',
  CHANNEL_REFUND_FAILED: '渠道失败',
}

export function refundStatusType(status: string): ElTagType {
  if (status === 'COMPLETED') return 'success'
  if (status === 'REJECTED' || status === 'CHANNEL_REFUND_FAILED') return 'danger'
  if (status === 'PENDING') return 'warning'
  return 'primary'
}

const REFUND_APPROVER_ROLES = ['SUPER_ADMIN', 'OPS_ADMIN', 'FINANCE']

export function canApproveRefund(): boolean {
  try {
    const roles: string[] = JSON.parse(localStorage.getItem('adminRoles') ?? '[]')
    return roles.some((r) => REFUND_APPROVER_ROLES.includes(r))
  } catch {
    return false
  }
}

export const FULFILLMENT_STATUS_LABEL: Record<string, string> = {
  PENDING: '待处理',
  PROCESSING: '处理中',
  WAIT_USER: '等待用户',
  WAIT_ADMIN: '等待管理员',
  SUCCESS: '成功',
  FAILED: '失败',
  CANCELLED: '已取消',
  NOT_STARTED: '未开始',
}

export function fulfillmentStatusType(status: string): ElTagType {
  if (status === 'SUCCESS') return 'success'
  if (status === 'FAILED') return 'danger'
  if (status === 'WAIT_USER' || status === 'WAIT_ADMIN') return 'warning'
  if (status === 'PROCESSING') return 'primary'
  return 'info'
}

const FULFILLMENT_WRITE_ROLES = ['SUPER_ADMIN', 'OPS_ADMIN', 'CUSTOMER_SERVICE']

export function canManageFulfillment(): boolean {
  try {
    const roles: string[] = JSON.parse(localStorage.getItem('adminRoles') ?? '[]')
    return roles.some((r) => FULFILLMENT_WRITE_ROLES.includes(r))
  } catch {
    return false
  }
}
