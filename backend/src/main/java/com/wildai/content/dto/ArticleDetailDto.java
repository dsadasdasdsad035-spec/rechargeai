package com.wildai.content.dto;

import com.wildai.content.domain.ArticleStatus;

import java.time.Instant;

public record ArticleDetailDto(
        Long id,
        String title,
        String slug,
        String summary,
        String coverImageUrl,
        String contentMarkdown,
        String contentHtml,
        String seoTitle,
        String seoDescription,
        ArticleStatus status,
        Instant publishedAt,
        Instant createdAt,
        Instant updatedAt) {
}
