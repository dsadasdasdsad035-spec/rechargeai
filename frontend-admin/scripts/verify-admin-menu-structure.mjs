import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { dirname, resolve } from 'node:path'

const __dirname = dirname(fileURLToPath(import.meta.url))
const appVue = readFileSync(resolve(__dirname, '../src/App.vue'), 'utf8')

const productMenuMatch = appVue.match(/<el-sub-menu\s+index="\/products"[\s\S]*?<\/el-sub-menu>/)

if (!productMenuMatch) {
  console.error('产品管理必须是子菜单，并包含产品列表与服务类型教程')
  process.exit(1)
}

const productMenu = productMenuMatch[0]
const requiredItems = [
  ['index="/products"', '产品列表'],
  ['index="/service-types"', '服务类型教程'],
]

for (const [index, label] of requiredItems) {
  if (!productMenu.includes(index) || !productMenu.includes(label)) {
    console.error(`产品管理子菜单缺少：${label}`)
    process.exit(1)
  }
}

const standaloneServiceTypeMenu = /<el-menu-item\s+index="\/service-types"[\s\S]*?<\/el-menu-item>/.test(
  appVue.replace(productMenu, ''),
)

if (standaloneServiceTypeMenu) {
  console.error('服务类型教程不能作为产品管理外的独立菜单展示')
  process.exit(1)
}

console.log('管理端菜单结构校验通过')
