import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/products' },
    { path: '/login', component: () => import('../pages/LoginPage.vue') },
    { path: '/register', component: () => import('../pages/RegisterPage.vue') },
    { path: '/products', component: () => import('../pages/ProductListPage.vue') },
    { path: '/products/:id', component: () => import('../pages/ProductDetailPage.vue') },
    { path: '/checkout/:id', component: () => import('../pages/CheckoutPage.vue'), meta: { auth: true } },
    { path: '/payment/:orderNo', component: () => import('../pages/PaymentPage.vue'), meta: { auth: true } },
    { path: '/transaction-record', component: () => import('../pages/TransactionRecordPage.vue'), meta: { auth: true } },
  ],
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  if (to.meta.auth && !auth.isLoggedIn) return '/login'
})

export default router
