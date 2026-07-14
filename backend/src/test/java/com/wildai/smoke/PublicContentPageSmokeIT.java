package com.wildai.smoke;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PublicContentPageSmokeIT extends BaseSmokeIT {

    @Test
    void publishedArticleIsRenderedInFirstHtmlAndPrivateArticlesStayHidden() throws Exception {
        String adminToken = adminLogin();
        long articleId = createDraftArticle(adminToken, "SEO 指南", "seo-guide", "# SEO 指南\n公开正文");

        assertArticleNotFound("/articles/seo-guide", "SEO 指南");

        mockMvc.perform(post("/admin/api/articles/" + articleId + "/publish")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/articles/seo-guide"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("<h1>SEO 指南</h1>")))
                .andExpect(content().string(containsString("公开正文")))
                .andExpect(content().string(containsString(
                        "<link rel=\"canonical\" href=\"https://rechargeai.cn/articles/seo-guide\"")))
                .andExpect(content().string(containsString("application/ld+json")))
                .andExpect(content().string(not(containsString("id=\"app\""))));

        mockMvc.perform(get("/articles"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("SEO 指南")));

        mockMvc.perform(post("/admin/api/articles/" + articleId + "/withdraw")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        assertArticleNotFound("/articles/seo-guide", "SEO 指南");
        assertArticleNotFound("/articles/not-published", "SEO 指南");
    }

    @Test
    void onlyOnShelfProductsAreRenderedAndRootRedirectsPermanently() throws Exception {
        String adminToken = adminLogin();
        long onShelfId = createAndShelfProduct(adminToken, "主流浏览器套餐", DEFAULT_SALE_PRICE);
        long offShelfId = createAndShelfProduct(adminToken, "下架保密套餐", DEFAULT_SALE_PRICE);

        mockMvc.perform(post("/admin/api/products/" + offShelfId + "/shelf")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"OFF_SHELF\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("主流浏览器套餐")))
                .andExpect(content().string(not(containsString("下架保密套餐"))));

        mockMvc.perform(get("/products/" + onShelfId))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("主流浏览器套餐")))
                .andExpect(content().string(containsString("href=\"/checkout/" + onShelfId + "\"")));

        assertProductNotFound(offShelfId, "下架保密套餐");
        assertProductNotFound(Long.MAX_VALUE, "下架保密套餐");

        mockMvc.perform(get("/"))
                .andExpect(status().isMovedPermanently())
                .andExpect(header().string("Location", "/products"));
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
        mockMvc.perform(get(path))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("页面不存在")))
                .andExpect(content().string(not(containsString(privateText))));
    }

    private void assertProductNotFound(long productId, String privateText) throws Exception {
        mockMvc.perform(get("/products/" + productId))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("页面不存在")))
                .andExpect(content().string(not(containsString(privateText))));
    }
}
