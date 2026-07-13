# RechargeAi SEO 与文章管理设计

**日期**：2026-07-13

**状态**：已确认

**适用范围**：`backend`、`frontend-user`、`frontend-admin`、`deploy`

## 1. 背景

RechargeAi 当前采用 Spring Boot 模块化单体、Vue 3 双前端和 Nginx 静态部署。公开产品页由 Vite 单页应用在浏览器中请求 API 后生成内容，初始 HTML 只有统一站点标题，不包含产品正文、页面级描述或结构化数据。

本功能需要完成两项能力：

1. 为产品与文章公开页提供搜索引擎可直接抓取的 HTML 和完整 SEO 信息。
2. 在现有管理后台中提供文章草稿、预览、发布、撤回和编辑能力。

现有 Vue 3 架构不支持 IE11。本功能的浏览器范围限定为主流桌面端与移动端浏览器，不包含 IE11。

## 2. 目标与非目标

### 2.1 目标

- 产品列表、产品详情、文章列表和文章详情由服务端返回完整 HTML。
- 根据页面或文章内容生成标题、描述、规范链接、社交分享标签和结构化数据。
- 管理员可以创建草稿、预览、发布、更新、撤回和删除草稿。
- 发布与撤回立即影响公开访问和站点地图。
- 登录、注册、结算、支付、交易记录和管理后台不参与搜索引擎收录。
- 支持 Chrome、Edge、Firefox、Safari、iOS Safari 和 Android Chrome 的近两个主版本。

### 2.2 非目标

- 不迁移整个用户端到 Nuxt 或新增 Node 运行服务。
- 不支持 IE11。
- 不实现多人审核、定时发布、多作者、文章分类、标签、修订历史或批量导入。
- 不根据爬虫身份返回与用户不同的正文。
- 不引入外部 CMS、搜索服务或 CDN 缓存失效机制。

## 3. 方案决策

采用 Spring Boot 服务端渲染公开产品页与文章页，现有 Vue 应用继续承载账户与交易流程。

选择该方案的原因：

- 与现有 Spring Boot + Nginx 部署方式一致，不增加常驻 Node 服务。
- 公开内容范围只有四类页面，服务端模板边界清晰。
- 文章发布后可立即返回可抓取 HTML，不依赖重新构建前端。
- 不需要识别爬虫或维护两套不同内容。

未采用的方案：

- Nuxt SSR：SEO 完整，但需要迁移用户端并扩展生产部署，改动范围超出本功能。
- SPA Meta + Sitemap：改动小，但初始 HTML 没有正文，无法稳定满足多搜索引擎抓取要求。

## 4. 架构与路由

### 4.1 组件边界

| 组件 | 职责 |
|------|------|
| `backend/content` | 文章实体、仓储、Markdown 转换与过滤、发布状态、管理接口、公开文章查询 |
| `backend/seo` | 页面 SEO 模型、结构化数据、站点地图和 robots 输出 |
| Spring MVC 模板 | 渲染产品列表、产品详情、文章列表、文章详情和公开 404 页面 |
| `frontend-admin` | 文章列表、编辑、Markdown 预览、发布与撤回操作 |
| `frontend-user` | 保留登录、注册、结算、支付和交易记录；公开内容入口使用普通链接 |
| Nginx | 规范域名跳转、SSR 路由转发、SPA 回退、索引控制响应头 |

`content` 模块只依赖通用异常、审计日志和资源上传能力；`seo` 模块通过只读服务访问已发布文章和已上架产品，不反向修改业务状态。

### 4.2 请求路由

| 路径 | 处理方 | 索引规则 |
|------|--------|----------|
| `/` | Nginx `301` 到 `/products` | 不单独收录 |
| `/products` | Spring MVC | `index,follow` |
| `/products/{id}` | Spring MVC | 仅已上架产品可收录 |
| `/articles` | Spring MVC | `index,follow` |
| `/articles/{slug}` | Spring MVC | 仅已发布文章可收录 |
| `/sitemap.xml` | Spring MVC | 公开 |
| `/robots.txt` | Spring MVC | 公开 |
| `/login`、`/register`、`/checkout/**`、`/payment/**`、`/transaction-record` | Vue 用户端 | `noindex,nofollow` |
| `/admin/**` | Vue 管理端 | `noindex,nofollow` |

Nginx 将 SSR 路径转发到后端，其余用户路径继续使用 `/index.html` 回退。`www.rechargeai.cn` 通过 `301` 统一跳转到 `https://rechargeai.cn`。

用户端导航到产品或文章时使用普通 `<a>`，确保进入服务端路由；产品详情的购买按钮链接到 `/checkout/{id}`，随后由 Vue 继续下单流程。现有 Vue 产品页面文件在本功能中不删除，先解除公开路由注册并作为迁移对照，避免丢失当前工作区中的未提交实现。

## 5. 文章数据模型

新增 Flyway 迁移 `V13__content_articles.sql` 和表 `content_article`。

| 字段 | 类型 | 约束与含义 |
|------|------|------------|
| `id` | `BIGINT` | 主键，自增 |
| `title` | `VARCHAR(200)` | 必填文章标题 |
| `slug` | `VARCHAR(180)` | 必填、唯一、公开地址标识 |
| `summary` | `VARCHAR(500)` | 可选；为空时从正文纯文本生成 |
| `cover_image_url` | `VARCHAR(512)` | 可选封面图 |
| `content_markdown` | `LONGTEXT` | Markdown 唯一内容源 |
| `content_html` | `LONGTEXT` | 保存时生成的安全 HTML 缓存 |
| `seo_title` | `VARCHAR(200)` | 可选人工覆盖 |
| `seo_description` | `VARCHAR(320)` | 可选人工覆盖 |
| `status` | `VARCHAR(16)` | `DRAFT` 或 `PUBLISHED` |
| `published_at` | `DATETIME(6)` | 首次发布时间，草稿可为空 |
| `created_at` | `DATETIME(6)` | 创建时间 |
| `updated_at` | `DATETIME(6)` | 最后更新时间 |

索引：

- `UNIQUE(slug)`：保证公开地址唯一。
- `INDEX(status, published_at)`：支持公开文章倒序分页。
- `INDEX(updated_at)`：支持站点地图更新时间查询。

`slug` 允许管理员在首次发布前修改；留空时保存后生成 `article-{id}`。首次发布后不允许直接修改，避免已有链接失效。本期不提供旧地址重定向表。

## 6. 发布状态与管理流程

状态只有 `DRAFT` 和 `PUBLISHED`：

```text
DRAFT --发布--> PUBLISHED --撤回--> DRAFT
```

规则：

- 新文章始终以草稿创建。
- 发布前必须存在非空标题和正文，并保证 `slug` 唯一。
- 首次发布设置 `published_at`；后续撤回再发布保留首次发布时间。
- 已发布文章保存后立即更新公开内容和 `updated_at`。
- 撤回后公开地址立即返回 `404`，后台仍保留内容。
- 只有草稿可以删除；已发布文章必须先撤回。
- 发布、撤回、更新和删除通过现有 `AuditLogService` 记录管理员、文章 ID、操作类型和摘要。

管理端页面：

- `/articles`：分页列表、标题搜索、状态筛选、发布状态与操作入口。
- `/articles/new`：创建草稿。
- `/articles/{id}/edit`：编辑、Markdown 预览、保存、发布或撤回。

Markdown 编辑器支持正文图片上传。新增文章资源接口和独立存储目录，图片仅允许 JPEG、PNG、WebP、GIF，单文件上限 5 MB；不接受 SVG、HTML 或视频文件。

## 7. 接口契约

### 7.1 管理接口

所有接口位于 `/admin/api/articles`，沿用 `ROLE_ADMIN` 鉴权和现有 `ApiResponse`。

| 方法与路径 | 用途 |
|------------|------|
| `GET /admin/api/articles` | 按 `page`、`size`、`title`、`status` 分页查询 |
| `GET /admin/api/articles/{id}` | 获取编辑详情 |
| `POST /admin/api/articles` | 创建草稿 |
| `PUT /admin/api/articles/{id}` | 更新文章内容与 SEO 覆盖字段 |
| `POST /admin/api/articles/preview` | 将未保存的 Markdown 转为安全 HTML，用于准确预览 |
| `POST /admin/api/articles/{id}/publish` | 发布文章 |
| `POST /admin/api/articles/{id}/withdraw` | 撤回文章 |
| `DELETE /admin/api/articles/{id}` | 删除草稿 |
| `POST /admin/api/article-assets` | 上传正文图片或封面图 |

分页默认每页 10 条，最大 50 条。重复 `slug` 返回 `409`；字段校验失败返回 `400`；文章不存在返回 `404`。

### 7.2 公开访问

公开页面由 MVC 控制器直接调用只读服务，不额外暴露文章 JSON API。文章图片通过 `GET /api/article-assets/{storedName}` 读取，并设置准确的 `Content-Type`、`X-Content-Type-Options: nosniff` 和长期缓存头。

## 8. Markdown 与内容安全

`content_markdown` 是唯一可编辑内容。保存时由后端完成：

1. Markdown 转 HTML。
2. 使用允许列表过滤 HTML。
3. 保存过滤后的 `content_html`。
4. 从过滤后的纯文本生成缺省摘要和 SEO 描述。

允许标题、段落、强调、引用、列表、表格、代码、分隔线、链接和图片。过滤以下内容：

- `script`、`style`、`iframe`、`object`、`embed` 和表单元素。
- `on*` 事件属性、内联脚本和危险 CSS。
- `javascript:`、`data:text/html` 等危险协议。
- 未经资源服务校验的本地文件路径。

外部链接补充 `rel="noopener noreferrer"`；图片必须包含可用的替代文本，编辑器在缺少替代文本时提示管理员。

## 9. SEO 规则

### 9.1 页面 Meta

所有可索引页面输出：

- 独立 `<title>` 和 `meta[name=description]`。
- 绝对地址 `link[rel=canonical]`。
- Open Graph 标题、描述、地址、类型和图片。
- `twitter:card=summary_large_image`。
- `html lang="zh-CN"`、主题色和移动端 viewport。

文章 SEO 值优先级：

1. 人工填写的 `seo_title` / `seo_description`。
2. 文章标题 / 摘要。
3. 从过滤后正文截取并规范空白的纯文本。

产品 SEO 描述由产品名称、服务类型、周期、价格和合规说明中的非空字段组合，不输出虚构评分、库存或促销信息。

### 9.2 结构化数据

- 文章详情：`Article` 和 `BreadcrumbList`。
- 产品详情：`Product`、真实 `Offer` 和 `BreadcrumbList`。
- 文章列表与产品列表：`ItemList`。

JSON-LD 只使用页面中真实可见的数据。文章发布者固定为 `RechargeAi`，不虚构独立作者。

### 9.3 Sitemap 与 Robots

`/sitemap.xml` 只包含：

- `/products`、`/articles`。
- 当前已上架产品详情。
- 当前已发布文章详情。

每条详情地址使用对应 `updated_at` 作为 `lastmod`。发布、撤回、更新文章或上下架产品后，使进程内站点地图缓存失效；下一次请求重新生成。

`/robots.txt` 允许公开内容，禁止账户、交易和管理路径，并声明：

```text
Sitemap: https://rechargeai.cn/sitemap.xml
```

用户 SPA 和管理 SPA 的入口 HTML 默认包含 `noindex,nofollow`。Nginx 对这些路径同时添加 `X-Robots-Tag: noindex, nofollow`，作为双重保护。

## 10. 浏览器兼容与表现

浏览器基线：Chrome、Edge、Firefox、Safari、iOS Safari、Android Chrome 的近两个主版本。

- 用户端和管理端 Vite 构建目标统一为 `ES2018`。
- CSS 根据浏览器范围添加必要前缀。
- `oklch` 颜色前提供普通十六进制或 RGB 降级值。
- `backdrop-filter` 等增强效果不可用时使用不透明背景，不影响阅读和操作。
- SSR 公开页使用语义化 HTML，正文、导航和购买入口在 JavaScript 不可用时仍可访问。
- 远程 Google Fonts 改为本地字体或系统字体栈，避免外部字体阻塞。
- 图片声明宽高并延迟加载非首屏资源，首屏封面不延迟加载。

## 11. 错误处理

- 不存在、未发布或不可公开的资源使用服务端中文 404 页面和真实 `404` 状态码。
- 模板渲染异常返回通用中文错误页，日志记录请求路径和异常，不输出文章草稿或敏感配置。
- 管理 API 继续使用统一业务异常格式。
- 图片类型、体积或内容校验失败返回明确中文错误，不保存部分文件。
- 发布事务失败时文章状态保持不变，审计日志不记录成功操作。
- 站点地图单条脏数据不得导致整个 XML 无法输出；记录错误并跳过无效地址。

## 12. 验收与测试

### 12.1 后端测试

- 文章创建、更新、发布、撤回和删除草稿状态测试。
- 首次发布时间保留、发布后 `slug` 锁定和重复 `slug` 冲突测试。
- Markdown 转换、危险 HTML/协议过滤和摘要生成测试。
- 管理接口鉴权、分页过滤和中文错误测试。
- SSR 产品与文章页面的 `200`、`404`、Meta、canonical 和 JSON-LD 测试。
- Sitemap 仅包含已发布文章与已上架产品，并正确输出 `lastmod`。
- Robots 与私有路径 `X-Robots-Tag` 测试。
- 图片格式、大小、路径穿越和响应头测试。

### 12.2 前端与跨浏览器测试

- 管理端完成创建草稿、预览、发布、更新、撤回和删除草稿闭环。
- Chromium、Firefox、WebKit 验证产品列表、产品详情、文章列表、文章详情和购买跳转。
- 验证无 JavaScript 时公开正文和购买链接仍存在。
- 验证移动端导航、长标题、长正文、代码块和图片不会溢出。
- 用户端和管理端分别执行类型检查与生产构建。

### 12.3 部署验收

- 后端完整测试及打包通过。
- Nginx 配置检查通过。
- 使用 `curl` 验证首次 HTML 已包含正文、Meta 和 JSON-LD。
- 验证 `www` 到根域名的 `301`、公开 404、私有页面 `noindex`、robots 和 sitemap。
- 发布测试文章后无需重新构建前端即可公开访问；撤回后立即返回 404。

## 13. 完成标准

以下条件全部满足才视为完成：

- 四类公开页面均由服务端返回完整、可索引 HTML。
- 文章管理发布闭环可用，权限和审计生效。
- 只有已发布文章和已上架产品进入站点地图。
- 私有页面不参与索引，错误页面返回真实状态码。
- 主流浏览器自动化测试、前后端构建和部署路由验证全部通过。
- 实施未覆盖或丢失工作区中已有的无关修改。
