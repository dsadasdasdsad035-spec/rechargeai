import http from './http'

export interface ServiceTypeGuide {
  serviceType: string
  displayName: string
  accountTutorial?: string | null
  tokenTutorial?: string | null
}

export function listServiceTypes() {
  return http.get<{ data: ServiceTypeGuide[] }>('/service-types')
}

export function updateServiceType(serviceType: string, payload: {
  displayName: string
  accountTutorial?: string
  tokenTutorial?: string
}) {
  return http.put<{ data: ServiceTypeGuide }>(`/service-types/${serviceType}`, payload)
}
