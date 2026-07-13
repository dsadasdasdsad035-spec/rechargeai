import http from './http'

export interface SupportSession {
  sessionNo: string
  userId: number
  subject: string
  status: string
  lastMessage?: string | null
  unreadUserCount: number
  unreadAdminCount: number
  lastMessageAt?: string | null
  createdAt: string
  updatedAt: string
}

export interface SupportMessage {
  id: number
  sessionNo: string
  senderType: 'USER' | 'ADMIN'
  senderId: number
  content: string
  createdAt: string
}

export interface PageResult<T> {
  pageNo: number
  pageSize: number
  total: number
  items: T[]
}

export interface SupportEvent {
  type: 'SESSION_CREATED' | 'MESSAGE'
  session?: SupportSession
  message?: SupportMessage
}

export function listSupportSessions() {
  return http.get<{ data: PageResult<SupportSession> }>('/support/sessions', {
    params: { pageNo: 1, pageSize: 50 },
  })
}

export function listSupportMessages(sessionNo: string) {
  return http.get<{ data: PageResult<SupportMessage> }>(`/support/sessions/${sessionNo}/messages`, {
    params: { pageNo: 1, pageSize: 100 },
  })
}

export function sendSupportMessage(sessionNo: string, content: string) {
  return http.post<{ data: SupportMessage }>(`/support/sessions/${sessionNo}/messages`, { content })
}

export function createSupportSocket(onMessage: (event: SupportEvent) => void, onClose?: () => void) {
  const token = localStorage.getItem('adminToken')
  if (!token) return null
  const protocol = window.location.protocol === 'https:' ? 'wss' : 'ws'
  const socket = new WebSocket(`${protocol}://${window.location.host}/ws/support?token=${encodeURIComponent(token)}`)
  socket.addEventListener('message', (event) => {
    onMessage(JSON.parse(event.data))
  })
  socket.addEventListener('close', () => {
    onClose?.()
  })
  return socket
}
