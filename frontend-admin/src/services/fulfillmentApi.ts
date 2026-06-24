import http from './http'

export interface FulfillmentTaskSummary {
  taskNo: string
  orderNo: string
  productName: string
  status: string
  assigneeAdminId: number | null
  assigneeName: string | null
  createdAt: string
  updatedAt: string
}

export interface AdminFulfillmentLog {
  logType: string
  content: string
  userVisible: boolean
  operatorId: number | null
  operatorName: string | null
  createdAt: string
}

export interface FulfillmentTaskDetail extends FulfillmentTaskSummary {
  userId: number
  amount: number
  currency: string
  orderStatus: string
  paymentStatus: string
  fulfillmentStatus: string
  subscriptionStart: string | null
  subscriptionEnd: string | null
  failureReason: string | null
  logs: AdminFulfillmentLog[]
}

export function listFulfillmentTasks(params: Record<string, unknown>) {
  return http.get<{ data: { items: FulfillmentTaskSummary[]; total: number } }>(
    '/fulfillment-tasks',
    { params },
  )
}

export function getFulfillmentTask(taskNo: string) {
  return http.get<{ data: FulfillmentTaskDetail }>(`/fulfillment-tasks/${taskNo}`)
}

export function assignTask(taskNo: string, assigneeAdminId: number) {
  return http.post(`/fulfillment-tasks/${taskNo}/assign`, { assigneeAdminId })
}

export function addTaskNote(taskNo: string, content: string, userVisible = true) {
  return http.post(`/fulfillment-tasks/${taskNo}/notes`, { content, userVisible })
}

export function waitUserTask(taskNo: string, instruction?: string) {
  return http.post(`/fulfillment-tasks/${taskNo}/wait-user`, { instruction })
}

export function markTaskSuccess(
  taskNo: string,
  payload: { subscriptionStart: string; subscriptionEnd: string; remark?: string },
) {
  return http.post(`/fulfillment-tasks/${taskNo}/success`, payload)
}

export function markTaskFailed(
  taskNo: string,
  payload: { failureReason: string; remark?: string },
) {
  return http.post(`/fulfillment-tasks/${taskNo}/fail`, payload)
}
