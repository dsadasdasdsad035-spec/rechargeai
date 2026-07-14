package com.wildai.content.dto;

import com.wildai.content.domain.ArticleStatus;

import java.time.Instant;

public record ArticleSummaryDto(
        Long id,
        String title,
        String slug,
        String summary,
        String coverImageUrl,
        ArticleStatus status,
        Instant publishedAt,
        Instant createdAt,
        Instant updatedAt) {
}
