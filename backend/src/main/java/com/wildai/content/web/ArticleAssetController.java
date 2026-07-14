package com.wildai.content.web;

import com.wildai.content.dto.ArticleAssetResource;
import com.wildai.content.service.ArticleAssetService;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/article-assets")
public class ArticleAssetController {

    private final ArticleAssetService articleAssetService;

    public ArticleAssetController(ArticleAssetService articleAssetService) {
        this.articleAssetService = articleAssetService;
    }

    @GetMapping("/{storedName:.+}")
    public ResponseEntity<Resource> get(@PathVariable String storedName) {
        ArticleAssetResource asset = articleAssetService.load(storedName);
        return ResponseEntity.ok()
                .contentType(asset.contentType())
                .cacheControl(CacheControl.maxAge(30, TimeUnit.DAYS).cachePublic())
                .header("X-Content-Type-Options", "nosniff")
                .body(asset.resource());
    }
}
