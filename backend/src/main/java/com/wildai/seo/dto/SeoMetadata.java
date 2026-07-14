package com.wildai.seo.dto;

public record SeoMetadata(
        String title,
        String description,
        String canonical,
        String type,
        String imageUrl,
        String robots,
        String jsonLd) {
}
