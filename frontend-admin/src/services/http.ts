import axios from 'axios'

const http = axios.create({ baseURL: '/admin/api' })

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('adminToken')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

export default http
