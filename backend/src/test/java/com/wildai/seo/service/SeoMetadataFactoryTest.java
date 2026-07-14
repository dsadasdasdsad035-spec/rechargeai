package com.wildai.seo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wildai.common.config.WildAiProperties;
import com.wildai.content.dto.ArticlePublicDto;
import com.wildai.content.dto.ArticlePublicSummaryDto;
import com.wildai.product.dto.ProductDetailDto;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SeoMetadataFactoryTest {

    private static final String BASE_URL = "https://rechargeai.cn";
    private static final String SITE_NAME = "RechargeAi";
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void createsArticleMetadataAndEscapesJsonLdForHtmlEmbedding() throws Exception {
        var publishedAt = Instant.parse("2026-07-01T08:30:00Z");
        var updatedAt = Instant.parse("2026-07-02T09:45:00Z");
        var article = new ArticlePublicDto(
                1L,
                "标题</script>",
                "safe-slug",
                "自动摘要",
                null,
                "<p>文章正文</p>",
                "人工 SEO 标题",
                "人工 SEO 描述",
                publishedAt,
                updatedAt);

        var metadata = factory().forArticle(article);

        assertThat(metadata.title()).isEqualTo("人工 SEO 标题 | RechargeAi");
        assertThat(metadata.description()).isEqualTo("人工 SEO 描述");
        assertThat(metadata.canonical()).isEqualTo("https://rechargeai.cn/articles/safe-slug");
        assertThat(metadata.type()).isEqualTo("article");
        assertThat(metadata.robots()).isEqualTo("index,follow");
        assertThat(metadata.imageUrl()).isNull();
        assertThat(metadata.jsonLd())
                .doesNotContain("</script>")
                .contains("\\u003c/script\\u003e");

        var root = objectMapper.readTree(metadata.jsonLd());
        var articleNode = graphNode(root, "Article");
        var breadcrumbNode = graphNode(root, "BreadcrumbList");
        assertThat(articleNode.path("headline").asText()).isEqualTo("标题</script>");
        assertThat(articleNode.path("description").asText()).isEqualTo("人工 SEO 描述");
        assertThat(articleNode.path("url").asText()).isEqualTo(metadata.canonical());
        assertThat(articleNode.path("datePublished").asText()).isEqualTo(publishedAt.toString());
        assertThat(articleNode.path("dateModified").asText()).isEqualTo(updatedAt.toString());
        assertThat(articleNode.path("publisher").path("@type").asText()).isEqualTo("Organization");
        assertThat(articleNode.path("publisher").path("name").asText()).isEqualTo(SITE_NAME);
        assertThat(articleNode.has("author")).isFalse();
        assertThat(articleNode.has("aggregateRating")).isFalse();
        assertThat(articleNode.has("rating")).isFalse();
        assertThat(breadcrumbNode.path("itemListElement")).hasSize(3);
    }

    @Test
    void fallsBackToBusinessTextAndDoesNotSplitEmojiDuringDescriptionTruncation() {
        var summary = "摘 \n\t要" + "a".repeat(155) + " \n\t" + "😀" + "结尾";
        var article = article("普通标题", "safe-slug", summary, null, "  ", "\n");

        var metadata = factory().forArticle(article);

        assertThat(metadata.title()).isEqualTo("普通标题 | RechargeAi");
        assertThat(metadata.description())
                .hasSize(158)
                .doesNotContain("  ", "\n", "\t")
                .startsWith("摘 要")
                .doesNotEndWith(" ")
                .doesNotContain("😀");
        assertThat(hasUnpairedSurrogate(metadata.description())).isFalse();
    }

    @Test
    void reportsJsonSerializationFailureAsChineseInternalError() {
        assertThatThrownBy(() -> new StructuredDataService(objectMapper).toSafeJson(new Object()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("结构化数据序列化失败");
    }

    @Test
    void doesNotDuplicateSiteSuffix() {
        var article = article(
                "普通标题",
                "safe-slug",
                "摘要",
                null,
                "人工标题 | RechargeAi",
                "描述");

        assertThat(factory().forArticle(article).title()).isEqualTo("人工标题 | RechargeAi");
    }

    @Test
    void collapsesRepeatedSiteSuffixesToExactlyOne() {
        for (String seoTitle : List.of(
                "人工标题 | RechargeAi | RechargeAi",
                "人工标题 | RechargeAi | RechargeAi | RechargeAi")) {
            var article = article(
                    "普通标题",
                    "safe-slug",
                    "摘要",
                    null,
                    seoTitle,
                    "描述");

            assertThat(factory().forArticle(article).title()).isEqualTo("人工标题 | RechargeAi");
        }
    }

    @Test
    void rejectsTitleMadeOnlyOfSiteSuffixes() {
        var article = article(
                "普通标题",
                "safe-slug",
                "摘要",
                null,
                " | RechargeAi | RechargeAi",
                "描述");

        assertThatThrownBy(() -> factory().forArticle(article))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("页面标题移除站点后缀后不能为空");
    }

    @Test
    void resolvesRelativeArticleImageAndKeepsAbsoluteHttpsImage() throws Exception {
        var relative = factory().forArticle(article(
                "标题", "relative-image", "摘要", "/api/article-assets/a.png", null, null));
        var absolute = factory().forArticle(article(
                "标题", "absolute-image", "摘要", "https://cdn.example.com/a.png", null, null));

        assertThat(relative.imageUrl()).isEqualTo("https://rechargeai.cn/api/article-assets/a.png");
        assertThat(graphNode(objectMapper.readTree(relative.jsonLd()), "Article").path("image").asText())
                .isEqualTo(relative.imageUrl());
        assertThat(absolute.imageUrl()).isEqualTo("https://cdn.example.com/a.png");
        assertThat(graphNode(objectMapper.readTree(absolute.jsonLd()), "Article").path("image").asText())
                .isEqualTo(absolute.imageUrl());
    }

    @Test
    void keepsBaseUrlSubpathWhenResolvingDirectoryRelativeArticleImage() {
        var metadata = factory("https://example.com/app/", SITE_NAME).forArticle(article(
                "标题", "safe-slug", "摘要", "images/a.png", null, null));
        var rootRelative = factory("https://example.com/app/", SITE_NAME).forArticle(article(
                "标题", "root-relative", "摘要", "/api/article-assets/a.png", null, null));

        assertThat(metadata.canonical()).isEqualTo("https://example.com/app/articles/safe-slug");
        assertThat(metadata.imageUrl()).isEqualTo("https://example.com/app/images/a.png");
        assertThat(rootRelative.imageUrl()).isEqualTo("https://example.com/api/article-assets/a.png");
    }

    @Test
    void ignoresUnsafeOrMissingArticleImagesWithoutInventingFallback() throws Exception {
        for (String unsafe : List.of(
                "javascript:alert(1)",
                "data:image/png;base64,AAAA",
                "file:///tmp/a.png",
                "//evil.example/a.png",
                "   ")) {
            var metadata = factory().forArticle(article(
                    "标题", "unsafe-image", "摘要", unsafe, null, null));

            assertThat(metadata.imageUrl()).isNull();
            assertThat(graphNode(objectMapper.readTree(metadata.jsonLd()), "Article").has("image")).isFalse();
        }
    }

    @Test
    void createsArticleItemListWithoutArticleBodies() throws Exception {
        var articles = List.of(
                summary(1L, "第一篇", "first"),
                summary(2L, "第二篇", "second"));

        var metadata = factory().forArticleList(articles);

        assertThat(metadata.title()).isEqualTo("文章中心 | RechargeAi");
        assertThat(metadata.canonical()).isEqualTo("https://rechargeai.cn/articles");
        assertThat(metadata.type()).isEqualTo("website");
        assertThat(metadata.robots()).isEqualTo("index,follow");
        assertThat(metadata.imageUrl()).isNull();
        var root = objectMapper.readTree(metadata.jsonLd());
        assertThat(root.path("@type").asText()).isEqualTo("ItemList");
        var elements = root.path("itemListElement");
        assertThat(elements).hasSize(2);
        assertListItem(elements.get(0), 1, "第一篇", "https://rechargeai.cn/articles/first");
        assertListItem(elements.get(1), 2, "第二篇", "https://rechargeai.cn/articles/second");
        assertThat(metadata.jsonLd()).doesNotContain("正文", "contentHtml", "summary");
    }

    @Test
    void createsProductGraphWithRealOfferAndNoFabricatedReviewData() throws Exception {
        var product = product(42L, "ChatGPT Plus", "OPENAI", "19.90", "CNY", "ON_SHELF");

        var metadata = factory().forProduct(product);

        assertThat(metadata.title()).isEqualTo("ChatGPT Plus | RechargeAi");
        assertThat(metadata.description()).contains("ChatGPT Plus", "OPENAI");
        assertThat(metadata.canonical()).isEqualTo("https://rechargeai.cn/products/42");
        assertThat(metadata.type()).isEqualTo("product");
        assertThat(metadata.imageUrl()).isNull();
        assertThat(metadata.robots()).isEqualTo("index,follow");
        var root = objectMapper.readTree(metadata.jsonLd());
        var productNode = graphNode(root, "Product");
        var breadcrumbNode = graphNode(root, "BreadcrumbList");
        assertThat(productNode.path("name").asText()).isEqualTo("ChatGPT Plus");
        assertThat(productNode.path("url").asText()).isEqualTo(metadata.canonical());
        assertThat(productNode.path("offers").path("price").decimalValue())
                .isEqualByComparingTo("19.90");
        assertThat(productNode.path("offers").path("priceCurrency").asText()).isEqualTo("CNY");
        assertThat(productNode.path("offers").path("availability").asText())
                .isEqualTo("https://schema.org/InStock");
        assertThat(productNode.has("image")).isFalse();
        assertThat(productNode.has("aggregateRating")).isFalse();
        assertThat(productNode.has("review")).isFalse();
        assertThat(breadcrumbNode.path("itemListElement")).hasSize(3);
    }

    @Test
    void mapsEveryNonShelfProductToOutOfStock() throws Exception {
        var metadata = factory().forProduct(
                product(43L, "下架产品", "OTHER", "9.90", "USD", "OFF_SHELF"));

        var productNode = graphNode(objectMapper.readTree(metadata.jsonLd()), "Product");
        assertThat(productNode.path("offers").path("availability").asText())
                .isEqualTo("https://schema.org/OutOfStock");
    }

    @Test
    void rejectsNonPositiveProductSalePrice() {
        for (String salePrice : List.of("0", "-0.01")) {
            assertThatThrownBy(() -> factory().forProduct(
                    product(43L, "无效价格产品", "OTHER", salePrice, "CNY", "ON_SHELF")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("售价必须大于 0");
        }
    }

    @Test
    void rejectsNonIso4217ProductCurrency() {
        assertThatThrownBy(() -> factory().forProduct(
                product(43L, "无效币种产品", "OTHER", "9.90", "INVALID", "ON_SHELF")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("币种必须为 ISO 4217 三字母代码");
    }

    @Test
    void rejectsUnknownProductStatus() {
        assertThatThrownBy(() -> factory().forProduct(
                product(43L, "无效状态产品", "OTHER", "9.90", "CNY", "UNKNOWN")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("产品状态无效");
    }

    @Test
    void productListUsesTheSameOfferValidation() {
        var invalidProduct = product(43L, "无效列表产品", "OTHER", "0", "CNY", "ON_SHELF");

        assertThatThrownBy(() -> factory().forProductList(List.of(invalidProduct)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("售价必须大于 0");
    }

    @Test
    void normalizesProductCurrencyInOfferAndDescription() throws Exception {
        var metadata = factory().forProduct(
                product(43L, "美元产品", "OTHER", "9.90", " usd ", "ON_SHELF"));

        var productNode = graphNode(objectMapper.readTree(metadata.jsonLd()), "Product");
        assertThat(productNode.path("offers").path("priceCurrency").asText()).isEqualTo("USD");
        assertThat(metadata.description()).contains("9.90 USD").doesNotContain("usd");
    }

    @Test
    void createsProductItemListInInputOrder() throws Exception {
        var products = List.of(
                product(7L, "产品乙", "TYPE_B", "7.00", "CNY", "ON_SHELF"),
                product(3L, "产品甲", "TYPE_A", "3.00", "USD", "OFF_SHELF"));

        var metadata = factory().forProductList(products);

        assertThat(metadata.title()).isEqualTo("AI 订阅产品 | RechargeAi");
        assertThat(metadata.canonical()).isEqualTo("https://rechargeai.cn/products");
        assertThat(metadata.type()).isEqualTo("website");
        var elements = objectMapper.readTree(metadata.jsonLd()).path("itemListElement");
        assertThat(elements).hasSize(2);
        assertListItem(elements.get(0), 1, "产品乙", "https://rechargeai.cn/products/7");
        assertListItem(elements.get(1), 2, "产品甲", "https://rechargeai.cn/products/3");
    }

    @Test
    void normalizesTrailingSlashAndRejectsInvalidSeoConfiguration() {
        assertThat(factory("https://rechargeai.cn/", SITE_NAME)
                .forArticle(article("标题", "safe-slug", "摘要", null, null, null))
                .canonical())
                .isEqualTo("https://rechargeai.cn/articles/safe-slug");

        for (String invalidBaseUrl : new String[]{null, "", "  ", "/relative", "ftp://example.com"}) {
            assertThatThrownBy(() -> factory(invalidBaseUrl, SITE_NAME))
                    .isInstanceOf(IllegalArgumentException.class);
        }
        for (String invalidSiteName : new String[]{null, "", "  "}) {
            assertThatThrownBy(() -> factory(BASE_URL, invalidSiteName))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    void rejectsMissingDtosAndCanonicalIdentifiers() {
        var factory = factory();

        assertThatThrownBy(() -> factory.forArticle(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> factory.forArticle(
                article("标题", "../unsafe", "摘要", null, null, null)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> factory.forProduct(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> factory.forProduct(
                product(null, "产品", "TYPE", "1.00", "CNY", "ON_SHELF")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> factory.forArticleList(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> factory.forProductList(null)).isInstanceOf(IllegalArgumentException.class);
    }

    private SeoMetadataFactory factory() {
        return factory(BASE_URL, SITE_NAME);
    }

    private SeoMetadataFactory factory(String baseUrl, String siteName) {
        var properties = new WildAiProperties();
        properties.getSeo().setBaseUrl(baseUrl);
        properties.getSeo().setSiteName(siteName);
        return new SeoMetadataFactory(properties, new StructuredDataService(objectMapper));
    }

    private ArticlePublicDto article(
            String title,
            String slug,
            String summary,
            String coverImageUrl,
            String seoTitle,
            String seoDescription) {
        return new ArticlePublicDto(
                1L,
                title,
                slug,
                summary,
                coverImageUrl,
                "<p>正文</p>",
                seoTitle,
                seoDescription,
                Instant.parse("2026-07-01T08:30:00Z"),
                Instant.parse("2026-07-02T09:45:00Z"));
    }

    private ArticlePublicSummaryDto summary(Long id, String title, String slug) {
        return new ArticlePublicSummaryDto(
                id,
                title,
                slug,
                "列表摘要",
                null,
                Instant.parse("2026-07-01T08:30:00Z"),
                Instant.parse("2026-07-02T09:45:00Z"));
    }

    private ProductDetailDto product(
            Long id,
            String name,
            String serviceType,
            String salePrice,
            String currency,
            String status) {
        return new ProductDetailDto(
                id,
                "PRODUCT_" + id,
                name,
                serviceType,
                new BigDecimal("29.90"),
                new BigDecimal(salePrice),
                currency,
                30,
                status,
                "{}",
                2,
                "退款政策",
                "合规提示",
                null);
    }

    private JsonNode graphNode(JsonNode root, String type) {
        return StreamSupport.stream(root.path("@graph").spliterator(), false)
                .filter(node -> type.equals(node.path("@type").asText()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("缺少结构化数据节点：" + type));
    }

    private void assertListItem(JsonNode node, int position, String name, String url) {
        assertThat(node.path("@type").asText()).isEqualTo("ListItem");
        assertThat(node.path("position").asInt()).isEqualTo(position);
        assertThat(node.path("name").asText()).isEqualTo(name);
        assertThat(node.path("url").asText()).isEqualTo(url);
        assertThat(node.has("description")).isFalse();
        assertThat(node.has("content")).isFalse();
    }

    private boolean hasUnpairedSurrogate(String value) {
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (Character.isHighSurrogate(current)) {
                if (index + 1 >= value.length() || !Character.isLowSurrogate(value.charAt(index + 1))) {
                    return true;
                }
                index++;
            } else if (Character.isLowSurrogate(current)) {
                return true;
            }
        }
        return false;
    }
}
