package com.wildai.smoke;

import com.wildai.content.domain.Article;
import com.wildai.content.domain.ArticleStatus;
import com.wildai.content.repository.ArticleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

class ArticleRepositorySmokeIT extends BaseSmokeIT {

    @Autowired
    ArticleRepository articleRepository;

    @Test
    @Transactional
    void savesAndFindsArticleBySlug() {
        Article article = new Article();
        article.setTitle("SEO 入门");
        article.setSlug("seo-intro");
        article.setSummary("SEO 入门摘要");
        article.setContentMarkdown("# SEO 入门");
        article.setContentHtml("<h1>SEO 入门</h1>");
        article.setStatus(ArticleStatus.DRAFT);

        Article saved = articleRepository.saveAndFlush(article);

        assertThat(articleRepository.findBySlug("seo-intro")).contains(saved);
    }
}
