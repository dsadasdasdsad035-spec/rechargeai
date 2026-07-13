<script setup lang="ts">
import { ref } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { useAuthStore } from './stores/auth'
import SupportChatWidget from './components/SupportChatWidget.vue'

const auth = useAuthStore()
const route = useRoute()
const menuOpen = ref(false)

function closeMenu() {
  menuOpen.value = false
}

function logout() {
  auth.logout()
  closeMenu()
}
</script>

<template>
  <header class="header">
    <div class="header__inner">
      <RouterLink to="/products" class="logo" @click="closeMenu">
        <span class="logo__mark">R</span>
        <span class="logo__text">RechargeAi</span>
      </RouterLink>

      <button
        class="menu-toggle"
        :aria-expanded="menuOpen"
        aria-label="打开菜单"
        @click="menuOpen = !menuOpen"
      >
        <span class="menu-toggle__bar" :class="{ open: menuOpen }" />
      </button>

      <nav class="nav" :class="{ 'nav--open': menuOpen }">
        <RouterLink
          to="/products"
          class="nav__link"
          active-class="nav__link--active"
          @click="closeMenu"
        >
          服务
        </RouterLink>
        <RouterLink
          to="/transaction-record"
          class="nav__link"
          active-class="nav__link--active"
          @click="closeMenu"
        >
          交易记录
        </RouterLink>
        <RouterLink
          v-if="!auth.isLoggedIn"
          to="/login"
          class="nav__link"
          :class="{ 'nav__link--active': route.path === '/login' || route.path === '/register' }"
          @click="closeMenu"
        >
          登录
        </RouterLink>
        <button v-else type="button" class="nav__link nav__link--btn" @click="logout">
          退出
        </button>
      </nav>
    </div>
  </header>

  <main class="main">
    <RouterView />
  </main>

  <SupportChatWidget v-if="auth.isLoggedIn" />
</template>

<style scoped>
.header {
  position: sticky;
  top: 0;
  z-index: 100;
  background: oklch(0.98 0.008 85 / 0.92);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid var(--color-border);
}

.header__inner {
  max-width: var(--content-max);
  margin: 0 auto;
  height: var(--header-height);
  padding: 0 var(--space-md);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-md);
}

.logo {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  text-decoration: none;
  color: var(--color-text-heading);
}

.logo__mark {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-primary);
  color: oklch(0.98 0.01 165);
  font-family: var(--font-serif);
  font-weight: 600;
  font-size: 1rem;
  border-radius: var(--radius-sm);
}

.logo__text {
  font-family: var(--font-serif);
  font-weight: 600;
  font-size: 1.0625rem;
  letter-spacing: -0.02em;
}

.menu-toggle {
  display: none;
  width: 44px;
  height: 44px;
  align-items: center;
  justify-content: center;
  border: none;
  background: transparent;
  border-radius: var(--radius-sm);
}

.menu-toggle__bar,
.menu-toggle__bar::before,
.menu-toggle__bar::after {
  display: block;
  width: 20px;
  height: 2px;
  background: var(--color-text-heading);
  border-radius: 1px;
  transition: transform var(--duration-normal) var(--ease-out);
}

.menu-toggle__bar {
  position: relative;
}

.menu-toggle__bar::before,
.menu-toggle__bar::after {
  content: '';
  position: absolute;
  left: 0;
}

.menu-toggle__bar::before {
  top: -6px;
}

.menu-toggle__bar::after {
  top: 6px;
}

.menu-toggle__bar.open {
  background: transparent;
}

.menu-toggle__bar.open::before {
  transform: translateY(6px) rotate(45deg);
}

.menu-toggle__bar.open::after {
  transform: translateY(-6px) rotate(-45deg);
}

.nav {
  display: flex;
  align-items: center;
  gap: var(--space-xs);
}

.nav__link {
  padding: var(--space-sm) var(--space-md);
  border-radius: var(--radius-sm);
  font-size: 0.9375rem;
  font-weight: 500;
  color: var(--color-text);
  text-decoration: none;
  border: none;
  background: transparent;
  min-height: 36px;
  display: inline-flex;
  align-items: center;
  transition:
    background var(--duration-fast) var(--ease-out),
    color var(--duration-fast) var(--ease-out);
}

.nav__link:hover {
  background: var(--color-neutral-bg);
  color: var(--color-text-heading);
}

.nav__link--active {
  background: var(--color-primary-muted);
  color: var(--color-primary-hover);
}

.nav__link--btn {
  cursor: pointer;
  font-family: inherit;
}

.main {
  flex: 1;
  max-width: var(--content-max);
  width: 100%;
  margin: 0 auto;
}

@media (max-width: 639px) {
  .menu-toggle {
    display: flex;
  }

  .nav {
    position: fixed;
    top: var(--header-height);
    left: 0;
    right: 0;
    flex-direction: column;
    align-items: stretch;
    padding: var(--space-md);
    gap: var(--space-xs);
    background: var(--color-surface-raised);
    border-bottom: 1px solid var(--color-border);
    box-shadow: var(--shadow-md);
    transform: translateY(-8px);
    opacity: 0;
    pointer-events: none;
    transition:
      opacity var(--duration-normal) var(--ease-out),
      transform var(--duration-normal) var(--ease-out);
  }

  .nav--open {
    transform: translateY(0);
    opacity: 1;
    pointer-events: auto;
  }

  .nav__link {
    min-height: 44px;
    padding: var(--space-md);
  }
}
</style>
