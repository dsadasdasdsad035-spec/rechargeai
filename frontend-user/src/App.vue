<script setup lang="ts">
import { RouterLink, RouterView } from 'vue-router'
import { useAuthStore } from './stores/auth'

const auth = useAuthStore()
</script>

<template>
  <header class="header">
    <RouterLink to="/products" class="logo">WildAI 订阅助手</RouterLink>
    <nav>
      <RouterLink to="/products">服务</RouterLink>
      <RouterLink to="/transaction-record">交易记录</RouterLink>
      <RouterLink v-if="!auth.isLoggedIn" to="/login">登录</RouterLink>
      <a v-else href="#" @click.prevent="auth.logout()">退出</a>
    </nav>
  </header>
  <main class="main"><RouterView /></main>
</template>

<style scoped>
.header { display: flex; justify-content: space-between; padding: 12px 16px; border-bottom: 1px solid #eee; }
.logo { font-weight: 700; color: #111; text-decoration: none; }
nav { display: flex; gap: 16px; }
nav a { color: #333; text-decoration: none; }
.main { max-width: 960px; margin: 0 auto; padding: 16px; }
</style>
