package com.wildai.content.dto;

import java.time.Instant;

public record ArticlePublicSummaryDto(
        Long id,
        String title,
        String slug,
        String summary,
        String coverImageUrl,
        Instant publishedAt,
        Instant updatedAt) {
}
