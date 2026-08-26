import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/', redirect: '/orders' },
    { path: '/login', component: () => import('../views/LoginView.vue') },
    { path: '/products', component: () => import('../views/ProductListView.vue'), meta: { auth: true } },
    { path: '/products/new', component: () => import('../views/ProductFormView.vue'), meta: { auth: true } },
    { path: '/products/:id/edit', component: () => import('../views/ProductFormView.vue'), meta: { auth: true } },
    { path: '/service-types', component: () => import('../views/ServiceTypeGuideView.vue'), meta: { auth: true } },
    { path: '/articles', component: () => import('../views/ArticleListView.vue'), meta: { auth: true } },
    { path: '/articles/new', component: () => import('../views/ArticleEditView.vue'), meta: { auth: true } },
    { path: '/articles/:id/edit', component: () => import('../views/ArticleEditView.vue'), meta: { auth: true } },
    { path: '/orders', component: () => import('../views/OrderListView.vue'), meta: { auth: true } },
    { path: '/fulfillment', component: () => import('../views/FulfillmentListView.vue'), meta: { auth: true } },
    { path: '/fulfillment/:taskNo', component: () => import('../views/FulfillmentDetailView.vue'), meta: { auth: true } },
    { path: '/support', component: () => import('../views/SupportChatView.vue'), meta: { auth: true } },
    { path: '/refunds', component: () => import('../views/RefundListView.vue'), meta: { auth: true } },
    { path: '/refunds/:refundNo', component: () => import('../views/RefundDetailView.vue'), meta: { auth: true } },
    { path: '/users', component: () => import('../views/UserListView.vue'), meta: { auth: true } },
    { path: '/roles', component: () => import('../views/RoleListView.vue'), meta: { auth: true } },
    { path: '/audit-logs', component: () => import('../views/AuditLogListView.vue'), meta: { auth: true } },
    { path: '/finance', component: () => import('../views/FinanceOverviewView.vue'), meta: { auth: true } },
    { path: '/finance/ledger', component: () => import('../views/LedgerListView.vue'), meta: { auth: true } },
    { path: '/finance/payout-accounts', component: () => import('../views/PayoutAccountListView.vue'), meta: { auth: true } },
    { path: '/finance/withdrawals', component: () => import('../views/WithdrawalListView.vue'), meta: { auth: true } },
    { path: '/finance/settings', component: () => import('../views/PaymentSettingsView.vue'), meta: { auth: true } },
    { path: '/settings', component: () => import('../views/SystemSettingsView.vue'), meta: { auth: true } },
  ],
})

router.beforeEach((to) => {
  if (to.meta.auth && !localStorage.getItem('adminToken')) return '/login'
})

export default router
