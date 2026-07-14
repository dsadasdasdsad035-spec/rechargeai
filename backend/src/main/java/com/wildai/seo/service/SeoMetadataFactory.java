package com.wildai.seo.service;

import com.wildai.common.config.WildAiProperties;
import com.wildai.content.dto.ArticlePublicDto;
import com.wildai.content.dto.ArticlePublicSummaryDto;
import com.wildai.product.dto.ProductDetailDto;
import com.wildai.seo.dto.SeoMetadata;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

@Component
public class SeoMetadataFactory {

    private static final String CONTEXT = "https://schema.org";
    private static final String ROBOTS = "index,follow";
    private static final int DESCRIPTION_MAX_LENGTH = 160;
    private static final Pattern SLUG_PATTERN = Pattern.compile("[a-z0-9]+(?:-[a-z0-9]+)*");

    private final StructuredDataService structuredDataService;
    private final URI baseUri;
    private final String baseUrl;
    private final String siteName;

    public SeoMetadataFactory(
            WildAiProperties properties,
            StructuredDataService structuredDataService) {
        if (properties == null || properties.getSeo() == null) {
            throw new IllegalArgumentException("SEO 配置不能为空");
        }
        if (structuredDataService == null) {
            throw new IllegalArgumentException("结构化数据服务不能为空");
        }
        this.baseUrl = normalizeBaseUrl(properties.getSeo().getBaseUrl());
        this.baseUri = URI.create(this.baseUrl);
        this.siteName = requireText(properties.getSeo().getSiteName(), "SEO 站点名称不能为空");
        this.structuredDataService = structuredDataService;
    }

    public SeoMetadata forArticle(ArticlePublicDto article) {
        requireArticle(article);
        String canonical = articleUrl(article.slug());
        String description = description(firstNonBlank(article.seoDescription(), article.summary(), article.title()));
        String imageUrl = safeImageUrl(article.coverImageUrl());

        var articleNode = new LinkedHashMap<String, Object>();
        articleNode.put("@type", "Article");
        articleNode.put("headline", requireText(article.title(), "文章标题不能为空"));
        articleNode.put("description", description);
        articleNode.put("url", canonical);
        if (article.publishedAt() != null) {
            articleNode.put("datePublished", article.publishedAt().toString());
        }
        if (article.updatedAt() != null) {
            articleNode.put("dateModified", article.updatedAt().toString());
        }
        articleNode.put("publisher", organization());
        if (imageUrl != null) {
            articleNode.put("image", imageUrl);
        }

        var root = graph(articleNode, breadcrumbs(List.of(
                breadcrumb(1, siteName, baseUrl),
                breadcrumb(2, "文章中心", url("/articles")),
                breadcrumb(3, article.title().trim(), canonical))));
        return metadata(
                pageTitle(firstNonBlank(article.seoTitle(), article.title())),
                description,
                canonical,
                "article",
                imageUrl,
                root);
    }

    public SeoMetadata forProduct(ProductDetailDto product) {
        requireProduct(product);
        String canonical = productUrl(product.id());
        String description = productDescription(product);

        var offer = new LinkedHashMap<String, Object>();
        offer.put("@type", "Offer");
        offer.put("price", product.salePrice());
        offer.put("priceCurrency", requireText(product.currency(), "产品币种不能为空"));
        offer.put("availability", "ON_SHELF".equals(product.status())
                ? "https://schema.org/InStock"
                : "https://schema.org/OutOfStock");

        var productNode = new LinkedHashMap<String, Object>();
        productNode.put("@type", "Product");
        productNode.put("name", requireText(product.name(), "产品名称不能为空"));
        productNode.put("url", canonical);
        productNode.put("offers", offer);

        var root = graph(productNode, breadcrumbs(List.of(
                breadcrumb(1, siteName, baseUrl),
                breadcrumb(2, "AI 订阅产品", url("/products")),
                breadcrumb(3, product.name().trim(), canonical))));
        return metadata(
                pageTitle(product.name()),
                description,
                canonical,
                "product",
                null,
                root);
    }

    public SeoMetadata forArticleList(List<ArticlePublicSummaryDto> articles) {
        if (articles == null) {
            throw new IllegalArgumentException("文章列表不能为空");
        }
        var elements = new ArrayList<Map<String, Object>>(articles.size());
        for (int index = 0; index < articles.size(); index++) {
            var article = articles.get(index);
            requireArticleSummary(article);
            elements.add(listItem(index + 1, article.title().trim(), articleUrl(article.slug())));
        }
        String canonical = url("/articles");
        var root = itemList("文章中心", canonical, elements);
        return metadata(
                pageTitle("文章中心"),
                description("浏览 " + siteName + " 发布的 AI 相关文章与使用指南。"),
                canonical,
                "website",
                null,
                root);
    }

    public SeoMetadata forProductList(List<ProductDetailDto> products) {
        if (products == null) {
            throw new IllegalArgumentException("产品列表不能为空");
        }
        var elements = new ArrayList<Map<String, Object>>(products.size());
        for (int index = 0; index < products.size(); index++) {
            var product = products.get(index);
            requireProduct(product);
            elements.add(listItem(index + 1, product.name().trim(), productUrl(product.id())));
        }
        String canonical = url("/products");
        var root = itemList("AI 订阅产品", canonical, elements);
        return metadata(
                pageTitle("AI 订阅产品"),
                description("浏览 " + siteName + " 提供的 AI 订阅产品与价格信息。"),
                canonical,
                "website",
                null,
                root);
    }

    private SeoMetadata metadata(
            String title,
            String description,
            String canonical,
            String type,
            String imageUrl,
            Map<String, Object> structuredData) {
        return new SeoMetadata(
                title,
                description,
                canonical,
                type,
                imageUrl,
                ROBOTS,
                structuredDataService.toSafeJson(structuredData));
    }

    private Map<String, Object> graph(Map<String, Object> detail, Map<String, Object> breadcrumb) {
        var root = new LinkedHashMap<String, Object>();
        root.put("@context", CONTEXT);
        root.put("@graph", List.of(detail, breadcrumb));
        return root;
    }

    private Map<String, Object> itemList(
            String name,
            String canonical,
            List<Map<String, Object>> elements) {
        var root = new LinkedHashMap<String, Object>();
        root.put("@context", CONTEXT);
        root.put("@type", "ItemList");
        root.put("name", name);
        root.put("url", canonical);
        root.put("itemListElement", elements);
        return root;
    }

    private Map<String, Object> organization() {
        var organization = new LinkedHashMap<String, Object>();
        organization.put("@type", "Organization");
        organization.put("name", siteName);
        return organization;
    }

    private Map<String, Object> breadcrumbs(List<Map<String, Object>> items) {
        var breadcrumbs = new LinkedHashMap<String, Object>();
        breadcrumbs.put("@type", "BreadcrumbList");
        breadcrumbs.put("itemListElement", items);
        return breadcrumbs;
    }

    private Map<String, Object> breadcrumb(int position, String name, String item) {
        var breadcrumb = new LinkedHashMap<String, Object>();
        breadcrumb.put("@type", "ListItem");
        breadcrumb.put("position", position);
        breadcrumb.put("name", name);
        breadcrumb.put("item", item);
        return breadcrumb;
    }

    private Map<String, Object> listItem(int position, String name, String itemUrl) {
        var item = new LinkedHashMap<String, Object>();
        item.put("@type", "ListItem");
        item.put("position", position);
        item.put("name", name);
        item.put("url", itemUrl);
        return item;
    }

    private String pageTitle(String value) {
        if (isBlank(value)) {
            throw new IllegalArgumentException("页面标题不能为空");
        }
        String suffix = " | " + siteName;
        String title = value.stripTrailing();
        while (title.endsWith(suffix)) {
            title = title.substring(0, title.length() - suffix.length()).stripTrailing();
        }
        title = title.strip();
        if (title.isEmpty()) {
            throw new IllegalArgumentException("页面标题移除站点后缀后不能为空");
        }
        return title + suffix;
    }

    private String productDescription(ProductDetailDto product) {
        var parts = new ArrayList<String>();
        parts.add(requireText(product.name(), "产品名称不能为空"));
        if (!isBlank(product.serviceType())) {
            parts.add("服务类型：" + product.serviceType().trim());
        }
        if (product.periodDays() != null) {
            parts.add("服务周期：" + product.periodDays() + " 天");
        }
        parts.add("价格：" + product.salePrice().toPlainString() + " "
                + requireText(product.currency(), "产品币种不能为空"));
        return description(String.join("，", parts));
    }

    private String description(String value) {
        String normalized = requireText(value, "页面描述不能为空")
                .replaceAll("\\s+", " ")
                .trim();
        if (normalized.length() <= DESCRIPTION_MAX_LENGTH) {
            return normalized;
        }
        int end = DESCRIPTION_MAX_LENGTH;
        if (Character.isHighSurrogate(normalized.charAt(end - 1))) {
            end--;
        }
        return normalized.substring(0, end).trim();
    }

    private String safeImageUrl(String value) {
        if (isBlank(value)) {
            return null;
        }
        String candidate = value.trim();
        if (candidate.startsWith("//")) {
            return null;
        }
        try {
            URI imageUri = new URI(candidate);
            URI resolved = imageUri.isAbsolute() ? imageUri : baseUri.resolve(imageUri);
            return isHttpScheme(resolved.getScheme()) && resolved.getHost() != null
                    ? resolved.toString()
                    : null;
        } catch (URISyntaxException ex) {
            return null;
        }
    }

    private String articleUrl(String slug) {
        String safeSlug = requireText(slug, "文章链接标识不能为空");
        if (!SLUG_PATTERN.matcher(safeSlug).matches()) {
            throw new IllegalArgumentException("文章链接标识格式无效");
        }
        return url("/articles/" + safeSlug);
    }

    private String productUrl(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("产品编号必须为正数");
        }
        return url("/products/" + id);
    }

    private String url(String path) {
        return baseUrl + path;
    }

    private void requireArticle(ArticlePublicDto article) {
        if (article == null) {
            throw new IllegalArgumentException("文章不能为空");
        }
        if (article.id() == null || article.id() <= 0) {
            throw new IllegalArgumentException("文章编号必须为正数");
        }
        requireText(article.title(), "文章标题不能为空");
        articleUrl(article.slug());
    }

    private void requireArticleSummary(ArticlePublicSummaryDto article) {
        if (article == null) {
            throw new IllegalArgumentException("文章摘要不能为空");
        }
        if (article.id() == null || article.id() <= 0) {
            throw new IllegalArgumentException("文章编号必须为正数");
        }
        requireText(article.title(), "文章标题不能为空");
        articleUrl(article.slug());
    }

    private void requireProduct(ProductDetailDto product) {
        if (product == null) {
            throw new IllegalArgumentException("产品不能为空");
        }
        productUrl(product.id());
        requireText(product.name(), "产品名称不能为空");
        if (product.salePrice() == null) {
            throw new IllegalArgumentException("产品售价不能为空");
        }
        requireText(product.currency(), "产品币种不能为空");
    }

    private static String normalizeBaseUrl(String value) {
        String normalized = requireText(value, "SEO 基础地址不能为空");
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        try {
            URI uri = new URI(normalized);
            if (!uri.isAbsolute()
                    || !isHttpScheme(uri.getScheme())
                    || uri.getHost() == null
                    || uri.getRawQuery() != null
                    || uri.getRawFragment() != null) {
                throw new IllegalArgumentException("SEO 基础地址必须是绝对 HTTP 或 HTTPS 地址");
            }
            return uri.toString();
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException("SEO 基础地址格式无效", ex);
        }
    }

    private static boolean isHttpScheme(String scheme) {
        return scheme != null && ("http".equals(scheme.toLowerCase(Locale.ROOT))
                || "https".equals(scheme.toLowerCase(Locale.ROOT)));
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (!isBlank(value)) {
                return value;
            }
        }
        throw new IllegalArgumentException("候选文本不能为空");
    }

    private static String requireText(String value, String message) {
        if (isBlank(value)) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
