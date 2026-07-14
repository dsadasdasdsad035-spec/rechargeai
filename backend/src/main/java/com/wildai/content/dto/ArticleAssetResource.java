package com.wildai.content.dto;

import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;

public record ArticleAssetResource(Resource resource, MediaType contentType) {}
