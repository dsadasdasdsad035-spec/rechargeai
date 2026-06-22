import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig(({ mode }) => ({
  plugins: [vue()],
  base: mode === 'production' ? '/admin/' : '/',
  server: {
    port: 5174,
    proxy: {
      '/admin/api': { target: 'http://localhost:8080', changeOrigin: true },
    },
  },
}))
