# SSR 公开页视觉一致性实现计划

> **对于代理工作者：** 必需子技能：使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法跟踪。

**目标：** 在保留 Spring MVC SSR 与全部 SEO 能力的前提下，让产品、文章和错误公开页重新使用 Vue 用户端既有的 RechargeAi 品牌视觉。

**架构：** 继续由 Thymeleaf 输出完整公开 HTML，不引入 hydration。以 Vue 用户端设计变量和组件结构为来源，更新共享片段、五类公开模板与独立 `site.css`，通过后端模板/CSS 契约测试、Playwright 计算样式断言和生产 smoke test 阻止再次漂移。

**技术栈：** Java 21、Spring Boot 3.3、Thymeleaf、JUnit 5、MockMvc、AssertJ、CSS、Playwright

---

### 任务 1：建立 SSR 品牌结构与 CSS 回归契约

**文件：**

- 创建：`backend/src/test/java/com/wildai/seo/PublicStyleContractTest.java`
- 修改：`backend/src/test/java/com/wildai/smoke/PublicContentPageSmokeIT.java`

- [ ] **步骤 1：编写 CSS 设计系统失败测试**

创建 `PublicStyleContractTest`，直接读取打包资源并锁定本次回归的关键特征：

```java
package com.wildai.seo;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class PublicStyleContractTest {

    @Test
    void publicStyleReusesUserBrandSystem() throws IOException {
        String css = new ClassPathResource("static/seo/site.css")
                .getContentAsString(StandardCharsets.UTF_8);

        assertThat(css)
                .contains("--font-serif: Georgia")
                .contains(".logo__mark")
                .contains(".product-card__cta")
                .contains(".info-list");
    }

    @Test
    void publicHeadingsUseRestrainedBrandScale() throws IOException {
        String css = new ClassPathResource("static/seo/site.css")
                .getContentAsString(StandardCharsets.UTF_8);

        assertThat(css)
                .contains("--page-title-size: clamp(1.375rem, 1.2rem + 0.5vw, 1.625rem)")
                .contains("--article-title-size: clamp(1.75rem, 1.55rem + 1vw, 2.25rem)")
                .containsPattern("(?s)\\.page-heading h1\\s*\\{[^}]*font-size:\\s*var\\(--page-title-size\\)")
                .containsPattern("(?s)\\.article-heading h1\\s*\\{[^}]*font-size:\\s*var\\(--article-title-size\\)");
    }
}
```

- [ ] **步骤 2：扩展公开 HTML 失败测试**

在 `onlyOnShelfProductsAreRenderedAndRootRedirectsPermanently` 的产品列表断言中增加：

```java
.andExpect(content().string(containsString("class=\"logo__mark\"")))
.andExpect(content().string(containsString("AI 订阅服务")))
.andExpect(content().string(containsString("class=\"product-card__cta\"")))
```

产品详情同时覆盖无可选信息与完整公开信息两种产品：

```java
assertThat(withoutOptionalInformation.selectFirst(".info-card")).isNull();
assertThat(withPublicInformation.selectFirst("section.info-card > dl.info-list")).isNotNull();
assertThat(withPublicInformation.select("dt").eachText()).containsExactly("预计处理", "退款政策");
assertThat(withPublicInformation.selectFirst(".notice[role='note']")).isNotNull();
assertThat(withPublicInformation.selectFirst("a.button.button--block")).isNotNull();
```

在文章详情断言中增加共享品牌断言：

```java
.andExpect(content().string(containsString("class=\"logo__mark\"")))
.andExpect(content().string(containsString("class=\"logo__text\"")))
```

- [ ] **步骤 3：运行测试并确认按预期失败**

运行：

```bash
cd backend
mvn -Dtest=PublicStyleContractTest,PublicContentPageSmokeIT test
```

预期：FAIL；CSS 测试缺少 `--font-serif`/`.logo__mark` 和标题变量绑定，HTML 测试缺少 Logo 标记、原产品标题和产品详情结构。失败不能来自编译错误或测试环境错误。

---

### 任务 2：恢复共享品牌头部与全站设计变量

**文件：**

- 修改：`backend/src/main/resources/templates/public/fragments.html`
- 修改：`backend/src/main/resources/static/seo/site.css`
- 测试：`backend/src/test/java/com/wildai/seo/PublicStyleContractTest.java`
- 测试：`backend/src/test/java/com/wildai/smoke/PublicContentPageSmokeIT.java`

- [ ] **步骤 1：将品牌链接改为原 Logo 结构**

`fragments.html` 中品牌链接固定为：

```html
<a class="logo" href="/products" aria-label="RechargeAi 首页">
  <span class="logo__mark" aria-hidden="true">R</span>
  <span class="logo__text">RechargeAi</span>
</a>
```

桌面导航固定为“服务”“文章”“交易记录”“登录”，分别链接 `/products`、`/articles`、`/transaction-record`、`/login`。移动端使用原生 `<details class="mobile-nav">` 与 `<summary class="menu-toggle" aria-label="主导航菜单">`，内部重复相同导航链接；桌面 `.nav-links` 与移动端 `.mobile-nav` 通过媒体查询互斥显示。

- [ ] **步骤 2：迁移 Vue 设计变量**

将 `frontend-user/src/style.css` 中下列变量完整迁移到 `site.css`，每个 `oklch()` 前保留十六进制降级：

- 主色、hover、muted、subtle；
- 暖色背景、表面、边框、正文与标题色；
- warning 与 neutral 语义色；
- `--font-sans`、`--font-serif`；
- `--space-*`、`--radius-*`、`--shadow-*`、动效、56px 头部与 960px 内容宽度；
- `--page-title-size` 和 `--article-title-size` 两个受测试约束的字号变量。

- [ ] **步骤 3：实现共享基础样式**

基础规则必须包含：

- 根字号 15–16px、1.6 行高、字体平滑；
- `h1/h2/h3` 使用 `--font-serif`、600 字重；
- Logo 尺寸与 Vue `App.vue` 一致；
- 导航链接 36px 桌面高度、44px 移动端高度；
- 所有链接和按钮具备 `:focus-visible` 轮廓；
- Safari `-webkit-backdrop-filter` 降级与 `prefers-reduced-motion`；
- 不添加远程字体或 JavaScript。

- [ ] **步骤 4：运行快速 CSS 测试**

运行：

```bash
cd backend
mvn -Dtest=PublicStyleContractTest test
```

预期：2 个测试 PASS。

---

### 任务 3：对齐产品列表与产品详情

**文件：**

- 修改：`backend/src/main/resources/templates/public/product-list.html`
- 修改：`backend/src/main/resources/templates/public/product-detail.html`
- 修改：`backend/src/main/resources/static/seo/site.css`
- 测试：`backend/src/test/java/com/wildai/smoke/PublicContentPageSmokeIT.java`

- [ ] **步骤 1：重建产品列表语义结构**

页面标题改为“AI 订阅服务”，描述改为“选择您需要的 AI 服务，我们将协助完成订阅流程”。每个产品用整卡链接输出以下结构：

```html
<a class="card product-card" th:href="@{/products/{id}(id=${product.id})}">
  <div class="product-card__top">
    <h2 class="product-card__name" th:text="${product.name}">产品名称</h2>
    <span class="product-card__eta" th:if="${product.estimatedHours != null}">约 1h</span>
  </div>
  <div class="product-card__price">
    <span class="product-card__amount">168.00 CNY</span>
    <span class="product-card__period">/ 30 天</span>
  </div>
  <span class="product-card__cta">查看详情 →</span>
</a>
```

实际文本继续由 Thymeleaf 字段组合，不在模板中写死价格或周期。

- [ ] **步骤 2：重建产品详情层级**

将产品名和价格放入 `.page-header`；信息卡使用 `.info-card > .info-list` 的 `dl/div/dt/dd` 结构，且仅在预计处理或退款政策至少一个字段非空时输出；合规说明放入 `role="note"` 的 `.notice`；购买链接使用 `class="button button--block"` 且保持 `/checkout/{id}`。

- [ ] **步骤 3：实现产品专属样式**

`site.css` 对齐原 Vue 页面：

- 产品网格：单列、480px 两列、768px 三列；
- 卡片最小高度 140px，24px 内边距，10px 圆角，轻阴影；
- 产品名 17px 衬线，价格 22px 衬线墨绿，周期/CTA 14px；
- 详情价格 24px，信息行左右布局，合规提示使用 warning 浅背景；
- 购买按钮 48px 高、全宽，active 只做轻微 scale；
- 移动端信息行允许换行且不横向溢出。

- [ ] **步骤 4：运行公开页面测试**

运行：

```bash
cd backend
mvn -Dtest=PublicStyleContractTest,PublicContentPageSmokeIT test
```

预期：4 个测试 PASS；产品列表仍只显示上架产品，产品详情购买链接与 404 断言保持通过。

- [ ] **步骤 5：提交品牌与产品修复**

```bash
git add backend/src/main/resources/templates/public/fragments.html \
  backend/src/main/resources/templates/public/product-list.html \
  backend/src/main/resources/templates/public/product-detail.html \
  backend/src/main/resources/static/seo/site.css \
  backend/src/test/java/com/wildai/seo/PublicStyleContractTest.java \
  backend/src/test/java/com/wildai/smoke/PublicContentPageSmokeIT.java
git diff --cached --check
git commit -m "fix(ui): 恢复 SSR 产品页品牌视觉"
```

---

### 任务 4：统一文章页与错误页视觉并增加浏览器契约

**文件：**

- 修改：`backend/src/main/resources/templates/public/article-list.html`
- 修改：`backend/src/main/resources/templates/public/article-detail.html`
- 修改：`backend/src/main/resources/templates/public/error.html`（仅增加统一类名，不改变错误语义）
- 修改：`backend/src/main/resources/static/seo/site.css`
- 修改：`backend/src/main/java/com/wildai/content/service/ArticleMarkdownService.java`
- 修改：`backend/src/test/java/com/wildai/content/service/ArticleMarkdownServiceTest.java`
- 修改：`backend/src/test/java/com/wildai/seo/PublicStyleContractTest.java`
- 修改：`backend/src/test/java/com/wildai/smoke/PublicContentPageSmokeIT.java`
- 修改：`backend/src/test/java/com/wildai/smoke/ArticleAdminSmokeIT.java`
- 修改：`frontend-user/e2e/seo-articles.spec.ts`
- 修改：`frontend-user/playwright.config.ts`

- [ ] **步骤 1：限定文章与错误页样式边界**

- 页面列表标题使用 `--page-title-size`；
- 文章详情标题使用 `--article-title-size`，桌面不超过 36px；
- 文章内容宽度维持 760px、1.8 行高；
- 封面图与卡片图保持 16:9、8px 圆角；
- 代码块、图片与表格保持安全溢出；
- 错误卡片复用表面、边框、圆角和按钮，不修改 404/500 状态与 noindex。
- Markdown 正文在安全清洗后将 `h1` 至 `h5` 各降一级，`h6` 保持不变，确保页面级文章标题是详情页唯一的 `h1`；
- `.article-body h1` 保留受控字号与间距，兼容数据库中尚未重新渲染的历史正文，且视觉层级低于页面标题。

- [ ] **步骤 2：增加 Playwright 视觉契约断言**

在现有文章详情访问后增加：

```ts
await expect(page.locator('.logo__mark')).toHaveText('R')
const articleTitleSize = await page.locator('.article-heading h1').evaluate((element) =>
  Number.parseFloat(getComputedStyle(element).fontSize),
)
expect(articleTitleSize).toBeLessThanOrEqual(36)
```

在产品列表访问后增加：

```ts
await expect(page.getByRole('heading', { name: 'AI 订阅服务' })).toBeVisible()
await expect(productCards.first().locator('.product-card__cta')).toHaveText('查看详情 →')
const productTitleSize = await page.locator('.page-heading h1').evaluate((element) =>
  Number.parseFloat(getComputedStyle(element).fontSize),
)
expect(productTitleSize).toBeLessThanOrEqual(28)
```

- [ ] **步骤 3：增加移动端 Chromium 项目**

`playwright.config.ts` 的 `projects` 增加：

```ts
{ name: 'mobile-chromium', use: { ...devices['Pixel 7'] } },
```

现有 Chromium、Firefox、WebKit 项目保持不变。

- [ ] **步骤 4：运行前端类型检查与构建**

```bash
cd frontend-user
npm ci
npm run build
```

预期：TypeScript 与 Vite 构建退出码 0。

- [ ] **步骤 5：在本地完整服务上运行浏览器测试**

先按仓库 quickstart 启动 MySQL、Redis、后端与 Nginx/开发代理，再运行：

```bash
cd frontend-user
PUBLIC_BASE_URL=http://localhost:8080 npm run test:e2e
```

预期：Chromium、Firefox、WebKit、mobile-chromium 共 4 个项目全部 PASS。

- [ ] **步骤 6：提交文章与浏览器验收修复**

```bash
git add backend/src/main/resources/templates/public/article-list.html \
  backend/src/main/resources/templates/public/article-detail.html \
  backend/src/main/resources/templates/public/error.html \
  backend/src/main/resources/static/seo/site.css \
  frontend-user/e2e/seo-articles.spec.ts \
  frontend-user/playwright.config.ts
git diff --cached --check
git commit -m "test(ui): 固化 SSR 公开页视觉契约"
```

---

### 任务 5：全量验证与本地视觉审查

**文件：**

- 不新增生产文件
- 检查：全部已修改文件

- [ ] **步骤 1：运行后端完整验证**

```bash
cd backend
mvn test
mvn -Dtest=PublicStyleContractTest,PublicContentPageSmokeIT test
mvn package -DskipTests
```

预期：三条命令均退出码 0；相关测试 0 失败，JAR 成功生成并包含 `/static/seo/site.css` 与公开模板。

- [ ] **步骤 2：运行两端构建**

```bash
cd frontend-user && npm run build
cd ../frontend-admin && npm ci && npm run build
```

预期：两个 Vite 构建均退出码 0。

- [ ] **步骤 3：执行桌面与移动视觉审查**

使用本地浏览器分别以桌面与手机视口检查 `/products`、一个产品详情、`/articles`、一个文章详情和公开 404。记录计算样式与截图，确认：

- Logo 包含绿色 `R` 标记；
- 产品标题不超过 28px，文章标题不超过 36px；
- 移动菜单可用且页面无横向滚动；
- 产品卡 CTA、详情按钮、文章图片/表格与错误页均可访问；
- SEO head、正文和链接无需 JavaScript 即存在。

- [ ] **步骤 4：检查工作区与提交历史**

```bash
git diff --check
git status --short
git log --oneline --decorate -5
```

预期：无未提交的任务文件，无空白错误，提交只包含设计、SSR 模板/CSS和对应测试。

---

### 任务 6：推送、合并、部署与生产验收

**文件：**

- 使用：`deploy/deploy.sh`
- 使用：`scripts/smoke-test.sh`

- [ ] **步骤 1：推送修复分支**

```bash
git push -u origin fix/ssr-visual-parity
```

- [ ] **步骤 2：快进合并到上线分支并推送**

在主工作树 `/Users/mjy/cc/rechargeai` 执行：

```bash
git fetch origin
git merge --ff-only fix/ssr-visual-parity
git push origin 001-wildai-subscription-platform
```

如果远端上线分支已前进，先停止并审查新提交，不使用强推或自动冲突解决。

- [ ] **步骤 3：执行生产部署**

确认现有 `SSHPASS` 环境变量可用后，在主工作树执行：

```bash
./deploy/deploy.sh
```

预期：后端和两端前端构建完成，部署包上传，生产 backend/nginx 容器重新启动。

- [ ] **步骤 4：运行生产 smoke test**

```bash
BASE_URL=https://rechargeai.cn ./scripts/smoke-test.sh
curl -fsS https://rechargeai.cn/products | rg 'logo__mark|AI 订阅服务|product-card__cta|canonical|application/ld\+json'
curl -fsS https://rechargeai.cn/seo/site.css | rg -- '--font-serif|--page-title-size|\.logo__mark'
```

预期：冒烟测试退出码 0；线上 HTML 与 CSS 同时包含品牌视觉契约和 SEO 标记。

- [ ] **步骤 5：执行生产浏览器复核**

在生产域名复查桌面与手机产品页，读取实际计算样式并截图。只有在主标题上限、Logo、导航、卡片、按钮和 SEO 均符合设计文档后，才报告部署完成。
