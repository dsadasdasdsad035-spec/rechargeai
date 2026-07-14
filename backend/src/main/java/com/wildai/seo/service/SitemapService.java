package com.wildai.seo.service;

import com.wildai.common.config.WildAiProperties;
import com.wildai.content.domain.ArticleStatus;
import com.wildai.content.repository.ArticleRepository;
import com.wildai.product.repository.AiServiceProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

@Service
public class SitemapService {

    private static final Logger log = LoggerFactory.getLogger(SitemapService.class);
    private static final String SITEMAP_NAMESPACE = "http://www.sitemaps.org/schemas/sitemap/0.9";

    private final ArticleRepository articleRepository;
    private final AiServiceProductRepository productRepository;
    private final SitemapVersion sitemapVersion;
    private final String baseUrl;
    private volatile CachedSitemap cache;

    public SitemapService(
            ArticleRepository articleRepository,
            AiServiceProductRepository productRepository,
            SitemapVersion sitemapVersion,
            WildAiProperties properties) {
        this.articleRepository = articleRepository;
        this.productRepository = productRepository;
        this.sitemapVersion = sitemapVersion;
        this.baseUrl = normalizeBaseUrl(properties.getSeo().getBaseUrl());
    }

    @Transactional(readOnly = true)
    public synchronized String render() {
        long version = sitemapVersion.current();
        CachedSitemap current = cache;
        if (current != null && current.version() == version) {
            return current.xml();
        }

        String xml = generateXml();
        cache = new CachedSitemap(version, xml);
        return xml;
    }

    private String generateXml() {
        try {
            StringWriter output = new StringWriter();
            XMLStreamWriter xml = XMLOutputFactory.newFactory().createXMLStreamWriter(output);
            xml.writeStartDocument("UTF-8", "1.0");
            xml.writeStartElement("urlset");
            xml.writeDefaultNamespace(SITEMAP_NAMESPACE);
            writeUrl(xml, "/products", null);
            writeUrl(xml, "/articles", null);

            productRepository.findByStatusOrderByUpdatedAtDesc("ON_SHELF").forEach(product -> {
                try {
                    if (product.getId() == null) {
                        throw new IllegalArgumentException("产品 ID 为空");
                    }
                    writeUrl(xml, "/products/" + product.getId(), product.getUpdatedAt());
                } catch (Exception exception) {
                    log.warn("生成站点地图时跳过产品，产品ID={}，原因={}",
                            product.getId(), exception.getMessage());
                }
            });

            articleRepository.findByStatusOrderByUpdatedAtDesc(ArticleStatus.PUBLISHED).forEach(article -> {
                try {
                    if (article.getSlug() == null || article.getSlug().isBlank()) {
                        throw new IllegalArgumentException("文章链接标识为空");
                    }
                    writeUrl(xml, "/articles/" + article.getSlug(), article.getUpdatedAt());
                } catch (Exception exception) {
                    log.warn("生成站点地图时跳过文章，文章ID={}，原因={}",
                            article.getId(), exception.getMessage());
                }
            });

            xml.writeEndElement();
            xml.writeEndDocument();
            xml.close();
            return output.toString();
        } catch (XMLStreamException exception) {
            throw new IllegalStateException("生成站点地图失败", exception);
        }
    }

    private void writeUrl(XMLStreamWriter xml, String path, Instant updatedAt) throws XMLStreamException {
        xml.writeStartElement("url");
        xml.writeStartElement("loc");
        xml.writeCharacters(baseUrl + path);
        xml.writeEndElement();
        if (updatedAt != null) {
            xml.writeStartElement("lastmod");
            xml.writeCharacters(DateTimeFormatter.ISO_INSTANT.format(updatedAt));
            xml.writeEndElement();
        }
        xml.writeEndElement();
    }

    private static String normalizeBaseUrl(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("SEO 根地址不能为空");
        }
        return value.trim().replaceAll("/+$", "");
    }

    private record CachedSitemap(long version, String xml) {
    }
}
