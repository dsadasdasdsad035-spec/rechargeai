package com.wildai.content.dto;

import java.time.Instant;

public record ArticlePublicDto(
        Long id,
        String title,
        String slug,
        String summary,
        String coverImageUrl,
        String contentHtml,
        String seoTitle,
        String seoDescription,
        Instant publishedAt,
        Instant updatedAt) {
}
