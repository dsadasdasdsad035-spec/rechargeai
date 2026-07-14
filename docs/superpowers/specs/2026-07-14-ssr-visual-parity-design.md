# RechargeAi SSR 公开页视觉一致性设计

**日期**：2026-07-14

**分支**：`fix/ssr-visual-parity`

**状态**：已批准

## 1. 背景

SEO 功能将 `/products`、`/products/{id}`、`/articles`、`/articles/{slug}` 和公开错误页改为 Spring MVC 服务端渲染。现有实现满足首次 HTML 可抓取要求，但 SSR 模板只迁移了少量颜色和间距，没有复用 Vue 用户端的完整设计系统，导致线上出现 60px 大标题、系统字体标题、纯文本品牌标识和卡片层级变化。

本修复保留 SSR、SEO 元数据、结构化数据、路由和无 JavaScript 可访问性，只恢复视觉与交互一致性。

## 2. 目标与非目标

### 2.1 目标

- 所有 SSR 公开页使用与 Vue 用户端一致的墨绿、暖中性色、衬线标题、圆角、阴影和间距节奏。
- 站点头部恢复绿色 `R` 标记和 `RechargeAi` 衬线品牌文字。
- 产品列表和产品详情尽量复刻原 Vue 页面结构、信息层级、卡片、价格和 CTA。
- 文章列表、文章详情和错误页保留现有内容结构，但套用同一品牌设计系统。
- 桌面、平板和手机均保持清晰导航、合理字号和至少 44px 的主要触控区域。
- 首次 HTML 中继续包含正文、链接、canonical、JSON-LD 和公开页语义结构。

### 2.2 非目标

- 不回退为 Vue SPA，也不引入 Nuxt、Node SSR 或客户端 hydration。
- 不修改产品、文章、支付或认证业务逻辑。
- 不修改 SEO 文案、canonical、robots、JSON-LD 或 sitemap 规则。
- 不新增远程字体依赖；继续使用系统字体与本地可用的 Georgia/中文宋体回退。
- 不重新设计管理端或用户端私有页面。

## 3. 设计方向

沿用 Vue 用户端已经确定的“精致工具型 + 支付信任感”方向：暖米色背景、墨绿主色、克制的边框与阴影、衬线字体承担品牌和重点数据，无衬线字体承担正文与导航。修复目标是恢复既有品牌，而不是创造另一套公开站设计。

视觉来源优先级：

1. `frontend-user/src/style.css` 的设计变量和全局排版。
2. `frontend-user/src/App.vue` 的 Logo、导航与移动端头部。
3. `ProductListPage.vue`、`ProductDetailPage.vue`、`BaseCard.vue` 和 `BaseButton.vue` 的产品页面表现。
4. SSR 文章页现有语义结构和可读性要求。

## 4. 页面与组件设计

### 4.1 共享头部

- 品牌链接拆为 `.logo__mark` 与 `.logo__text`，尺寸、字体、颜色和间距对齐 Vue 头部。
- 桌面导航使用“服务”“文章”“交易记录”“登录”，保持普通链接以跨越 SSR/SPA 边界。
- 桌面头部高度为 56px，最大内容宽度为 960px。
- 移动端使用原生 `<details>`/`<summary>` 实现无 JavaScript 菜单，视觉对齐原汉堡按钮；菜单展开后纵向显示全部导航。
- 所有导航链接保留清晰的 hover 与 `:focus-visible` 状态。

### 4.2 产品列表

- 标题恢复为“AI 订阅服务”，使用约 22–26px 的衬线层级，不再使用 60px 展示标题。
- 描述恢复原 Vue 文案和 15px 弱化文本层级。
- 产品卡在 480px 以下单列、480px 起两列、768px 起三列。
- 卡片恢复产品名、预计处理时间、衬线价格、服务周期和“查看详情 →”层级；整张卡片可点击。
- 卡片使用暖白表面、1px 边框、10px 圆角、轻阴影和最多 1px 的 hover 上移。

### 4.3 产品详情

- 标题与价格放在页面头部，价格使用 24px 衬线字体，周期使用 15px 无衬线弱化文本。
- 产品信息使用 `dl` 行式布局呈现预计处理和退款政策。
- 合规说明使用警告色浅背景独立呈现。
- “立即订购”恢复 48px 高、全宽墨绿主按钮，并继续链接到 `/checkout/{id}`。

### 4.4 文章列表与详情

- 文章列表沿用卡片结构，但使用同一表面色、边框、圆角、阴影和衬线标题。
- 列表标题使用与产品页相同的页面标题层级，封面图保持 16:9。
- 文章详情内容宽度保持 760px；文章标题可比普通页面标题高一级，但桌面上限不超过 36px，移动端不超过 30px。
- 正文继续支持安全 HTML、图片、代码块和横向滚动表格；正文排版使用 1.8 行高。

### 4.5 错误页

- 保留 404/500 语义与 `noindex,nofollow`。
- 错误卡片复用统一卡片与按钮样式，标题字号不超过普通文章详情标题。

## 5. CSS 与兼容策略

- `backend/src/main/resources/static/seo/site.css` 补齐 Vue 设计系统中的颜色、字体、间距、圆角、阴影、动效与布局变量。
- 每个 `oklch()` 值之前保留十六进制或 RGB 降级。
- 使用 `clamp()` 时设置克制上限；公开页主标题不得再次超过 36px。
- 保留 Safari 14 可用的 CSS 降级、`-webkit-backdrop-filter` 和 `prefers-reduced-motion`。
- 不依赖 JavaScript完成导航、正文阅读、产品详情跳转和购买入口。

## 6. 模板变更边界

预计只修改：

- `backend/src/main/resources/templates/public/fragments.html`
- `backend/src/main/resources/templates/public/product-list.html`
- `backend/src/main/resources/templates/public/product-detail.html`
- `backend/src/main/resources/templates/public/article-list.html`
- `backend/src/main/resources/templates/public/article-detail.html`
- `backend/src/main/resources/templates/public/error.html`（仅在结构对齐需要时）
- `backend/src/main/resources/static/seo/site.css`
- 对应的公开页面测试与浏览器验收测试

Vue 页面只作为视觉来源，不修改、不重新注册公开路由。

## 7. 测试与验收

### 7.1 自动化契约

- 先扩展公开页面测试并观察失败，覆盖 Logo 标记、产品卡 CTA、产品详情信息结构和公开 CSS 设计标识。
- 修改模板/CSS 后运行相关测试，确认 SSR 正文、canonical、JSON-LD、404 和购买链接原有断言继续通过。
- 运行后端完整测试与打包，保证静态资源被正确打入产物。

### 7.2 浏览器验收

在 Chromium、Firefox 和 WebKit 中验证：

- `/products`
- 一个已上架产品详情
- `/articles`
- 一个已发布文章详情
- 公开 404 页面

桌面与移动端至少检查：Logo、标题计算字号、导航、网格列数、卡片、按钮、横向溢出和键盘焦点。上线后再次对生产域名截图，并确认产品页主标题桌面不超过 28px、文章详情标题不超过 36px。

## 8. 风险与控制

- **共享 CSS 影响文章正文**：通过限定 `.page-heading`、`.article-heading`、`.product-*` 和 `.article-body` 选择器避免全局误伤。
- **移动菜单无客户端状态**：使用原生 `<details>`，不增加脚本和 hydration 风险。
- **模板结构影响 SEO 测试**：保留现有主标题、正文、链接和结构化数据输出，仅调整包裹元素与类名。
- **视觉再次漂移**：在浏览器验收中加入计算字号和 Logo 结构断言，不只检查页面是否可见。

## 9. 完成标准

- 线上所有 SSR 公开页视觉属于同一套 RechargeAi 品牌体系。
- 产品页不再出现 60px 主标题，Logo 不再是纯文本。
- 产品列表与详情的关键层级和交互与原 Vue 页面一致。
- 文章和错误页在保持原语义结构的前提下完成视觉统一。
- 自动化测试、后端打包、跨浏览器测试和生产 smoke test 全部通过。
