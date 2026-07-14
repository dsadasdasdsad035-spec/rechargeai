package com.wildai.smoke;

import com.wildai.content.domain.Article;
import com.wildai.content.domain.ArticleStatus;
import com.wildai.content.repository.ArticleListProjection;
import com.wildai.content.repository.ArticleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ArticleRepositorySmokeIT extends BaseSmokeIT {

    @Autowired
    ArticleRepository articleRepository;

    @Autowired
    PlatformTransactionManager transactionManager;

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

    @Test
    void rejectsStaleArticleWrittenByAnotherTransaction() {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        Long articleId = transaction.execute(status -> {
            Article article = article("optimistic-" + UUID.randomUUID());
            return articleRepository.saveAndFlush(article).getId();
        });
        Article firstCopy = transaction.execute(status -> articleRepository.findById(articleId).orElseThrow());
        Article staleCopy = transaction.execute(status -> articleRepository.findById(articleId).orElseThrow());

        firstCopy.setTitle("管理员甲已更新");
        transaction.executeWithoutResult(status -> articleRepository.saveAndFlush(firstCopy));
        staleCopy.setTitle("管理员乙的过期更新");

        assertThatThrownBy(() -> transaction.executeWithoutResult(
                status -> articleRepository.saveAndFlush(staleCopy)))
                .isInstanceOf(OptimisticLockingFailureException.class);
    }

    @Test
    @Transactional
    void listQueriesUseProjectionWithoutLongTextAndStableIdOrder() {
        Instant listTime = Instant.parse("2026-07-14T00:00:00Z");
        Article first = article("projection-first-" + UUID.randomUUID());
        first.setTitle("投影排序文章");
        first.setStatus(ArticleStatus.PUBLISHED);
        first.setPublishedAt(listTime);
        first.setUpdatedAt(listTime);
        first = articleRepository.saveAndFlush(first);

        Article second = article("projection-second-" + UUID.randomUUID());
        second.setTitle("投影排序文章");
        second.setStatus(ArticleStatus.PUBLISHED);
        second.setPublishedAt(listTime);
        second.setUpdatedAt(listTime);
        second = articleRepository.saveAndFlush(second);

        var published = articleRepository.findByStatusOrderByPublishedAtDesc(
                ArticleStatus.PUBLISHED, PageRequest.of(0, 10));
        var admin = articleRepository.searchAdmin(
                "投影排序", ArticleStatus.PUBLISHED, PageRequest.of(0, 10));

        assertThat(published.getContent())
                .extracting(ArticleListProjection::getId)
                .containsExactly(second.getId(), first.getId());
        assertThat(admin.getContent())
                .extracting(ArticleListProjection::getId)
                .containsExactly(second.getId(), first.getId());
        assertThat(Arrays.stream(ArticleListProjection.class.getMethods())
                .map(method -> method.getName()))
                .doesNotContain("getContentMarkdown", "getContentHtml");
        assertThat(published.getContent()).allSatisfy(item ->
                assertThat(item).isNotInstanceOf(Article.class));
    }

    private Article article(String slug) {
        Article article = new Article();
        article.setTitle("并发更新文章");
        article.setSlug(slug);
        article.setSummary("并发测试摘要");
        article.setContentMarkdown("# 并发测试");
        article.setContentHtml("<h1>并发测试</h1>");
        article.setStatus(ArticleStatus.DRAFT);
        return article;
    }
}
