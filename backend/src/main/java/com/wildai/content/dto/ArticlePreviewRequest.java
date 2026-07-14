package com.wildai.content.dto;

import jakarta.validation.constraints.Size;

public record ArticlePreviewRequest(
        @Size(
                max = ArticleSaveRequest.MAX_CONTENT_MARKDOWN_LENGTH,
                message = "文章正文不能超过 200000 个字符")
        String contentMarkdown) {
}
