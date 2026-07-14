package com.wildai.smoke;

import com.fasterxml.jackson.databind.JsonNode;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PublicContentPageSmokeIT extends BaseSmokeIT {

    @Test
    void publishedArticleUsesBrandStructureAndPrivateArticlesStayHidden() throws Exception {
        String adminToken = adminLogin();
        long articleId = createDraftArticle(adminToken, "SEO 指南", "seo-guide", "# SEO 指南\n公开正文");

        assertArticleNotFound("/articles/seo-guide", "SEO 指南");

        mockMvc.perform(post("/admin/api/articles/" + articleId + "/publish")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        var articleDetail = mockMvc.perform(get("/articles/seo-guide"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("<h1>SEO 指南</h1>")))
                .andExpect(content().string(containsString("公开正文")))
                .andExpect(content().string(containsString(
                        "<link rel=\"canonical\" href=\"https://rechargeai.cn/articles/seo-guide\"")))
                .andExpect(content().string(containsString("application/ld+json")))
                .andExpect(content().string(not(containsString("id=\"app\""))))
                .andReturn();

        assertPublishedArticleBrandStructure(articleDetail.getResponse()
                .getContentAsString(StandardCharsets.UTF_8));

        var articleList = mockMvc.perform(get("/articles"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("SEO 指南")))
                .andReturn();

        assertPublishedArticleListStructure(articleList.getResponse()
                .getContentAsString(StandardCharsets.UTF_8));

        mockMvc.perform(post("/admin/api/articles/" + articleId + "/withdraw")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        assertArticleNotFound("/articles/seo-guide", "SEO 指南");
        assertArticleNotFound("/articles/not-published", "SEO 指南");
    }

    @Test
    void onlyOnShelfProductsUseVisualStructureAndRootRedirectsPermanently() throws Exception {
        String adminToken = adminLogin();
        long onShelfId = createAndShelfProduct(adminToken, "主流浏览器套餐", DEFAULT_SALE_PRICE);
        long offShelfId = createAndShelfProduct(adminToken, "下架保密套餐", DEFAULT_SALE_PRICE);
        long detailedProductId = createAndShelfProductWithPublicInformation(
                adminToken,
                "完整信息套餐",
                6,
                "未履约可全额退款",
                "请勿提供第三方账户密码");

        mockMvc.perform(post("/admin/api/products/" + offShelfId + "/shelf")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"OFF_SHELF\"}"))
                .andExpect(status().isOk());

        var productList = mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("主流浏览器套餐")))
                .andExpect(content().string(not(containsString("下架保密套餐"))))
                .andReturn();

        assertProductListVisualStructure(productList.getResponse()
                .getContentAsString(StandardCharsets.UTF_8));

        var productDetail = mockMvc.perform(get("/products/" + onShelfId))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("主流浏览器套餐")))
                .andExpect(content().string(containsString("href=\"/checkout/" + onShelfId + "\"")))
                .andReturn();

        assertProductDetailWithoutOptionalInformation(
                productDetail.getResponse().getContentAsString(StandardCharsets.UTF_8),
                onShelfId);

        var detailedProduct = mockMvc.perform(get("/products/" + detailedProductId))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("完整信息套餐")))
                .andReturn();

        assertProductDetailWithPublicInformation(
                detailedProduct.getResponse().getContentAsString(StandardCharsets.UTF_8),
                detailedProductId);

        assertProductNotFound(offShelfId, "下架保密套餐");
        assertProductNotFound(Long.MAX_VALUE, "下架保密套餐");

        mockMvc.perform(get("/"))
                .andExpect(status().isMovedPermanently())
                .andExpect(header().string("Location", "/products"));
    }

    private void assertPublishedArticleBrandStructure(String html) {
        Document document = Jsoup.parse(html);
        Element logoMark = document.selectFirst("header .logo .logo__mark");
        Element logoText = document.selectFirst("header .logo .logo__text");

        assertThat(logoMark).isNotNull();
        assertThat(logoMark.text()).isEqualTo("R");
        assertThat(logoText).isNotNull();
        assertThat(logoText.text()).isEqualTo("RechargeAi");
        assertThat(document.select("h1")).hasSize(1);
        assertThat(document.select(".article-body h2").eachText()).contains("SEO 指南");
    }

    private void assertPublishedArticleListStructure(String html) {
        Document document = Jsoup.parse(html);
        Element articleLink = document.selectFirst(
                "a.card-link[href='/articles/seo-guide']");

        assertThat(articleLink).isNotNull();
        Element articleCard = articleLink.closest("article.article-card");
        assertThat(articleCard).isNotNull();
        assertThat(articleLink.parent().tagName()).isEqualTo("h2");
        Element summary = articleCard.selectFirst("p.article-card__summary");
        assertThat(summary).isNotNull();
        assertThat(summary.text()).isEqualTo("适用于主流浏览器的 SEO 指南");
    }

    private void assertProductListVisualStructure(String html) {
        Document document = Jsoup.parse(html);
        Element logoMark = document.selectFirst("header .logo .logo__mark");
        Element heading = document.selectFirst(".page-heading h1");
        Element firstProductCard = document.select(".product-card").first();

        assertThat(logoMark).isNotNull();
        assertThat(logoMark.text()).isEqualTo("R");
        assertThat(heading).isNotNull();
        assertThat(heading.text()).isEqualTo("AI 订阅服务");
        assertThat(firstProductCard).isNotNull();

        Element callToAction = firstProductCard.selectFirst(".product-card__cta");
        assertThat(callToAction).isNotNull();
        assertThat(callToAction.text()).isEqualTo("查看详情 →");
    }

    private void assertProductDetailWithoutOptionalInformation(String html, long productId) {
        Document document = Jsoup.parse(html);

        assertThat(document.selectFirst(".info-card")).isNull();
        assertThat(document.selectFirst(
                "a.button.button--block[href='/checkout/" + productId + "']"))
                .isNotNull();
    }

    private void assertProductDetailWithPublicInformation(String html, long productId) {
        Document document = Jsoup.parse(html);
        Element infoList = document.selectFirst("section.info-card > dl.info-list");
        Element notice = document.selectFirst(".notice[role='note']");

        assertThat(infoList).isNotNull();
        assertThat(infoList.select("dt").eachText()).containsExactly("预计处理", "退款政策");
        assertThat(infoList.select("dd").eachText())
                .containsExactly("约 6 小时", "未履约可全额退款");
        assertThat(notice).isNotNull();
        assertThat(notice.text()).isEqualTo("请勿提供第三方账户密码");
        assertThat(document.selectFirst(
                "a.button.button--block[href='/checkout/" + productId + "']"))
                .isNotNull();
    }

    private long createAndShelfProductWithPublicInformation(
            String adminToken,
            String name,
            int estimatedHours,
            String refundPolicyText,
            String complianceNotice) throws Exception {
        var create = mockMvc.perform(post("/admin/api/products")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", name,
                                "salePrice", DEFAULT_SALE_PRICE,
                                "periodDays", 30,
                                "currency", "CNY",
                                "serviceType", "GENERAL",
                                "estimatedHours", estimatedHours,
                                "refundPolicyText", refundPolicyText,
                                "complianceNotice", complianceNotice))))
                .andExpect(status().isOk())
                .andReturn();
        long productId = objectMapper.readTree(create.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        mockMvc.perform(post("/admin/api/products/" + productId + "/shelf")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ON_SHELF\"}"))
                .andExpect(status().isOk());
        return productId;
    }

    private long createDraftArticle(
            String adminToken,
            String title,
            String slug,
            String contentMarkdown) throws Exception {
        var result = mockMvc.perform(post("/admin/api/articles")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", title,
                                "slug", slug,
                                "summary", "适用于主流浏览器的 SEO 指南",
                                "contentMarkdown", contentMarkdown,
                                "seoTitle", "",
                                "seoDescription", ""))))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
        return data.path("id").asLong();
    }

    private void assertArticleNotFound(String path, String privateText) throws Exception {
        var result = mockMvc.perform(get(path))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("页面不存在")))
                .andExpect(content().string(not(containsString(privateText))))
                .andReturn();

        assertPublicNotFoundStructure(result.getResponse()
                .getContentAsString(StandardCharsets.UTF_8));
    }

    private void assertProductNotFound(long productId, String privateText) throws Exception {
        var result = mockMvc.perform(get("/products/" + productId))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("页面不存在")))
                .andExpect(content().string(not(containsString(privateText))))
                .andReturn();

        assertPublicNotFoundStructure(result.getResponse()
                .getContentAsString(StandardCharsets.UTF_8));
    }

    private void assertPublicNotFoundStructure(String html) {
        Document document = Jsoup.parse(html);

        assertThat(document.selectFirst("article.error-card")).isNotNull();
        assertThat(document.selectFirst("a.button[href='/products']")).isNotNull();
        assertThat(document.selectFirst("header .logo .logo__mark")).isNotNull();
    }
}
