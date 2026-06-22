import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/', redirect: '/orders' },
    { path: '/login', component: () => import('../views/LoginView.vue') },
    { path: '/products', component: () => import('../views/ProductListView.vue'), meta: { auth: true } },
    { path: '/products/new', component: () => import('../views/ProductFormView.vue'), meta: { auth: true } },
    { path: '/orders', component: () => import('../views/OrderListView.vue'), meta: { auth: true } },
    { path: '/users', component: () => import('../views/UserListView.vue'), meta: { auth: true } },
  ],
})

router.beforeEach((to) => {
  if (to.meta.auth && !localStorage.getItem('adminToken')) return '/login'
})

export default router
