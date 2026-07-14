package com.wildai.seo.web;

import com.wildai.common.config.WildAiProperties;
import com.wildai.seo.service.SitemapService;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SeoEndpointController {

    private final SitemapService sitemapService;
    private final String baseUrl;

    public SeoEndpointController(SitemapService sitemapService, WildAiProperties properties) {
        this.sitemapService = sitemapService;
        this.baseUrl = properties.getSeo().getBaseUrl().trim().replaceAll("/+$", "");
    }

    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> sitemap() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noCache())
                .body(sitemapService.render());
    }

    @GetMapping(value = "/robots.txt", produces = MediaType.TEXT_PLAIN_VALUE)
    public String robots() {
        return """
                User-agent: *
                Allow: /products
                Allow: /articles
                Disallow: /admin/
                Disallow: /login
                Disallow: /register
                Disallow: /checkout/
                Disallow: /payment/
                Disallow: /transaction-record
                Sitemap: %s/sitemap.xml
                """.formatted(baseUrl);
    }
}
