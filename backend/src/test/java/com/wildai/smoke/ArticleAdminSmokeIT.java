package com.wildai.smoke;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ArticleAdminSmokeIT extends BaseSmokeIT {

    private static final Path ARTICLE_UPLOAD_DIR = Path.of(
            System.getProperty("java.io.tmpdir"), "wildai-article-assets-" + UUID.randomUUID());

    @DynamicPropertySource
    static void articleAssetProperties(DynamicPropertyRegistry registry) {
        registry.add("wildai.article.upload-dir", ARTICLE_UPLOAD_DIR::toString);
    }

    @AfterAll
    static void cleanArticleUploadDir() throws IOException {
        if (!Files.exists(ARTICLE_UPLOAD_DIR)) {
            return;
        }
        try (var paths = Files.walk(ARTICLE_UPLOAD_DIR)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException exception) {
                    throw new IllegalStateException("清理文章图片测试目录失败", exception);
                }
            });
        }
    }

    @Test
    void articleManagementCompletesFullLifecycle() throws Exception {
        String adminToken = adminLogin();
        String slug = "seo-guide-" + System.currentTimeMillis();

        var created = mockMvc.perform(post("/admin/api/articles")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(articleJson("SEO 指南", slug, "# SEO 指南\n正文")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andReturn();

        long articleId = objectMapper.readTree(created.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        var listResult = mockMvc.perform(get("/admin/api/articles")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("title", "SEO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andReturn();

        JsonNode items = objectMapper.readTree(listResult.getResponse().getContentAsString())
                .path("data").path("items");
        assertThat(items).anySatisfy(item -> assertThat(item.path("id").asLong()).isEqualTo(articleId));

        mockMvc.perform(get("/admin/api/articles/" + articleId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.slug").value(slug))
                .andExpect(jsonPath("$.data.contentMarkdown").value("# SEO 指南\n正文"));

        mockMvc.perform(put("/admin/api/articles/" + articleId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(articleJson("SEO 指南更新", slug, "# SEO 指南更新\n新正文")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("SEO 指南更新"))
                .andExpect(jsonPath("$.data.slug").value(slug))
                .andExpect(jsonPath("$.data.contentMarkdown").value("# SEO 指南更新\n新正文"))
                .andExpect(jsonPath("$.data.contentHtml").value(containsString("<h1>SEO 指南更新</h1>")))
                .andExpect(jsonPath("$.data.contentHtml").value(containsString("<p>新正文</p>")));

        mockMvc.perform(post("/admin/api/articles/preview")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "contentMarkdown", "# 预览<script>alert(1)</script>"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.html").value(containsString("<h1>预览</h1>")))
                .andExpect(jsonPath("$.data.html").value(not(containsString("<script>"))));

        mockMvc.perform(post("/admin/api/articles/" + articleId + "/publish")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"));

        mockMvc.perform(post("/admin/api/articles/" + articleId + "/withdraw")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DRAFT"));

        mockMvc.perform(delete("/admin/api/articles/" + articleId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"));

        mockMvc.perform(get("/admin/api/articles/" + articleId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("404"))
                .andExpect(jsonPath("$.message").value("文章不存在"));
    }

    @Test
    void articleManagementRequiresAdminAuthentication() throws Exception {
        mockMvc.perform(get("/admin/api/articles"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("401"));

        mockMvc.perform(post("/admin/api/articles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(articleJson("SEO 指南", "unauthorized-seo", "# SEO 指南")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("401"));
    }

    @Test
    void articleAssetUploadEnforcesContentPermissionAndServesPublicImage() throws Exception {
        byte[] png = new byte[] {
                (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00
        };

        mockMvc.perform(multipart("/admin/api/article-assets")
                        .file("file", png))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("401"));

        String username = "asset-audit-" + System.currentTimeMillis();
        String password = "Audit123456";
        String superToken = adminLogin();
        AdminSession auditAdmin = createSuperAdmin(username, password);
        mockMvc.perform(put("/admin/api/admins/" + auditAdmin.adminId() + "/roles")
                        .header("Authorization", "Bearer " + superToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roleCodes\":[\"AUDIT_READONLY\"]}"))
                .andExpect(status().isOk());

        String auditToken = adminLogin(username, password);
        mockMvc.perform(multipart("/admin/api/article-assets")
                        .file("file", png)
                        .header("Authorization", "Bearer " + auditToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("403"))
                .andExpect(jsonPath("$.message").value("无权管理文章内容"));

        var uploadResult = mockMvc.perform(multipart("/admin/api/article-assets")
                        .file(new MockMultipartFile("file", "cover.png", "image/png", png))
                        .header("Authorization", "Bearer " + superToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.storedName").value(
                        org.hamcrest.Matchers.matchesPattern("[0-9a-f]{32}\\.png")))
                .andExpect(jsonPath("$.data.contentType").value("image/png"))
                .andReturn();

        String url = objectMapper.readTree(uploadResult.getResponse().getContentAsString())
                .path("data").path("url").asText();
        mockMvc.perform(get(url))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(content().bytes(png))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("Cache-Control", containsString("max-age=2592000")));
    }

    @Test
    void createArticleValidatesTitle() throws Exception {
        String adminToken = adminLogin();

        mockMvc.perform(post("/admin/api/articles")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(articleJson("", "invalid-title-" + System.currentTimeMillis(), "# 正文")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("400"))
                .andExpect(jsonPath("$.message").value("请填写文章标题"));
    }

    @Test
    void auditReadonlyAdminCannotCreateArticle() throws Exception {
        String username = "article-audit-" + System.currentTimeMillis();
        String password = "Audit123456";
        String superToken = adminLogin();
        AdminSession auditAdmin = createSuperAdmin(username, password);

        mockMvc.perform(put("/admin/api/admins/" + auditAdmin.adminId() + "/roles")
                        .header("Authorization", "Bearer " + superToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roleCodes\":[\"AUDIT_READONLY\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.roleCodes[0]").value("AUDIT_READONLY"));

        String auditToken = adminLogin(username, password);
        mockMvc.perform(post("/admin/api/articles")
                        .header("Authorization", "Bearer " + auditToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(articleJson(
                                "只读审计文章",
                                "audit-readonly-" + System.currentTimeMillis(),
                                "# 无权创建")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("403"))
                .andExpect(jsonPath("$.message").value("无权管理文章内容"));
    }

    @Test
    void malformedArticleRequestsReturnBadRequest() throws Exception {
        String adminToken = adminLogin();

        assertAll("客户端输入错误统一返回 400",
                () -> mockMvc.perform(get("/admin/api/articles")
                                .header("Authorization", "Bearer " + adminToken)
                                .param("status", "UNKNOWN"))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.code").value("400"))
                        .andExpect(jsonPath("$.message").value("请求参数格式错误")),
                () -> mockMvc.perform(get("/admin/api/articles")
                                .header("Authorization", "Bearer " + adminToken)
                                .param("pageNo", "abc"))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.code").value("400"))
                        .andExpect(jsonPath("$.message").value("请求参数格式错误")),
                () -> mockMvc.perform(get("/admin/api/articles/preview")
                                .header("Authorization", "Bearer " + adminToken))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.code").value("400"))
                        .andExpect(jsonPath("$.message").value("请求参数格式错误")),
                () -> mockMvc.perform(post("/admin/api/articles")
                                .header("Authorization", "Bearer " + adminToken)
                                .contentType(MediaType.APPLICATION_JSON))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.code").value("400"))
                        .andExpect(jsonPath("$.message").value("请求参数格式错误")),
                () -> mockMvc.perform(post("/admin/api/articles")
                                .header("Authorization", "Bearer " + adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{not-json"))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.code").value("400"))
                        .andExpect(jsonPath("$.message").value("请求参数格式错误")),
                () -> mockMvc.perform(post("/admin/api/articles/preview")
                                .header("Authorization", "Bearer " + adminToken)
                                .contentType(MediaType.APPLICATION_JSON))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.code").value("400"))
                        .andExpect(jsonPath("$.message").value("请求参数格式错误")));
    }

    @Test
    void missingAdminRouteReturnsNotFound() throws Exception {
        String adminToken = adminLogin();

        mockMvc.perform(get("/admin/api/articles/missing/path")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("404"))
                .andExpect(jsonPath("$.message").value("资源不存在"));
    }

    @Test
    void articleMarkdownRejectsOversizedContent() throws Exception {
        String adminToken = adminLogin();
        String oversizedContent = "正".repeat(200_001);

        assertAll("保存和预览均拒绝超长正文",
                () -> mockMvc.perform(post("/admin/api/articles")
                                .header("Authorization", "Bearer " + adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(articleJson(
                                        "超长正文",
                                        "oversized-content-" + System.currentTimeMillis(),
                                        oversizedContent)))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.code").value("400"))
                        .andExpect(jsonPath("$.message").value("文章正文不能超过 200000 个字符")),
                () -> mockMvc.perform(post("/admin/api/articles/preview")
                                .header("Authorization", "Bearer " + adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(Map.of(
                                        "contentMarkdown", oversizedContent))))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.code").value("400"))
                        .andExpect(jsonPath("$.message").value("文章正文不能超过 200000 个字符")));
    }

    private String articleJson(String title, String slug, String contentMarkdown) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "title", title,
                "slug", slug,
                "summary", "",
                "contentMarkdown", contentMarkdown,
                "seoTitle", "",
                "seoDescription", ""));
    }
}
