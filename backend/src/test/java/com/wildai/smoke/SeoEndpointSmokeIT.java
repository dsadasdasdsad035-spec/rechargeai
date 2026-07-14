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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SeoEndpointSmokeIT extends BaseSmokeIT {

    @Test
    void sitemapOnlyContainsPublicContentAndRefreshesAfterWithdrawal() throws Exception {
        String adminToken = adminLogin();
        long productId = createAndShelfProduct(adminToken, "站点地图套餐", DEFAULT_SALE_PRICE);
        long articleId = createDraftArticle(adminToken, "站点地图文章", "seo-sitemap");

        mockMvc.perform(get("/sitemap.xml"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_XML))
                .andExpect(content().string(containsString("/products/" + productId)))
                .andExpect(content().string(not(containsString("/articles/seo-sitemap"))))
                .andExpect(content().string(containsString("<lastmod>")));

        mockMvc.perform(post("/admin/api/articles/" + articleId + "/publish")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/sitemap.xml"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/articles/seo-sitemap")));

        mockMvc.perform(post("/admin/api/articles/" + articleId + "/withdraw")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/sitemap.xml"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("/articles/seo-sitemap"))));
    }

    @Test
    void robotsDeclaresCanonicalSitemapAndPrivateRoutes() throws Exception {
        mockMvc.perform(get("/robots.txt"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                .andExpect(content().string(containsString(
                        "Sitemap: https://rechargeai.cn/sitemap.xml")))
                .andExpect(content().string(containsString("Disallow: /admin/")))
                .andExpect(content().string(containsString("Disallow: /transaction-record")));
    }

    private long createDraftArticle(String adminToken, String title, String slug) throws Exception {
        var result = mockMvc.perform(post("/admin/api/articles")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", title,
                                "slug", slug,
                                "summary", "用于验证动态站点地图",
                                "contentMarkdown", "# " + title,
                                "seoTitle", "",
                                "seoDescription", ""))))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
        return data.path("id").asLong();
    }
}
