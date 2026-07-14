package com.wildai.seo.web;

import com.wildai.content.service.ArticlePublicQueryService;
import com.wildai.seo.service.SeoMetadataFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class PublicArticlePageController {

    private static final int ARTICLE_PAGE_SIZE = 50;

    private final ArticlePublicQueryService articleQueryService;
    private final SeoMetadataFactory seoMetadataFactory;

    public PublicArticlePageController(
            ArticlePublicQueryService articleQueryService,
            SeoMetadataFactory seoMetadataFactory) {
        this.articleQueryService = articleQueryService;
        this.seoMetadataFactory = seoMetadataFactory;
    }

    @GetMapping("/articles")
    public String list(Model model) {
        var articles = articleQueryService.listPublished(1, ARTICLE_PAGE_SIZE).items();
        model.addAttribute("articles", articles);
        model.addAttribute("seo", seoMetadataFactory.forArticleList(articles));
        return "public/article-list";
    }

    @GetMapping("/articles/{slug}")
    public String detail(@PathVariable String slug, Model model) {
        var article = articleQueryService.findPublished(slug)
                .orElseThrow(PublicPageNotFoundException::new);
        model.addAttribute("article", article);
        model.addAttribute("seo", seoMetadataFactory.forArticle(article));
        return "public/article-detail";
    }
}
