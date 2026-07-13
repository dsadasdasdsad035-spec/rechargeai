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

    @Test
    void keepsRelativeUrlsAndRejectsUnsafeImageProtocols() {
        var content = service.render("""
                [站内文章](/articles/a)
                ![文章图片](/api/article-assets/a.png)
                ![本地图片](file:///tmp/a.png)
                ![内嵌图片](data:image/png;base64,AAAA)
                """, "");

        assertThat(content.html())
                .contains("href=\"/articles/a\"", "src=\"/api/article-assets/a.png\"")
                .doesNotContain("src=\"file:", "src=\"data:");
    }

    @Test
    void avoidsSplittingEmojiAtSummaryLimits() {
        var manual = service.render("正文", "a".repeat(499) + "😀");
        var generated = service.render("a".repeat(219) + "😀", "");

        assertThat(manual.summary()).hasSizeLessThanOrEqualTo(500);
        assertThat(generated.summary()).hasSizeLessThanOrEqualTo(220);
        assertThat(hasUnpairedSurrogate(manual.summary())).isFalse();
        assertThat(hasUnpairedSurrogate(generated.summary())).isFalse();
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
