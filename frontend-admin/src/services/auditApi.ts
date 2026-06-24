import http from './http'

export interface AuditLog {
  id: number
  operatorId: number
  operatorName: string
  operationType: string
  targetType: string
  targetId: string
  beforeValue: string | null
  afterValue: string | null
  reason: string | null
  createdAt: string
}

export function listAuditLogs(params: Record<string, unknown>) {
  return http.get<{ data: { items: AuditLog[]; total: number } }>('/audit-logs', { params })
}
