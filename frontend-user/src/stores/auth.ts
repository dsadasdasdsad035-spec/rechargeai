import { defineStore } from 'pinia'
import http from '../services/http'

interface AuthState {
  accessToken: string | null
  refreshToken: string | null
  userNo: string | null
}

export const useAuthStore = defineStore('auth', {
  state: (): AuthState => ({
    accessToken: localStorage.getItem('accessToken'),
    refreshToken: localStorage.getItem('refreshToken'),
    userNo: localStorage.getItem('userNo'),
  }),
  getters: {
    isLoggedIn: (s) => !!s.accessToken,
  },
  actions: {
    async loginPhone(phone: string, verifyCode: string) {
      const { data } = await http.post('/auth/login', { phone, verifyCode })
      this.setTokens(data.data)
    },
    async registerPhone(phone: string, verifyCode: string) {
      const { data } = await http.post('/auth/register', { type: 'PHONE', phone, verifyCode })
      this.setTokens(data.data)
    },
    async registerEmail(email: string, verifyCode: string) {
      const { data } = await http.post('/auth/register', { type: 'EMAIL', email, verifyCode })
      this.setTokens(data.data)
    },
    async sendCodePhone(phone: string) {
      return http.post('/auth/send-code', { phone })
    },
    async sendCodeEmail(email: string) {
      return http.post('/auth/send-code', { email })
    },
    /** @deprecated 使用 sendCodePhone */
    async sendCode(phone: string) {
      return this.sendCodePhone(phone)
    },
    setTokens(payload: { accessToken: string; refreshToken: string; userNo: string }) {
      this.accessToken = payload.accessToken
      this.refreshToken = payload.refreshToken
      this.userNo = payload.userNo
      localStorage.setItem('accessToken', payload.accessToken)
      localStorage.setItem('refreshToken', payload.refreshToken)
      localStorage.setItem('userNo', payload.userNo)
    },
    logout() {
      this.accessToken = null
      this.refreshToken = null
      this.userNo = null
      localStorage.clear()
    },
  },
})
