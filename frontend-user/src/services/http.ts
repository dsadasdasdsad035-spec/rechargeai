import axios from 'axios'
import { isAuthRefreshable } from '../utils/apiError'

const http = axios.create({ baseURL: '/api' })

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('accessToken')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

http.interceptors.response.use(
  (res) => res,
  async (error) => {
    if (isAuthRefreshable(error)) {
      const refresh = localStorage.getItem('refreshToken')
      if (refresh && !error.config._retry) {
        error.config._retry = true
        try {
          const { data } = await axios.post('/api/auth/refresh', { refreshToken: refresh })
          localStorage.setItem('accessToken', data.data.accessToken)
          localStorage.setItem('refreshToken', data.data.refreshToken)
          error.config.headers.Authorization = `Bearer ${data.data.accessToken}`
          return http(error.config)
        } catch {
          // 刷新失败，走下方登出
        }
      }
      localStorage.clear()
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

export default http
