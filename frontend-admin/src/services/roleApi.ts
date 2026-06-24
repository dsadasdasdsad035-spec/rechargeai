import http from './http'

export interface AdminRole {
  id: number
  roleCode: string
  roleName: string
}

export interface AdminAccount {
  id: number
  username: string
  displayName: string | null
  status: string
  roleCodes: string[]
}

export function listRoles() {
  return http.get<{ data: AdminRole[] }>('/roles')
}

export function listAdmins() {
  return http.get<{ data: AdminAccount[] }>('/admins')
}

export function createAdmin(payload: {
  username: string
  password: string
  displayName?: string
  roleCodes: string[]
}) {
  return http.post<{ data: AdminAccount }>('/admins', payload)
}

export function setAdminRoles(adminId: number, roleCodes: string[]) {
  return http.put<{ data: AdminAccount }>(`/admins/${adminId}/roles`, { roleCodes })
}

export function updateAdminStatus(adminId: number, status: string) {
  return http.post<{ data: AdminAccount }>(`/admins/${adminId}/status`, { status })
}
