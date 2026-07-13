# RechargeAi SEO 与文章管理 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 RechargeAi 增加可被搜索引擎直接抓取的产品/文章公开页，以及完整的后台文章草稿、预览、发布和撤回闭环。

**Architecture:** Spring Boot 新增 `content` 与 `seo` 边界，使用 Thymeleaf 服务端渲染四类公开页面；账户和交易流程继续由现有 Vue SPA 承载。文章 Markdown 在后端统一转换和过滤，Nginx 将公开内容路由到后端并为 SPA 私有页面设置禁止索引响应头。

**Tech Stack:** Java 21、Spring Boot 3.3.5、Spring MVC、Thymeleaf、Spring Data JPA、Flyway、MySQL 8、CommonMark 0.28.0、jsoup 1.22.1、Vue 3、Vite 6、Element Plus、Playwright、Nginx。

---

## 执行门槛

当前工作区存在大量未提交改动，且以下待修改文件已经处于 dirty 状态：

- `backend/pom.xml`
- `backend/src/main/java/com/wildai/common/config/WildAiProperties.java`
- `backend/src/main/java/com/wildai/common/security/SecurityConfig.java`
- `backend/src/main/java/com/wildai/product/service/ProductService.java`
- `backend/src/main/resources/application.yml`
- `frontend-admin/src/App.vue`
- `frontend-admin/src/router/index.ts`
- `frontend-user/src/App.vue`
- `deploy/nginx.conf`

开始 Task 1 前必须先完成以下检查：

- [ ] **Gate 1: 保存当前业务基线**

Run:

```bash
git status --short
git diff --name-only
git diff --check
```

Expected: 明确列出当前修改；不得执行 `git stash`、`git reset` 或覆盖文件。由用户授权建立当前修改的检查点提交，或提供一个已包含这些修改的干净分支。

- [ ] **Gate 2: 在干净 worktree 中执行**

Run after the baseline is committed:

```bash
git worktree add ../rechargeai-seo -b feature/seo-articles
cd ../rechargeai-seo
git status --short
```

Expected: 最后一条命令无输出。每次提交前必须运行 `git diff --cached --name-only`，只允许出现当前任务列出的文件。

## 文件职责映射

### 后端新增

| 文件 | 职责 |
|------|------|
| `backend/src/main/resources/db/migration/V13__content_articles.sql` | 文章表、唯一约束与查询索引 |
| `backend/src/main/java/com/wildai/content/domain/Article.java` | 文章持久化实体 |
| `backend/src/main/java/com/wildai/content/domain/ArticleStatus.java` | `DRAFT` / `PUBLISHED` 状态 |
| `backend/src/main/java/com/wildai/content/repository/ArticleRepository.java` | 后台筛选、公开分页与 sitemap 查询 |
| `backend/src/main/java/com/wildai/content/dto/*.java` | 保存、预览、列表、详情和公开只读模型 |
| `backend/src/main/java/com/wildai/content/service/ArticleMarkdownService.java` | Markdown 转换、安全过滤、纯文本和摘要生成 |
| `backend/src/main/java/com/wildai/content/service/ArticleManagementService.java` | 草稿、发布、撤回、删除、审计 |
| `backend/src/main/java/com/wildai/content/service/ArticlePublicQueryService.java` | 只读公开文章查询 |
| `backend/src/main/java/com/wildai/content/service/ArticleAssetService.java` | 正文/封面图片验证、保存和读取 |
| `backend/src/main/java/com/wildai/content/dto/ArticleAssetResource.java` | 公开图片资源与媒体类型 |
| `backend/src/main/java/com/wildai/admin/web/AdminArticleController.java` | 后台文章 REST API |
| `backend/src/main/java/com/wildai/admin/web/AdminArticleAssetController.java` | 后台文章图片上传 API |
| `backend/src/main/java/com/wildai/content/web/ArticleAssetController.java` | 公开图片读取 API |
| `backend/src/main/java/com/wildai/seo/dto/SeoMetadata.java` | 模板使用的 SEO 元数据 |
| `backend/src/main/java/com/wildai/seo/service/StructuredDataService.java` | 安全 JSON-LD 序列化 |
| `backend/src/main/java/com/wildai/seo/service/SeoMetadataFactory.java` | 产品/文章页面元数据生成 |
| `backend/src/main/java/com/wildai/seo/service/SitemapVersion.java` | 发布变化版本号与缓存失效信号 |
| `backend/src/main/java/com/wildai/seo/service/SitemapService.java` | sitemap XML 生成与缓存 |
| `backend/src/main/java/com/wildai/seo/web/PublicArticlePageController.java` | 文章列表/详情 SSR |
| `backend/src/main/java/com/wildai/seo/web/PublicProductPageController.java` | 产品列表/详情 SSR |
| `backend/src/main/java/com/wildai/seo/web/SeoEndpointController.java` | robots 与 sitemap |
| `backend/src/main/java/com/wildai/seo/web/PublicPageNotFoundException.java` | SSR 页面专用 404 信号 |
| `backend/src/main/java/com/wildai/seo/web/PublicPageExceptionHandler.java` | 中文 HTML 404/500 |
| `backend/src/main/resources/templates/public/*.html` | 公开页和错误页模板 |
| `backend/src/main/resources/static/seo/site.css` | SSR 页面设计与兼容降级 |

### 后端修改

| 文件 | 修改点 |
|------|--------|
| `backend/pom.xml` | Thymeleaf、CommonMark、GFM 表格和 jsoup 依赖 |
| `backend/src/main/java/com/wildai/common/config/WildAiProperties.java` | `article` 上传与 `seo` 根域配置 |
| `backend/src/main/java/com/wildai/common/exception/GlobalExceptionHandler.java` | REST 异常处理只作用于 `@RestController` |
| `backend/src/main/java/com/wildai/common/security/SecurityConfig.java` | 放行 SSR、SEO 和公开图片 GET |
| `backend/src/main/java/com/wildai/product/repository/AiServiceProductRepository.java` | sitemap 查询 |
| `backend/src/main/java/com/wildai/product/service/ProductService.java` | 公开详情只允许上架产品并触发 sitemap 失效 |
| `backend/src/main/resources/application.yml` | 根域、文章上传目录与大小限制 |

### 前端与部署

| 文件 | 职责 |
|------|------|
| `frontend-admin/src/services/articleApi.ts` | 文章管理 API 类型与调用 |
| `frontend-admin/src/views/ArticleListView.vue` | 搜索、筛选和状态操作 |
| `frontend-admin/src/views/ArticleEditView.vue` | 草稿编辑、准确预览与发布 |
| `frontend-admin/src/components/MarkdownEditor.vue` | 可注入文章上传和服务端预览能力 |
| `frontend-admin/src/router/index.ts`、`frontend-admin/src/App.vue` | 文章路由与导航 |
| `frontend-user/src/router/index.ts`、`frontend-user/src/App.vue` | 公开内容使用整页链接，保留交易 SPA |
| 两端 `index.html`、`vite.config.ts`、`style.css` | noindex、字体移除、ES2018 和 CSS 降级 |
| `deploy/nginx.conf` | SSR 转发、规范域名、真实 404 与 noindex |
| `deploy/docker-compose.prod.yml`、`deploy/deploy.sh`、`deploy/.env.example` | 图片卷和 SEO 环境变量 |
| `frontend-user/playwright.config.ts`、`frontend-user/e2e/seo-articles.spec.ts` | Chromium/Firefox/WebKit 验收 |

---

### Task 1: 建立文章表与持久化模型

**Files:**
- Create: `backend/src/test/java/com/wildai/smoke/ArticleRepositorySmokeIT.java`
- Create: `backend/src/main/resources/db/migration/V13__content_articles.sql`
- Create: `backend/src/main/java/com/wildai/content/domain/ArticleStatus.java`
- Create: `backend/src/main/java/com/wildai/content/domain/Article.java`
- Create: `backend/src/main/java/com/wildai/content/repository/ArticleRepository.java`
- Modify: `backend/pom.xml`

- [ ] **Step 1: 写失败的数据库映射测试**

```java
package com.wildai.smoke;

import com.wildai.content.domain.Article;
import com.wildai.content.domain.ArticleStatus;
import com.wildai.content.repository.ArticleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class ArticleRepositorySmokeIT extends BaseSmokeIT {
    @Autowired ArticleRepository articleRepository;

    @Test
    void savesAndFindsArticleBySlug() {
        Article article = new Article();
        article.setTitle("SEO 入门");
        article.setSlug("seo-intro");
        article.setSummary("SEO 入门摘要");
        article.setContentMarkdown("# SEO 入门");
        article.setContentHtml("<h1>SEO 入门</h1>");
        article.setStatus(ArticleStatus.DRAFT);

        Article saved = articleRepository.saveAndFlush(article);

        assertThat(articleRepository.findBySlug("seo-intro"))
                .contains(saved);
    }
}
```

- [ ] **Step 2: 运行测试并确认失败**

Run:

```bash
cd backend
mvn -Dtest=ArticleRepositorySmokeIT test
```

Expected: FAIL，编译器提示 `com.wildai.content` 类型不存在。

- [ ] **Step 3: 添加渲染与过滤依赖**

在 `backend/pom.xml` 的 properties 中加入：

```xml
<commonmark.version>0.28.0</commonmark.version>
<jsoup.version>1.22.1</jsoup.version>
```

在 dependencies 中加入：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-thymeleaf</artifactId>
</dependency>
<dependency>
    <groupId>org.commonmark</groupId>
    <artifactId>commonmark</artifactId>
    <version>${commonmark.version}</version>
</dependency>
<dependency>
    <groupId>org.commonmark</groupId>
    <artifactId>commonmark-ext-gfm-tables</artifactId>
    <version>${commonmark.version}</version>
</dependency>
<dependency>
    <groupId>org.jsoup</groupId>
    <artifactId>jsoup</artifactId>
    <version>${jsoup.version}</version>
</dependency>
```

- [ ] **Step 4: 创建迁移、状态枚举和实体**

`V13__content_articles.sql` 使用以下完整表定义：

```sql
CREATE TABLE content_article (
    id BIGINT NOT NULL AUTO_INCREMENT,
    title VARCHAR(200) NOT NULL,
    slug VARCHAR(180) NOT NULL,
    summary VARCHAR(500) NULL,
    cover_image_url VARCHAR(512) NULL,
    content_markdown LONGTEXT NOT NULL,
    content_html LONGTEXT NOT NULL,
    seo_title VARCHAR(200) NULL,
    seo_description VARCHAR(320) NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
    published_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_content_article_slug (slug),
    KEY idx_content_article_status_published (status, published_at),
    KEY idx_content_article_updated_at (updated_at),
    CONSTRAINT chk_content_article_status CHECK (status IN ('DRAFT', 'PUBLISHED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

`ArticleStatus.java`：

```java
package com.wildai.content.domain;

public enum ArticleStatus {
    DRAFT,
    PUBLISHED
}
```

`Article.java` 使用 `@Entity`、`@Table(name = "content_article")`，字段与迁移逐一对应；`status` 使用 `@Enumerated(EnumType.STRING)`，`createdAt` / `updatedAt` 默认 `Instant.now()`，`publishedAt` 可空。提供与现有实体风格一致的 getter/setter。

- [ ] **Step 5: 创建仓储接口**

```java
package com.wildai.content.repository;

import com.wildai.content.domain.Article;
import com.wildai.content.domain.ArticleStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ArticleRepository extends JpaRepository<Article, Long> {
    Optional<Article> findBySlug(String slug);
    Optional<Article> findBySlugAndStatus(String slug, ArticleStatus status);
    boolean existsBySlugAndIdNot(String slug, Long id);
    Page<Article> findByStatusOrderByPublishedAtDesc(ArticleStatus status, Pageable pageable);
    List<Article> findByStatusOrderByUpdatedAtDesc(ArticleStatus status);

    @Query("""
            SELECT a FROM Article a
            WHERE (:title IS NULL OR LOWER(a.title) LIKE LOWER(CONCAT('%', :title, '%')))
              AND (:status IS NULL OR a.status = :status)
            ORDER BY a.updatedAt DESC
            """)
    Page<Article> searchAdmin(@Param("title") String title,
                              @Param("status") ArticleStatus status,
                              Pageable pageable);
}
```

- [ ] **Step 6: 运行迁移测试**

Run:

```bash
mvn -Dtest=ArticleRepositorySmokeIT test
```

Expected: PASS；日志中 Flyway 成功应用 `V13__content_articles.sql`。

- [ ] **Step 7: 提交持久化基础**

```bash
git add backend/pom.xml \
  backend/src/main/resources/db/migration/V13__content_articles.sql \
  backend/src/main/java/com/wildai/content/domain \
  backend/src/main/java/com/wildai/content/repository \
  backend/src/test/java/com/wildai/smoke/ArticleRepositorySmokeIT.java
git diff --cached --check
git commit -m "feat(content): 建立文章持久化模型"
```

### Task 2: 实现统一 Markdown 渲染与安全过滤

**Files:**
- Create: `backend/src/test/java/com/wildai/content/service/ArticleMarkdownServiceTest.java`
- Create: `backend/src/main/java/com/wildai/content/dto/RenderedArticleContent.java`
- Create: `backend/src/main/java/com/wildai/content/service/ArticleMarkdownService.java`

- [ ] **Step 1: 写危险内容与摘要测试**

```java
package com.wildai.content.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ArticleMarkdownServiceTest {
    private final ArticleMarkdownService service = new ArticleMarkdownService();

    @Test
    void rendersTablesAndRemovesExecutableContent() {
        var rendered = service.render("""
                # 标题
                | 名称 | 价格 |
                | --- | --- |
                | Plus | 20 |
                <script>alert(1)</script>
                [危险](javascript:alert(1))
                [官网](https://rechargeai.cn)
                """, "");

        assertThat(rendered.html()).contains("<table>", "<h1>标题</h1>");
        assertThat(rendered.html()).doesNotContain("script", "javascript:");
        assertThat(rendered.html()).contains("rel=\"noopener noreferrer\"");
        assertThat(rendered.summary()).startsWith("标题 名称 价格");
    }

    @Test
    void keepsManualSummaryWithinDatabaseLimit() {
        String summary = " 摘要 ".repeat(200);
        var rendered = service.render("正文", summary);
        assertThat(rendered.summary()).hasSizeLessThanOrEqualTo(500);
    }
}
```

- [ ] **Step 2: 运行测试并确认失败**

Run: `cd backend && mvn -Dtest=ArticleMarkdownServiceTest test`

Expected: FAIL，提示 `ArticleMarkdownService` 不存在。

- [ ] **Step 3: 实现渲染结果与服务**

`RenderedArticleContent.java`：

```java
package com.wildai.content.dto;

public record RenderedArticleContent(String html, String plainText, String summary) {}
```

`ArticleMarkdownService` 固定使用同一套解析器和 safelist：

```java
package com.wildai.content.service;

import com.wildai.content.dto.RenderedArticleContent;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ArticleMarkdownService {
    private static final int GENERATED_SUMMARY_LIMIT = 220;
    private final Parser parser = Parser.builder()
            .extensions(List.of(TablesExtension.create()))
            .build();
    private final HtmlRenderer renderer = HtmlRenderer.builder()
            .extensions(List.of(TablesExtension.create()))
            .build();
    private final Safelist safelist = Safelist.relaxed()
            .addTags("table", "thead", "tbody", "tr", "th", "td")
            .addAttributes("a", "rel")
            .addProtocols("a", "href", "http", "https", "mailto")
            .addProtocols("img", "src", "http", "https")
            .preserveRelativeLinks(true);

    public RenderedArticleContent render(String markdown, String manualSummary) {
        String source = markdown == null ? "" : markdown;
        String unsafeHtml = renderer.render(parser.parse(source));
        String cleaned = Jsoup.clean(unsafeHtml, "", safelist,
                new Document.OutputSettings().prettyPrint(false));
        Document document = Jsoup.parseBodyFragment(cleaned);
        document.select("a[href]").forEach(link -> link.attr("rel", "noopener noreferrer"));
        String html = document.body().html();
        String plainText = document.text().replaceAll("\\s+", " ").trim();
        String summary = manualSummary == null || manualSummary.isBlank()
                ? clip(plainText, GENERATED_SUMMARY_LIMIT)
                : clip(manualSummary.trim().replaceAll("\\s+", " "), 500);
        return new RenderedArticleContent(html, plainText, summary);
    }

    public String preview(String markdown) {
        return render(markdown, "").html();
    }

    private static String clip(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max).stripTrailing();
    }
}
```

- [ ] **Step 4: 运行测试并确认通过**

Run: `mvn -Dtest=ArticleMarkdownServiceTest test`

Expected: PASS，两个测试均通过。

- [ ] **Step 5: 提交 Markdown 安全边界**

```bash
git add backend/src/main/java/com/wildai/content/dto/RenderedArticleContent.java \
  backend/src/main/java/com/wildai/content/service/ArticleMarkdownService.java \
  backend/src/test/java/com/wildai/content/service/ArticleMarkdownServiceTest.java
git diff --cached --check
git commit -m "feat(content): 安全渲染文章 Markdown"
```

### Task 3: 实现文章草稿与发布领域服务

**Files:**
- Create: `backend/src/main/java/com/wildai/content/dto/ArticleSaveRequest.java`
- Create: `backend/src/main/java/com/wildai/content/dto/ArticleSummaryDto.java`
- Create: `backend/src/main/java/com/wildai/content/dto/ArticleDetailDto.java`
- Create: `backend/src/main/java/com/wildai/content/dto/ArticlePublicDto.java`
- Create: `backend/src/main/java/com/wildai/seo/service/SitemapVersion.java`
- Create: `backend/src/main/java/com/wildai/content/service/ArticleManagementService.java`
- Create: `backend/src/main/java/com/wildai/content/service/ArticlePublicQueryService.java`
- Create: `backend/src/test/java/com/wildai/content/service/ArticleManagementServiceTest.java`
- Modify: `backend/src/main/java/com/wildai/content/repository/ArticleRepository.java`

- [ ] **Step 1: 写发布状态与 slug 锁定测试**

测试至少覆盖以下断言，仓储、审计和 sitemap 版本使用 Mockito：

```java
private final ArticleRepository repository = mock(ArticleRepository.class);
private final AuditLogService auditLogService = mock(AuditLogService.class);
private final SitemapVersion sitemapVersion = mock(SitemapVersion.class);
private final ArticleManagementService service = new ArticleManagementService(
        repository, new ArticleMarkdownService(), auditLogService, sitemapVersion);

@Test
void publishSetsFirstPublishedAtAndInvalidatesSitemap() {
    Article draft = article(7L, "seo-guide", ArticleStatus.DRAFT, null, "正文");
    when(repository.findById(7L)).thenReturn(Optional.of(draft));

    ArticleDetailDto result = service.publish(9L, 7L);

    assertThat(result.status()).isEqualTo(ArticleStatus.PUBLISHED);
    assertThat(result.publishedAt()).isNotNull();
    verify(sitemapVersion).invalidate();
    verify(auditLogService).log(eq(9L), eq("ARTICLE_PUBLISH"), eq("ARTICLE"),
            eq("7"), any(), any(), eq("发布文章"));
}

@Test
void cannotChangeSlugAfterFirstPublish() {
    Article withdrawn = article(7L, "stable-url", ArticleStatus.DRAFT, Instant.now(), "正文");
    when(repository.findById(7L)).thenReturn(Optional.of(withdrawn));

    assertThatThrownBy(() -> service.update(9L, 7L,
            new ArticleSaveRequest("标题", "changed-url", "", null, "正文", null, null)))
            .isInstanceOf(BusinessException.class)
            .hasMessage("文章发布后不能修改链接标识");
}

@Test
void publishedArticleMustBeWithdrawnBeforeDelete() {
    Article published = article(7L, "seo-guide", ArticleStatus.PUBLISHED, Instant.now(), "正文");
    when(repository.findById(7L)).thenReturn(Optional.of(published));

    assertThatThrownBy(() -> service.deleteDraft(9L, 7L))
            .isInstanceOf(BusinessException.class)
            .hasMessage("已发布文章请先撤回");
}

private static Article article(Long id, String slug, ArticleStatus status,
                               Instant publishedAt, String markdown) {
    Article article = new Article();
    article.setId(id);
    article.setTitle("测试文章");
    article.setSlug(slug);
    article.setSummary("测试摘要");
    article.setContentMarkdown(markdown);
    article.setContentHtml("<p>" + markdown + "</p>");
    article.setStatus(status);
    article.setPublishedAt(publishedAt);
    return article;
}
```

- [ ] **Step 2: 运行测试并确认失败**

Run: `cd backend && mvn -Dtest=ArticleManagementServiceTest test`

Expected: FAIL，缺少 DTO、`SitemapVersion` 和领域服务。

- [ ] **Step 3: 定义 DTO 契约**

使用以下签名，后续前后端字段名必须保持一致：

```java
public record ArticleSaveRequest(
        @jakarta.validation.constraints.NotBlank(message = "请填写文章标题")
        @jakarta.validation.constraints.Size(max = 200, message = "文章标题不能超过 200 个字符")
        String title,
        @jakarta.validation.constraints.Size(max = 180, message = "链接标识不能超过 180 个字符")
        String slug,
        @jakarta.validation.constraints.Size(max = 500, message = "摘要不能超过 500 个字符")
        String summary,
        @jakarta.validation.constraints.Size(max = 512, message = "封面地址不能超过 512 个字符")
        String coverImageUrl,
        String contentMarkdown,
        @jakarta.validation.constraints.Size(max = 200, message = "SEO 标题不能超过 200 个字符")
        String seoTitle,
        @jakarta.validation.constraints.Size(max = 320, message = "SEO 描述不能超过 320 个字符")
        String seoDescription) {}
```

```java
public record ArticleSummaryDto(Long id, String title, String slug, String summary,
        String coverImageUrl, ArticleStatus status, Instant publishedAt,
        Instant createdAt, Instant updatedAt) {}

public record ArticleDetailDto(Long id, String title, String slug, String summary,
        String coverImageUrl, String contentMarkdown, String contentHtml,
        String seoTitle, String seoDescription, ArticleStatus status,
        Instant publishedAt, Instant createdAt, Instant updatedAt) {}

public record ArticlePublicDto(Long id, String title, String slug, String summary,
        String coverImageUrl, String contentHtml, String seoTitle,
        String seoDescription, Instant publishedAt, Instant updatedAt) {}
```

- [ ] **Step 4: 实现 sitemap 版本信号**

```java
package com.wildai.seo.service;

import org.springframework.stereotype.Component;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class SitemapVersion {
    private final AtomicLong value = new AtomicLong();
    public long current() { return value.get(); }
    public void invalidate() { value.incrementAndGet(); }
}
```

- [ ] **Step 5: 实现领域服务规则**

`ArticleManagementService` 的公开签名固定为：

```java
public PageResult<ArticleSummaryDto> search(String title, ArticleStatus status, int pageNo, int pageSize);
public ArticleDetailDto get(Long id);
public ArticleDetailDto create(Long operatorId, ArticleSaveRequest request);
public ArticleDetailDto update(Long operatorId, Long id, ArticleSaveRequest request);
public ArticleDetailDto publish(Long operatorId, Long id);
public ArticleDetailDto withdraw(Long operatorId, Long id);
public void deleteDraft(Long operatorId, Long id);
public String preview(String markdown);
```

实现时必须包含以下具体规则：

```java
private static final Pattern SLUG = Pattern.compile("[a-z0-9]+(?:-[a-z0-9]+)*");

private String normalizeSlug(String slug) {
    String value = slug == null ? "" : slug.trim().toLowerCase(Locale.ROOT);
    if (!value.isEmpty() && !SLUG.matcher(value).matches()) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "链接标识只能包含小写字母、数字和连字符");
    }
    return value;
}
```

- 创建时若未填写 slug，先保存唯一临时值 `draft-{32位UUID}` 获取自增 ID，再在同一事务中更新为 `article-{id}`。
- 更新时重新调用 `ArticleMarkdownService.render`，同时更新 `contentHtml`、生成后的 `summary` 和 `updatedAt`。
- `publishedAt != null` 时拒绝任何 slug 变化，即使文章已撤回。
- 发布前再次检查标题和 `contentMarkdown` 非空；首次发布设置 `publishedAt`。
- 发布、已发布文章更新、撤回和删除后调用 `sitemapVersion.invalidate()`。
- 捕获唯一键竞争导致的 `DataIntegrityViolationException`，转换为 `BusinessException(ErrorCode.CONFLICT, "链接标识已存在")`。
- 审计日志只写标题、slug 和状态，不写完整正文。
- 分页参数限制为 `pageNo >= 1`、`1 <= pageSize <= 50`。

`ArticlePublicQueryService` 只暴露：

```java
public PageResult<ArticlePublicDto> listPublished(int pageNo, int pageSize);
public Optional<ArticlePublicDto> findPublished(String slug);
```

它必须使用 `findByStatusOrderByPublishedAtDesc` 和 `findBySlugAndStatus`，不得复用后台 `findById` 后再判断。

- [ ] **Step 6: 运行领域测试**

Run: `mvn -Dtest=ArticleManagementServiceTest test`

Expected: PASS；发布、slug 锁定、撤回、删除和审计测试全部通过。

- [ ] **Step 7: 提交文章领域服务**

```bash
git add backend/src/main/java/com/wildai/content \
  backend/src/main/java/com/wildai/seo/service/SitemapVersion.java \
  backend/src/test/java/com/wildai/content/service/ArticleManagementServiceTest.java
git diff --cached --check
git commit -m "feat(content): 实现文章发布状态流转"
```

### Task 4: 暴露后台文章管理接口

**Files:**
- Create: `backend/src/main/java/com/wildai/content/dto/ArticlePreviewRequest.java`
- Create: `backend/src/main/java/com/wildai/content/dto/ArticlePreviewDto.java`
- Create: `backend/src/main/java/com/wildai/admin/web/AdminArticleController.java`
- Create: `backend/src/test/java/com/wildai/smoke/ArticleAdminSmokeIT.java`

- [ ] **Step 1: 写完整管理闭环测试**

```java
package com.wildai.smoke;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ArticleAdminSmokeIT extends BaseSmokeIT {
    @Test
    void adminCanCreatePreviewPublishWithdrawAndDeleteArticle() throws Exception {
        String token = adminLogin();
        String body = """
                {"title":"SEO 指南","slug":"seo-guide","summary":"",
                 "contentMarkdown":"# SEO 指南\\n正文","seoTitle":"","seoDescription":""}
                """;

        var created = mockMvc.perform(post("/admin/api/articles")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andReturn();
        long id = objectMapper.readTree(created.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        mockMvc.perform(post("/admin/api/articles/preview")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contentMarkdown\":\"# 预览<script>alert(1)</script>\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<h1>预览</h1>")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("<script>"))));

        mockMvc.perform(post("/admin/api/articles/" + id + "/publish")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"));

        mockMvc.perform(post("/admin/api/articles/" + id + "/withdraw")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DRAFT"));

        mockMvc.perform(delete("/admin/api/articles/" + id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }
}
```

- [ ] **Step 2: 运行测试并确认失败**

Run: `cd backend && mvn -Dtest=ArticleAdminSmokeIT test`

Expected: FAIL，创建接口返回 404。

- [ ] **Step 3: 实现预览 DTO 和控制器**

```java
public record ArticlePreviewRequest(String contentMarkdown) {}
public record ArticlePreviewDto(String html) {}
```

`AdminArticleController` 映射必须与设计一致：

```java
@RestController
@RequestMapping("/admin/api/articles")
public class AdminArticleController {
    private final ArticleManagementService service;

    public AdminArticleController(ArticleManagementService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<PageResult<ArticleSummaryDto>> list(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) ArticleStatus status,
            @RequestParam(defaultValue = "1") int pageNo,
            @RequestParam(defaultValue = "10") int pageSize) {
        return ApiResponse.ok(service.search(title, status, pageNo, pageSize));
    }

    @GetMapping("/{id}")
    public ApiResponse<ArticleDetailDto> get(@PathVariable Long id) {
        return ApiResponse.ok(service.get(id));
    }

    @PostMapping
    public ApiResponse<ArticleDetailDto> create(@AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ArticleSaveRequest request) {
        return ApiResponse.ok(service.create(principal.id(), request));
    }

    @PutMapping("/{id}")
    public ApiResponse<ArticleDetailDto> update(@AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id, @Valid @RequestBody ArticleSaveRequest request) {
        return ApiResponse.ok(service.update(principal.id(), id, request));
    }

    @PostMapping("/preview")
    public ApiResponse<ArticlePreviewDto> preview(@RequestBody ArticlePreviewRequest request) {
        return ApiResponse.ok(new ArticlePreviewDto(service.preview(request.contentMarkdown())));
    }

    @PostMapping("/{id}/publish")
    public ApiResponse<ArticleDetailDto> publish(@AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        return ApiResponse.ok(service.publish(principal.id(), id));
    }

    @PostMapping("/{id}/withdraw")
    public ApiResponse<ArticleDetailDto> withdraw(@AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        return ApiResponse.ok(service.withdraw(principal.id(), id));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        service.deleteDraft(principal.id(), id);
        return ApiResponse.ok(null);
    }
}
```

- [ ] **Step 4: 运行后台接口测试**

Run: `mvn -Dtest=ArticleAdminSmokeIT test`

Expected: PASS；未携带 token 的同类请求返回 401。

- [ ] **Step 5: 提交后台接口**

```bash
git add backend/src/main/java/com/wildai/content/dto \
  backend/src/main/java/com/wildai/admin/web/AdminArticleController.java \
  backend/src/test/java/com/wildai/smoke/ArticleAdminSmokeIT.java
git diff --cached --check
git commit -m "feat(admin): 增加文章管理接口"
```

### Task 5: 实现文章图片安全上传

**Files:**
- Create: `backend/src/main/java/com/wildai/content/dto/ArticleAssetUploadDto.java`
- Create: `backend/src/main/java/com/wildai/content/dto/ArticleAssetResource.java`
- Create: `backend/src/main/java/com/wildai/content/service/ArticleAssetService.java`
- Create: `backend/src/main/java/com/wildai/admin/web/AdminArticleAssetController.java`
- Create: `backend/src/main/java/com/wildai/content/web/ArticleAssetController.java`
- Create: `backend/src/test/java/com/wildai/content/service/ArticleAssetServiceTest.java`
- Modify: `backend/src/main/java/com/wildai/common/config/WildAiProperties.java`
- Modify: `backend/src/main/resources/application.yml`
- Modify: `backend/src/main/java/com/wildai/common/security/SecurityConfig.java`

- [ ] **Step 1: 写文件签名、大小和路径测试**

使用 `@TempDir Path tempDir` 和 `MockMultipartFile` 覆盖：

```java
@TempDir Path tempDir;
private ArticleAssetService service;

@BeforeEach
void setUp() throws IOException {
    WildAiProperties properties = new WildAiProperties();
    properties.getArticle().setUploadDir(tempDir.toString());
    service = new ArticleAssetService(properties);
    service.ensureUploadDir();
}

@Test
void acceptsPngBySignatureAndRejectsSpoofedHtml() {
    byte[] png = new byte[] {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 0, 0, 0, 1};
    var accepted = service.upload(new MockMultipartFile("file", "cover.png", "image/png", png));
    assertThat(accepted.url()).startsWith("/api/article-assets/");

    var fake = new MockMultipartFile("file", "x.png", "image/png", "<script>x</script>".getBytes());
    assertThatThrownBy(() -> service.upload(fake))
            .isInstanceOf(BusinessException.class)
            .hasMessage("图片内容与格式不匹配");
}

@Test
void rejectsTraversalOnRead() {
    assertThatThrownBy(() -> service.load("../secret"))
            .isInstanceOf(BusinessException.class)
            .hasMessage("资源不存在");
}
```

- [ ] **Step 2: 运行测试并确认失败**

Run: `cd backend && mvn -Dtest=ArticleAssetServiceTest test`

Expected: FAIL，缺少 `ArticleAssetService`。

- [ ] **Step 3: 增加配置模型**

在 `WildAiProperties` 增加 `Article article = new Article()`、getter/setter 以及：

```java
public static class Article {
    private String uploadDir = "./uploads/article";
    private long maxImageBytes = 5 * 1024 * 1024;
    public String getUploadDir() { return uploadDir; }
    public void setUploadDir(String uploadDir) { this.uploadDir = uploadDir; }
    public long getMaxImageBytes() { return maxImageBytes; }
    public void setMaxImageBytes(long maxImageBytes) { this.maxImageBytes = maxImageBytes; }
}
```

`application.yml` 增加：

```yaml
  article:
    upload-dir: ${WILDAI_ARTICLE_UPLOAD_DIR:./uploads/article}
    max-image-bytes: ${WILDAI_ARTICLE_MAX_IMAGE_BYTES:5242880}
```

- [ ] **Step 4: 实现图片服务与控制器**

`ArticleAssetService` 读取文件前 12 字节识别 JPEG、PNG、GIF、WebP；不得只相信扩展名或 `Content-Type`。保存名使用无连字符 UUID 和检测出的扩展名，返回：

```java
public record ArticleAssetUploadDto(String url, String storedName, String contentType) {}

public record ArticleAssetResource(
        org.springframework.core.io.Resource resource,
        org.springframework.http.MediaType contentType) {}
```

后台上传接口：

```java
@RestController
@RequestMapping("/admin/api/article-assets")
public class AdminArticleAssetController {
    private final ArticleAssetService service;
    public AdminArticleAssetController(ArticleAssetService service) { this.service = service; }
    @PostMapping
    public ApiResponse<ArticleAssetUploadDto> upload(@RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(service.upload(file));
    }
}
```

`ArticleAssetService.load(String storedName)` 返回 `ArticleAssetResource`。公开读取接口返回 `Cache-Control: public,max-age=2592000`、`X-Content-Type-Options: nosniff` 和准确图片类型：

```java
ArticleAssetResource asset = service.load(storedName);
return ResponseEntity.ok()
        .contentType(asset.contentType())
        .cacheControl(CacheControl.maxAge(30, TimeUnit.DAYS).cachePublic())
        .header("X-Content-Type-Options", "nosniff")
        .body(asset.resource());
```

在 `SecurityConfig` 的 `/api/**` 用户鉴权规则之前放行：

```java
.requestMatchers(HttpMethod.GET, "/api/article-assets/**").permitAll()
```

- [ ] **Step 5: 运行图片服务测试和安全测试**

Run:

```bash
mvn -Dtest=ArticleAssetServiceTest,ArticleAdminSmokeIT test
```

Expected: PASS；SVG、HTML、超限图片和路径穿越均被拒绝。

- [ ] **Step 6: 提交图片上传能力**

```bash
git add backend/src/main/java/com/wildai/content \
  backend/src/main/java/com/wildai/admin/web/AdminArticleAssetController.java \
  backend/src/main/java/com/wildai/common/config/WildAiProperties.java \
  backend/src/main/java/com/wildai/common/security/SecurityConfig.java \
  backend/src/main/resources/application.yml \
  backend/src/test/java/com/wildai/content/service/ArticleAssetServiceTest.java
git diff --cached --check
git commit -m "feat(content): 增加文章图片安全上传"
```

### Task 6: 生成页面 SEO 元数据与安全 JSON-LD

**Files:**
- Create: `backend/src/main/java/com/wildai/seo/dto/SeoMetadata.java`
- Create: `backend/src/main/java/com/wildai/seo/service/StructuredDataService.java`
- Create: `backend/src/main/java/com/wildai/seo/service/SeoMetadataFactory.java`
- Create: `backend/src/test/java/com/wildai/seo/service/SeoMetadataFactoryTest.java`
- Modify: `backend/src/main/java/com/wildai/common/config/WildAiProperties.java`
- Modify: `backend/src/main/resources/application.yml`

- [ ] **Step 1: 写元数据优先级和脚本闭合防护测试**

```java
private final WildAiProperties properties = new WildAiProperties();
private final StructuredDataService structuredData =
        new StructuredDataService(new ObjectMapper());
private final SeoMetadataFactory factory;

SeoMetadataFactoryTest() {
    properties.getSeo().setBaseUrl("https://rechargeai.cn");
    factory = new SeoMetadataFactory(properties, structuredData);
}

@Test
void articleUsesOverridesAndEscapesJsonLdScriptBoundary() {
    ArticlePublicDto article = new ArticlePublicDto(1L, "标题</script>", "safe-slug",
            "自动摘要", null, "<p>正文</p>", "人工 SEO 标题", "人工 SEO 描述",
            Instant.parse("2026-07-13T00:00:00Z"), Instant.parse("2026-07-13T01:00:00Z"));

    SeoMetadata seo = factory.forArticle(article);

    assertThat(seo.title()).isEqualTo("人工 SEO 标题 | RechargeAi");
    assertThat(seo.description()).isEqualTo("人工 SEO 描述");
    assertThat(seo.canonical()).isEqualTo("https://rechargeai.cn/articles/safe-slug");
    assertThat(seo.jsonLd()).doesNotContain("</script>").contains("\\u003c/script\\u003e");
}
```

- [ ] **Step 2: 运行测试并确认失败**

Run: `cd backend && mvn -Dtest=SeoMetadataFactoryTest test`

Expected: FAIL，缺少 SEO 类型。

- [ ] **Step 3: 增加 SEO 配置**

`WildAiProperties` 增加：

```java
public static class Seo {
    private String baseUrl = "https://rechargeai.cn";
    private String siteName = "RechargeAi";
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public String getSiteName() { return siteName; }
    public void setSiteName(String siteName) { this.siteName = siteName; }
}
```

`application.yml` 增加：

```yaml
  seo:
    base-url: ${WILDAI_SEO_BASE_URL:https://rechargeai.cn}
    site-name: RechargeAi
```

- [ ] **Step 4: 实现 SEO 模型和结构化数据**

```java
public record SeoMetadata(String title, String description, String canonical,
        String type, String imageUrl, String robots, String jsonLd) {}
```

`StructuredDataService.toSafeJson(Object value)` 使用现有 `ObjectMapper` 序列化，然后按顺序替换：

```java
return json.replace("&", "\\u0026")
        .replace("<", "\\u003c")
        .replace(">", "\\u003e");
```

`SeoMetadataFactory` 提供 `forArticle`、`forProduct`、`forArticleList`、`forProductList`。文章描述优先 `seoDescription`，再用 `summary`；统一压缩空白并截断到 160 字。产品 JSON-LD 只能写真实 `name`、`price`、`priceCurrency`、`availability` 和页面 URL，不生成评分字段。

文章或产品图片为相对路径时，必须以 `seo.base-url` 转为绝对 URL 后再写入 Open Graph 和 JSON-LD；缺少图片时不输出虚构图片地址。

详情页 JSON-LD 使用 `@graph`：文章页包含 `Article` 与 `BreadcrumbList`，产品页包含 `Product` 与 `BreadcrumbList`；列表页输出 `ItemList`。文章 `publisher.name` 固定为 `RechargeAi`，不生成虚构作者。为这些类型分别增加断言：

```java
assertThat(factory.forArticle(article).jsonLd())
        .contains("\"@type\":\"Article\"", "\"@type\":\"BreadcrumbList\"");
assertThat(factory.forArticleList(List.of(article)).jsonLd())
        .contains("\"@type\":\"ItemList\"");
```

- [ ] **Step 5: 运行 SEO 单元测试**

Run: `mvn -Dtest=SeoMetadataFactoryTest test`

Expected: PASS；JSON-LD 中不存在原始 `</script>`。

- [ ] **Step 6: 提交 SEO 生成器**

```bash
git add backend/src/main/java/com/wildai/seo \
  backend/src/main/java/com/wildai/common/config/WildAiProperties.java \
  backend/src/main/resources/application.yml \
  backend/src/test/java/com/wildai/seo/service/SeoMetadataFactoryTest.java
git diff --cached --check
git commit -m "feat(seo): 生成页面元数据与结构化数据"
```

### Task 7: 服务端渲染产品与文章公开页

**Files:**
- Create: `backend/src/main/java/com/wildai/seo/web/PublicArticlePageController.java`
- Create: `backend/src/main/java/com/wildai/seo/web/PublicProductPageController.java`
- Create: `backend/src/main/java/com/wildai/seo/web/PublicPageNotFoundException.java`
- Create: `backend/src/main/java/com/wildai/seo/web/PublicPageExceptionHandler.java`
- Create: `backend/src/main/resources/templates/public/fragments.html`
- Create: `backend/src/main/resources/templates/public/article-list.html`
- Create: `backend/src/main/resources/templates/public/article-detail.html`
- Create: `backend/src/main/resources/templates/public/product-list.html`
- Create: `backend/src/main/resources/templates/public/product-detail.html`
- Create: `backend/src/main/resources/templates/public/error.html`
- Create: `backend/src/main/resources/static/seo/site.css`
- Create: `backend/src/test/java/com/wildai/smoke/PublicContentPageSmokeIT.java`
- Modify: `backend/src/main/java/com/wildai/common/exception/GlobalExceptionHandler.java`
- Modify: `backend/src/main/java/com/wildai/common/security/SecurityConfig.java`
- Modify: `backend/src/main/java/com/wildai/product/service/ProductService.java`

- [ ] **Step 1: 写首次 HTML 与真实 404 测试**

测试通过后台 API 发布文章并验证：

```java
mockMvc.perform(get("/articles/seo-guide"))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
        .andExpect(content().string(containsString("<h1>SEO 指南</h1>")))
        .andExpect(content().string(containsString(
                "<link rel=\"canonical\" href=\"https://rechargeai.cn/articles/seo-guide\"")))
        .andExpect(content().string(containsString("application/ld+json")))
        .andExpect(content().string(not(containsString("id=\"app\""))));

mockMvc.perform(get("/articles/not-published"))
        .andExpect(status().isNotFound())
        .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
        .andExpect(content().string(containsString("页面不存在")));
```

同一测试还要创建上架产品，验证 `/products` 首次 HTML 包含产品名，`/products/{id}` 包含 `href="/checkout/{id}"`；下架产品详情返回 404。

- [ ] **Step 2: 运行测试并确认失败**

Run: `cd backend && mvn -Dtest=PublicContentPageSmokeIT test`

Expected: FAIL，公开路径被安全配置拦截或没有 MVC 映射。

- [ ] **Step 3: 限定 REST 异常处理范围并放行公开路由**

将 `GlobalExceptionHandler` 注解改为：

```java
@RestControllerAdvice(annotations = org.springframework.web.bind.annotation.RestController.class)
```

在 `SecurityConfig` 中、`/api/**` 规则之前加入：

```java
.requestMatchers(HttpMethod.GET,
        "/products", "/products/**", "/articles", "/articles/**",
        "/seo/**", "/sitemap.xml", "/robots.txt").permitAll()
```

- [ ] **Step 4: 增加公开产品查询和 MVC 控制器**

`ProductService` 增加：

```java
public ProductDetailDto getOnShelfById(Long id) {
    AiServiceProduct product = requireOnShelf(id);
    return toDto(product, true);
}
```

控制器模型键固定如下：

- 产品列表：`products`、`seo`
- 产品详情：`product`、`seo`
- 文章列表：`articles`、`seo`
- 文章详情：`article`、`seo`

文章详情只调用 `ArticlePublicQueryService.findPublished`；空结果抛出页面专用 `PublicPageNotFoundException`。产品详情捕获 `ProductService.getOnShelfById` 抛出的 `NOT_FOUND` 或 `PRODUCT_OFF_SHELF`，转换为同一页面 404 信号，不把业务异常渲染成 JSON。

`PublicPageExceptionHandler` 使用 `ModelAndView("public/error")`，处理 `PublicPageNotFoundException` 时设置 `HttpStatus.NOT_FOUND`；其他公开页异常设置 500 并用中文日志记录请求路径。异常类型使用固定定义：

```java
package com.wildai.seo.web;

public class PublicPageNotFoundException extends RuntimeException {
    public PublicPageNotFoundException() {
        super("页面不存在");
    }
}
```

- [ ] **Step 5: 创建模板公共 head**

`fragments.html` 的 head 片段必须完整输出：

```html
<head th:fragment="head(seo)">
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0, viewport-fit=cover">
  <meta name="theme-color" content="#f7f5f0">
  <meta name="robots" th:content="${seo.robots}">
  <title th:text="${seo.title}">RechargeAi</title>
  <meta name="description" th:content="${seo.description}">
  <link rel="canonical" th:href="${seo.canonical}">
  <meta property="og:title" th:content="${seo.title}">
  <meta property="og:description" th:content="${seo.description}">
  <meta property="og:type" th:content="${seo.type}">
  <meta property="og:url" th:content="${seo.canonical}">
  <meta property="og:image" th:if="${seo.imageUrl != null}" th:content="${seo.imageUrl}">
  <meta name="twitter:card" content="summary_large_image">
  <link rel="icon" href="/favicon.svg">
  <link rel="stylesheet" href="/seo/site.css">
  <script type="application/ld+json" th:utext="${seo.jsonLd}"></script>
</head>
```

页面使用语义化 `<header>`、`<nav>`、`<main>`、`<article>`。文章正文唯一允许 `th:utext` 的位置为已经过 `ArticleMarkdownService` 过滤的 `article.contentHtml`。

- [ ] **Step 6: 创建兼容 CSS**

`site.css` 以现有用户端色彩和间距为基准，每个增强值前必须有可用降级：

```css
:root {
  --color-primary: #1f6b56;
  --color-primary: oklch(0.42 0.09 165);
  --color-bg: #f7f5f0;
  --color-bg: oklch(0.98 0.008 85);
  --color-text: #3d3a36;
  --color-heading: #1f1d1a;
  --color-border: #e8e4dc;
  --content-max: 960px;
  font-family: system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
}
* { box-sizing: border-box; }
body { margin: 0; min-height: 100vh; color: var(--color-text); background: var(--color-bg); }
.site-header { position: sticky; top: 0; background: #f7f5f0; background: rgb(247 245 240 / 92%); }
@supports (backdrop-filter: blur(12px)) {
  .site-header { backdrop-filter: blur(12px); }
}
.site-inner, .page { width: min(100% - 2rem, var(--content-max)); margin-inline: auto; }
.site-nav { display: flex; gap: 1rem; align-items: center; min-height: 56px; }
.grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(240px, 1fr)); gap: 1rem; }
.card { display: block; padding: 1rem; border: 1px solid var(--color-border); border-radius: 10px; background: #fff; }
.article-body { line-height: 1.8; overflow-wrap: anywhere; }
.article-body img { display: block; max-width: 100%; height: auto; }
.article-body pre { max-width: 100%; overflow-x: auto; padding: 1rem; }
.button { display: inline-flex; min-height: 44px; align-items: center; justify-content: center; padding: .75rem 1rem; }
@media (max-width: 639px) { .site-nav { flex-wrap: wrap; } }
@media (prefers-reduced-motion: reduce) { *, *::before, *::after { scroll-behavior: auto !important; } }
```

- [ ] **Step 7: 运行 SSR 测试**

Run:

```bash
mvn -Dtest=PublicContentPageSmokeIT,ArticleAdminSmokeIT test
```

Expected: PASS；公开正文位于首次 HTML，草稿、撤回文章和下架产品返回 HTML 404。

- [ ] **Step 8: 提交 SSR 公开页**

```bash
git add backend/src/main/java/com/wildai/seo/web \
  backend/src/main/java/com/wildai/common/exception/GlobalExceptionHandler.java \
  backend/src/main/java/com/wildai/common/security/SecurityConfig.java \
  backend/src/main/java/com/wildai/product/service/ProductService.java \
  backend/src/main/resources/templates/public \
  backend/src/main/resources/static/seo \
  backend/src/test/java/com/wildai/smoke/PublicContentPageSmokeIT.java
git diff --cached --check
git commit -m "feat(seo): 服务端渲染公开内容页"
```

### Task 8: 生成 robots 与动态 sitemap

**Files:**
- Create: `backend/src/main/java/com/wildai/seo/service/SitemapService.java`
- Create: `backend/src/main/java/com/wildai/seo/web/SeoEndpointController.java`
- Create: `backend/src/test/java/com/wildai/smoke/SeoEndpointSmokeIT.java`
- Modify: `backend/src/main/java/com/wildai/product/repository/AiServiceProductRepository.java`
- Modify: `backend/src/main/java/com/wildai/product/service/ProductService.java`

- [ ] **Step 1: 写 sitemap 状态过滤与失效测试**

测试流程：创建上架产品、创建并发布文章、请求 sitemap、撤回文章、再次请求 sitemap。

```java
mockMvc.perform(get("/sitemap.xml"))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_XML))
        .andExpect(content().string(containsString("/articles/seo-sitemap")))
        .andExpect(content().string(containsString("/products/" + productId)))
        .andExpect(content().string(containsString("<lastmod>")));

mockMvc.perform(get("/robots.txt"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString(
                "Sitemap: https://rechargeai.cn/sitemap.xml")))
        .andExpect(content().string(containsString("Disallow: /admin/")));
```

撤回后第二次 sitemap 响应不得包含文章 slug。

- [ ] **Step 2: 运行测试并确认失败**

Run: `cd backend && mvn -Dtest=SeoEndpointSmokeIT test`

Expected: FAIL，SEO 端点不存在。

- [ ] **Step 3: 实现版本化缓存和 XML 生成**

`AiServiceProductRepository` 增加：

```java
List<AiServiceProduct> findByStatusOrderByUpdatedAtDesc(String status);
```

`SitemapService` 保存 `record CachedSitemap(long version, String xml)`；`render()` 比较 `SitemapVersion.current()`，版本一致直接返回缓存，否则读取：

- `ArticleStatus.PUBLISHED` 文章；
- `status = ON_SHELF` 产品；
- 固定 `/products`、`/articles`。

使用 `XMLStreamWriter` 写 UTF-8 XML，禁止字符串拼接未转义 slug。单条数据异常使用中文 WARN 日志记录 ID 并跳过，不中断整个 sitemap。

在 `ArticleManagementService` 已发布更新、发布、撤回、删除草稿后调用的失效逻辑保持不变。`ProductService.create` 与 `save` 成功后调用 `sitemapVersion.invalidate()`。

- [ ] **Step 4: 实现端点**

```java
@RestController
public class SeoEndpointController {
    private final SitemapService sitemapService;
    private final WildAiProperties properties;

    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> sitemap() {
        return ResponseEntity.ok().cacheControl(CacheControl.noCache())
                .body(sitemapService.render());
    }

    @GetMapping(value = "/robots.txt", produces = MediaType.TEXT_PLAIN_VALUE)
    public String robots() {
        return """
                User-agent: *
                Allow: /products
                Allow: /articles
                Disallow: /admin/
                Disallow: /login
                Disallow: /register
                Disallow: /checkout/
                Disallow: /payment/
                Disallow: /transaction-record
                Sitemap: %s/sitemap.xml
                """.formatted(properties.getSeo().getBaseUrl());
    }
}
```

- [ ] **Step 5: 运行 SEO 端点测试**

Run: `mvn -Dtest=SeoEndpointSmokeIT test`

Expected: PASS；撤回后缓存失效，sitemap 不再包含该文章。

- [ ] **Step 6: 提交 sitemap 与 robots**

```bash
git add backend/src/main/java/com/wildai/seo \
  backend/src/main/java/com/wildai/product/repository/AiServiceProductRepository.java \
  backend/src/main/java/com/wildai/product/service/ProductService.java \
  backend/src/test/java/com/wildai/smoke/SeoEndpointSmokeIT.java
git diff --cached --check
git commit -m "feat(seo): 增加动态站点地图与 robots"
```

### Task 9: 增加后台文章列表

**Files:**
- Create: `frontend-admin/src/services/articleApi.ts`
- Create: `frontend-admin/src/views/ArticleListView.vue`
- Modify: `frontend-admin/src/router/index.ts`
- Modify: `frontend-admin/src/App.vue`

- [ ] **Step 1: 先注册缺失页面路由**

在管理端路由增加：

```ts
{ path: '/articles', component: () => import('../views/ArticleListView.vue'), meta: { auth: true } },
```

在侧栏增加 `/articles` 菜单，并让 `activeMenu` 对 `/articles` 前缀返回 `/articles`。

- [ ] **Step 2: 运行构建并确认失败**

Run: `cd frontend-admin && npm run build`

Expected: FAIL，Vite 无法解析 `ArticleListView.vue`。

- [ ] **Step 3: 创建类型安全 API**

`articleApi.ts` 必须导出：

```ts
export type ArticleStatus = 'DRAFT' | 'PUBLISHED'
export interface ArticleSummary {
  id: number; title: string; slug: string; summary: string; coverImageUrl?: string | null
  status: ArticleStatus; publishedAt?: string | null; createdAt: string; updatedAt: string
}
export interface ArticleDetail extends ArticleSummary {
  contentMarkdown: string; contentHtml: string
  seoTitle?: string | null; seoDescription?: string | null
}
export interface ArticleSavePayload {
  title: string; slug?: string; summary?: string; coverImageUrl?: string
  contentMarkdown: string; seoTitle?: string; seoDescription?: string
}
export interface PageResult<T> { pageNo: number; pageSize: number; total: number; items: T[] }

export const listArticles = (params: Record<string, unknown>) =>
  http.get<{ data: PageResult<ArticleSummary> }>('/articles', { params })
export const getArticle = (id: number) => http.get<{ data: ArticleDetail }>(`/articles/${id}`)
export const createArticle = (payload: ArticleSavePayload) =>
  http.post<{ data: ArticleDetail }>('/articles', payload)
export const updateArticle = (id: number, payload: ArticleSavePayload) =>
  http.put<{ data: ArticleDetail }>(`/articles/${id}`, payload)
export const publishArticle = (id: number) => http.post(`/articles/${id}/publish`)
export const withdrawArticle = (id: number) => http.post(`/articles/${id}/withdraw`)
export const deleteArticle = (id: number) => http.delete(`/articles/${id}`)
```

- [ ] **Step 4: 创建文章列表页**

页面必须包含以下稳定交互：

- 标题输入框 `data-testid="article-search"`；
- 状态选择 `DRAFT` / `PUBLISHED`；
- “新建文章”跳转 `/articles/new`；
- 编辑跳转 `/articles/{id}/edit`；
- 草稿显示“发布”和“删除”；已发布显示“撤回”；
- 删除使用 `ElMessageBox.confirm`；所有失败消息优先显示后端中文 `message`；
- `el-pagination` 使用后端 `pageNo`、`pageSize`、`total`。

状态文案固定为：

```ts
const statusLabel: Record<ArticleStatus, string> = {
  DRAFT: '草稿',
  PUBLISHED: '已发布',
}
```

- [ ] **Step 5: 运行管理端构建**

Run: `npm run build`

Expected: PASS，无 TypeScript 或 Vue 模板错误。

- [ ] **Step 6: 提交文章列表**

```bash
git add frontend-admin/src/services/articleApi.ts \
  frontend-admin/src/views/ArticleListView.vue \
  frontend-admin/src/router/index.ts frontend-admin/src/App.vue
git diff --cached --check
git commit -m "feat(admin): 增加文章列表管理"
```

### Task 10: 增加文章编辑、服务端预览和图片上传

**Files:**
- Create: `frontend-admin/src/views/ArticleEditView.vue`
- Modify: `frontend-admin/src/services/articleApi.ts`
- Modify: `frontend-admin/src/components/MarkdownEditor.vue`
- Modify: `frontend-admin/src/router/index.ts`

- [ ] **Step 1: 注册编辑路由并确认构建失败**

```ts
{ path: '/articles/new', component: () => import('../views/ArticleEditView.vue'), meta: { auth: true } },
{ path: '/articles/:id/edit', component: () => import('../views/ArticleEditView.vue'), meta: { auth: true } },
```

Run: `cd frontend-admin && npm run build`

Expected: FAIL，缺少 `ArticleEditView.vue`。

- [ ] **Step 2: 扩展文章 API**

```ts
export const previewArticle = (contentMarkdown: string) =>
  http.post<{ data: { html: string } }>('/articles/preview', { contentMarkdown })

export async function uploadArticleImage(file: File): Promise<string> {
  const form = new FormData()
  form.append('file', file)
  const { data } = await http.post<{ data: { url: string } }>('/article-assets', form, {
    headers: { 'Content-Type': 'multipart/form-data' }, timeout: 60_000,
  })
  return data.data.url
}
```

- [ ] **Step 3: 让 MarkdownEditor 支持注入能力**

保持教程默认行为不变，新增可选 props：

```ts
const props = withDefaults(defineProps<{
  label: string
  placeholder?: string
  rows?: number
  allowVideo?: boolean
  uploadImage?: (file: File) => Promise<string>
  previewMarkdown?: (source: string) => Promise<string>
}>(), { allowVideo: true })
```

规则：

- `uploadImage` 存在时使用返回 URL 插入 `![文件名](url)`；否则继续调用 `uploadTutorialAsset`。
- `allowVideo=false` 时不渲染上传视频按钮和 input。
- 切换到预览标签时，若提供 `previewMarkdown`，调用后端并显示返回 HTML；正文变化后使用 300ms 防抖刷新。
- 请求竞态使用递增序号，只接受最后一次响应。
- 服务端预览失败显示“预览失败，请稍后重试”，不得回退到允许原始 HTML 的本地渲染。

- [ ] **Step 4: 创建文章编辑页**

表单字段固定为标题、slug、摘要、封面地址、正文、SEO 标题、SEO 描述。正文编辑器调用：

```vue
<MarkdownEditor
  v-model="form.contentMarkdown"
  label="文章正文（Markdown）"
  :rows="18"
  :allow-video="false"
  :upload-image="uploadArticleImage"
  :preview-markdown="previewMarkdown"
/>
```

页面行为：

- 新建页保存后 `router.replace('/articles/{id}/edit')`；
- 已发布文章的 slug 输入框禁用；撤回后因 `publishedAt` 仍存在也保持禁用；
- “保存草稿”与“发布”分开；新文章点击发布时先保存，再调用发布 API；
- 发布成功后显示公开链接 `/articles/{slug}`；
- 标题和正文发布前为空时在前端显示中文提示；后端仍执行最终校验；
- 发布前若正文匹配空替代文本图片语法 `![](...)`，阻止发布并提示“请为文章图片填写说明文字”；
- 表单添加 `data-testid="article-title"`、`article-slug`、`article-content`、`article-save`、`article-publish`。

- [ ] **Step 5: 运行管理端构建**

Run: `npm run build`

Expected: PASS；现有服务类型教程编辑页仍能上传视频和本地预览。

- [ ] **Step 6: 提交文章编辑器**

```bash
git add frontend-admin/src/views/ArticleEditView.vue \
  frontend-admin/src/services/articleApi.ts \
  frontend-admin/src/components/MarkdownEditor.vue \
  frontend-admin/src/router/index.ts
git diff --cached --check
git commit -m "feat(admin): 增加文章编辑发布流程"
```

### Task 11: 调整用户 SPA 边界与浏览器兼容

**Files:**
- Modify: `frontend-user/src/router/index.ts`
- Modify: `frontend-user/src/App.vue`
- Modify: `frontend-user/index.html`
- Modify: `frontend-user/vite.config.ts`
- Modify: `frontend-user/src/style.css`
- Modify: `frontend-admin/index.html`
- Modify: `frontend-admin/vite.config.ts`
- Modify: `frontend-admin/src/style.css`

- [ ] **Step 1: 修改公开导航与 SPA 路由**

从用户端 Vue Router 删除 `/products`、`/products/:id`；保留登录、注册、结算、支付和交易记录。根路径改为整页跳转，保证本地开发直接访问 `/` 也进入 SSR 产品页：

```ts
{
  path: '/',
  beforeEnter: () => {
    window.location.assign('/products')
    return false
  },
},
```

`App.vue` 中 logo 和“服务”改用普通 `<a href="/products">`，新增普通 `<a href="/articles">文章</a>`；登录和交易记录继续使用 `RouterLink`。

- [ ] **Step 2: 增加私有 SPA noindex 并移除远程字体**

两端 `index.html` 删除 Google Fonts 的 `preconnect` 和 stylesheet，加入：

```html
<meta name="robots" content="noindex,nofollow" />
<meta name="format-detection" content="telephone=no" />
```

- [ ] **Step 3: 固定构建目标**

两端 `vite.config.ts` 增加：

```ts
build: {
  target: 'es2018',
  cssTarget: 'safari14',
},
```

- [ ] **Step 4: 增加 CSS 降级**

对每个核心 `oklch` 变量先声明十六进制值；对 `100dvh` 前增加 `100vh`；用户头部增加：

```css
background: #f7f5f0;
background: oklch(0.98 0.008 85 / 0.92);
-webkit-backdrop-filter: blur(12px);
backdrop-filter: blur(12px);
```

字体栈统一为：

```css
--font-sans: system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
--font-serif: Georgia, "Songti SC", "Noto Serif CJK SC", serif;
```

- [ ] **Step 5: 分别构建两端**

Run:

```bash
cd frontend-user && npm run build
cd ../frontend-admin && npm run build
```

Expected: 两次构建均 PASS；用户构建不再包含公开产品页路由 chunk。

- [ ] **Step 6: 提交 SPA 边界和兼容配置**

```bash
git add frontend-user/src/router/index.ts frontend-user/src/App.vue \
  frontend-user/index.html frontend-user/vite.config.ts frontend-user/src/style.css \
  frontend-admin/index.html frontend-admin/vite.config.ts frontend-admin/src/style.css
git diff --cached --check
git commit -m "feat(frontend): 调整公开路由与主流浏览器兼容"
```

### Task 12: 更新 Nginx 与生产部署

**Files:**
- Modify: `deploy/nginx.conf`
- Modify: `deploy/docker-compose.prod.yml`
- Modify: `deploy/deploy.sh`
- Modify: `deploy/.env.example`

- [ ] **Step 1: 增加规范域名与 SSR 转发**

Nginx 必须形成三个 server 边界：

```nginx
server {
    listen 80;
    server_name rechargeai.cn www.rechargeai.cn;
    return 301 https://rechargeai.cn$request_uri;
}

server {
    listen 443 ssl;
    http2 on;
    server_name www.rechargeai.cn;
    ssl_certificate /etc/nginx/ssl/fullchain.pem;
    ssl_certificate_key /etc/nginx/ssl/privkey.key;
    return 301 https://rechargeai.cn$request_uri;
}
```

用户端开发服务器还要把 SSR 路径转发到 Spring Boot，避免 Vite 的 SPA fallback 截获公开页：

```ts
proxy: {
  '/api': { target: 'http://localhost:8080', changeOrigin: true },
  '/products': { target: 'http://localhost:8080', changeOrigin: true },
  '/articles': { target: 'http://localhost:8080', changeOrigin: true },
  '/seo': { target: 'http://localhost:8080', changeOrigin: true },
  '/sitemap.xml': { target: 'http://localhost:8080', changeOrigin: true },
  '/robots.txt': { target: 'http://localhost:8080', changeOrigin: true },
},
```

主域 server 中加入：

```nginx
location = / { return 301 /products; }

location ~ ^/(products|articles)(/.*)?$ {
    proxy_pass http://backend:8080;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
}

location ~ ^/(sitemap\.xml|robots\.txt)$ { proxy_pass http://backend:8080; }
location /seo/ { proxy_pass http://backend:8080; }
```

这些 location 必须位于用户 SPA `location /` 之前。

- [ ] **Step 2: 为私有 SPA 增加响应头**

```nginx
location ~ ^/(login|register|checkout|payment|transaction-record)(/|$) {
    root /usr/share/nginx/html/user;
    add_header X-Robots-Tag "noindex, nofollow" always;
    try_files $uri $uri/ /index.html;
}

location /admin/ {
    root /usr/share/nginx/html;
    add_header X-Robots-Tag "noindex, nofollow" always;
    try_files $uri $uri/ /admin/index.html;
}
```

用户端剩余 fallback 也设置 `X-Robots-Tag`，防止未知 SPA 路径被收录。

- [ ] **Step 3: 挂载文章图片目录并注入配置**

`docker-compose.prod.yml` 后端增加：

```yaml
volumes:
  - ./uploads/article:/app/uploads/article:Z
environment:
  WILDAI_ARTICLE_UPLOAD_DIR: /app/uploads/article
  WILDAI_SEO_BASE_URL: ${WILDAI_SEO_BASE_URL:-https://rechargeai.cn}
```

`deploy.sh` 的组装目录加入 `"$DEPLOY/uploads/article"`。`.env.example` 加入：

```dotenv
WILDAI_SEO_BASE_URL=https://rechargeai.cn
WILDAI_ARTICLE_MAX_IMAGE_BYTES=5242880
```

- [ ] **Step 4: 验证 Compose 配置与路由文本**

Run:

```bash
docker compose -f deploy/docker-compose.prod.yml --env-file deploy/.env.example config -q
rg -n "products\|articles|sitemap|robots|X-Robots-Tag|rechargeai.cn" deploy/nginx.conf
```

Expected: Compose 命令退出码 0；Nginx 输出同时包含 SSR 转发、规范域名和 noindex 规则。

- [ ] **Step 5: 提交部署配置**

```bash
git add deploy/nginx.conf deploy/docker-compose.prod.yml deploy/deploy.sh deploy/.env.example
git diff --cached --check
git commit -m "feat(deploy): 接入 SEO 公开路由与文章资源"
```

### Task 13: 增加跨浏览器验收并完成闭环验证

**Files:**
- Create: `frontend-user/playwright.config.ts`
- Create: `frontend-user/e2e/seo-articles.spec.ts`
- Modify: `frontend-user/package.json`
- Modify: `frontend-user/package-lock.json`
- Modify: `scripts/smoke-test.sh`

- [ ] **Step 1: 安装 Playwright 测试依赖**

Run:

```bash
cd frontend-user
npm install --save-dev @playwright/test
npx playwright install chromium firefox webkit
```

Expected: `package.json` 出现 `@playwright/test`，三个浏览器安装成功。

- [ ] **Step 2: 创建三浏览器配置**

```ts
import { defineConfig, devices } from '@playwright/test'

export default defineConfig({
  testDir: './e2e',
  fullyParallel: false,
  retries: 1,
  use: { baseURL: process.env.PUBLIC_BASE_URL ?? 'http://localhost:8080' },
  projects: [
    { name: 'chromium', use: { ...devices['Desktop Chrome'] } },
    { name: 'firefox', use: { ...devices['Desktop Firefox'] } },
    { name: 'webkit', use: { ...devices['Desktop Safari'] } },
  ],
})
```

在 `package.json` scripts 增加：

```json
"test:e2e": "playwright test"
```

- [ ] **Step 3: 写公开页面与发布闭环测试**

`seo-articles.spec.ts` 使用唯一 slug `${projectName}-${Date.now()}`，测试流程固定为：

1. 通过 `/admin/api/auth/login` 获取 token；
2. 通过 API 创建并发布文章；
3. 浏览器访问 `/articles/{slug}`，断言标题、正文、canonical、JSON-LD 和文章链接可见；
4. 访问 `/products`，断言存在至少一个产品卡片及可点击详情；
5. 撤回文章后再次访问详情，断言 HTTP 404 和“页面不存在”；
6. 删除草稿完成清理。

核心断言：

```ts
const response = await page.goto(`/articles/${slug}`)
expect(response?.status()).toBe(200)
await expect(page.getByRole('heading', { name: title })).toBeVisible()
await expect(page.locator('link[rel="canonical"]')).toHaveAttribute(
  'href', `https://rechargeai.cn/articles/${slug}`,
)
await expect(page.locator('script[type="application/ld+json"]')).toHaveCount(1)
```

- [ ] **Step 4: 扩展线上 smoke-test**

在现有支付冒烟测试结束前增加文章创建、发布、HTML 验证、sitemap 验证、撤回和删除；所有新增日志使用中文。HTML 校验必须使用 `curl -fsS "${BASE_URL}/articles/${ARTICLE_SLUG}"`，并检查正文和 canonical，而不是只调用 JSON API。

- [ ] **Step 5: 运行后端完整验证**

Run:

```bash
cd backend
mvn test
mvn -Dtest=ArticleRepositorySmokeIT,ArticleAdminSmokeIT,PublicContentPageSmokeIT,SeoEndpointSmokeIT test
mvn package -DskipTests
```

Expected: 三条命令均退出码 0；单元测试和四个 Testcontainers 集成测试通过。

- [ ] **Step 6: 运行前端构建与跨浏览器测试**

先启动 MySQL/Redis、后端和管理端开发服务器，再运行：

```bash
cd frontend-user && npm run build
cd ../frontend-admin && npm run build
cd ../frontend-user && npm run test:e2e
```

Expected: 两端构建通过；Chromium、Firefox、WebKit 三个项目全部 PASS。

- [ ] **Step 7: 验证 SEO 响应，不执行 JavaScript**

Run:

```bash
curl -fsS http://localhost:8080/products | rg '<title>|canonical|application/ld\+json|product-card'
curl -fsS http://localhost:8080/articles | rg '<title>|canonical|application/ld\+json'
curl -fsS http://localhost:8080/sitemap.xml | rg '<urlset|/products|/articles'
curl -fsS http://localhost:8080/robots.txt | rg 'Sitemap:|Disallow:'
```

Expected: 四个命令均匹配；HTML 正文和 SEO 标签直接存在于响应中。

- [ ] **Step 8: 检查触碰范围并提交验收测试**

```bash
git diff --check
git status --short
git add frontend-user/package.json frontend-user/package-lock.json \
  frontend-user/playwright.config.ts frontend-user/e2e/seo-articles.spec.ts \
  scripts/smoke-test.sh
git diff --cached --check
git commit -m "test(seo): 增加文章发布与跨浏览器验收"
```

- [ ] **Step 9: 最终提交历史与工作区检查**

Run:

```bash
git log --oneline --decorate -13
git status --short
```

Expected: 能看到本计划的分步提交；工作区无本功能遗留修改。若基线仍包含用户事先保留的修改，必须与 Gate 1 记录逐项一致，不得擅自清理。

---

## 需求覆盖检查

| 设计要求 | 实施任务 |
|----------|----------|
| 文章草稿、预览、发布、撤回、删除草稿 | Task 3、4、9、10 |
| 发布后立即公开、撤回后真实 404 | Task 3、7、13 |
| 产品与文章完整 SSR HTML | Task 7 |
| Meta、canonical、Open Graph、JSON-LD | Task 6、7 |
| 动态 sitemap 与 robots | Task 8 |
| 私有页面 noindex | Task 11、12 |
| 文章图片安全上传 | Task 5、10、12 |
| Markdown 安全过滤 | Task 2、4 |
| 主流浏览器与 CSS 降级 | Task 7、11、13 |
| 规范域名与 Nginx 路由 | Task 12 |
| 管理审计 | Task 3 |
| 不覆盖现有未提交修改 | 执行门槛、每个提交步骤、Task 13 |
