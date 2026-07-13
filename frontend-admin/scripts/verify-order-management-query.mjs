import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { dirname, resolve } from 'node:path'

const __dirname = dirname(fileURLToPath(import.meta.url))
const source = readFileSync(resolve(__dirname, '../src/views/OrderListView.vue'), 'utf8')

const requiredSnippets = [
  ['orderNoFilter', '订单号筛选'],
  ['userIdFilter', '用户 ID 筛选'],
  ['orderStatusFilter', '订单状态筛选'],
  ['paymentStatusFilter', '支付状态筛选'],
  ['createdRange', '下单时间筛选'],
  ['<el-pagination', '分页控件'],
  ["responseType: 'blob'", '带登录态的 Blob 导出'],
]

for (const [snippet, label] of requiredSnippets) {
  if (!source.includes(snippet)) {
    console.error(`订单管理缺少：${label}`)
    process.exit(1)
  }
}

if (source.includes("window.open('/admin/api/orders/export")) {
  console.error('订单导出不能使用 window.open，否则不会携带管理端 Authorization')
  process.exit(1)
}

console.log('订单管理查询与导出结构校验通过')
