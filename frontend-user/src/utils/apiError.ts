import axios from 'axios'

/** 从 axios 或未知异常中解析后端 message 字段 */
export function getApiErrorMessage(error: unknown, fallback: string): string {
  if (axios.isAxiosError(error)) {
    const data = error.response?.data as { message?: string } | undefined
    if (data?.message) return data.message
  }
  return fallback
}

/** 是否为可尝试刷新 Token 的鉴权失败（非业务 403） */
export function isAuthRefreshable(error: unknown): boolean {
  if (!axios.isAxiosError(error)) return false
  const status = error.response?.status
  const code = (error.response?.data as { code?: string } | undefined)?.code
  if (status === 401) return true
  // Spring Security 层 403：code 为 "403"；业务 403 如 USER_001 不刷新
  return status === 403 && code === '403'
}
