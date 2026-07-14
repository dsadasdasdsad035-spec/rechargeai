package com.wildai.content.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ArticleSaveRequest(
        @NotBlank(message = "请填写文章标题")
        @Size(max = 200, message = "文章标题不能超过 200 个字符")
        String title,
        @Size(max = 180, message = "链接标识不能超过 180 个字符")
        String slug,
        @Size(max = 500, message = "摘要不能超过 500 个字符")
        String summary,
        @Size(max = 512, message = "封面地址不能超过 512 个字符")
        String coverImageUrl,
        @Size(max = MAX_CONTENT_MARKDOWN_LENGTH, message = "文章正文不能超过 200000 个字符")
        String contentMarkdown,
        @Size(max = 200, message = "SEO 标题不能超过 200 个字符")
        String seoTitle,
        @Size(max = 320, message = "SEO 描述不能超过 320 个字符")
        String seoDescription) {

    public static final int MAX_CONTENT_MARKDOWN_LENGTH = 200_000;
}
