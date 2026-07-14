package com.wildai.content.repository;

import com.wildai.content.domain.ArticleStatus;

import java.time.Instant;

public interface ArticleListProjection {

    Long getId();

    String getTitle();

    String getSlug();

    String getSummary();

    String getCoverImageUrl();

    ArticleStatus getStatus();

    Instant getPublishedAt();

    Instant getCreatedAt();

    Instant getUpdatedAt();
}
