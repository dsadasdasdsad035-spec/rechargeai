package com.wildai.content.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ArticleMarkdownServiceTest {

    private final ArticleMarkdownService service = new ArticleMarkdownService();

    @Test
    void rendersTablesAndRemovesExecutableContent() {
        var content = service.render("""
                # 标题
                | 名称 | 价格 |
                | --- | --- |
                | Plus | 20 |
                <script>alert(1)</script>
                [危险](javascript:alert(1))
                [官网](https://rechargeai.cn)
                """, "");

        assertThat(content.html()).contains("<table>", "<h1>标题</h1>")
                .doesNotContain("script", "javascript:")
                .contains("rel=\"noopener noreferrer\"");
        assertThat(content.summary()).startsWith("标题 名称 价格");
    }

    @Test
    void keepsManualSummaryWithinDatabaseLimit() {
        String summary = " 摘要 ".repeat(200);

        var content = service.render("正文", summary);

        assertThat(content.summary()).hasSizeLessThanOrEqualTo(500);
    }
}
