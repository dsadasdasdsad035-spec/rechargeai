package com.wildai.content.service;

import com.wildai.admin.service.AuditLogService;
import com.wildai.common.exception.BusinessException;
import com.wildai.common.exception.ErrorCode;
import com.wildai.content.domain.Article;
import com.wildai.content.domain.ArticleStatus;
import com.wildai.content.dto.ArticleSaveRequest;
import com.wildai.content.repository.ArticleRepository;
import com.wildai.seo.service.SitemapVersion;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ArticleManagementServiceTest {

    private final ArticleRepository repository = mock(ArticleRepository.class);
    private final AuditLogService auditLogService = mock(AuditLogService.class);
    private final SitemapVersion sitemapVersion = mock(SitemapVersion.class);
    private final ArticleManagementService service = new ArticleManagementService(
            repository, new ArticleMarkdownService(), auditLogService, sitemapVersion);
    private final ArticlePublicQueryService publicQueryService = new ArticlePublicQueryService(repository);

    @Test
    void publishSetsFirstPublishedAtAndInvalidatesSitemap() {
        Article article = article(7L, "seo-guide", ArticleStatus.DRAFT, null);
        article.setContentMarkdown("绝密正文");
        when(repository.findById(7L)).thenReturn(Optional.of(article));
        when(repository.save(any(Article.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.publish(9L, 7L);

        assertThat(result.status()).isEqualTo(ArticleStatus.PUBLISHED);
        assertThat(result.publishedAt()).isNotNull();
        verify(sitemapVersion).invalidate();

        ArgumentCaptor<String> beforeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> afterCaptor = ArgumentCaptor.forClass(String.class);
        verify(auditLogService).log(
                eq(9L),
                eq("ARTICLE_PUBLISH"),
                eq("ARTICLE"),
                eq("7"),
                beforeCaptor.capture(),
                afterCaptor.capture(),
                eq("发布文章"));
        assertThat(beforeCaptor.getValue()).doesNotContain("绝密正文");
        assertThat(afterCaptor.getValue()).doesNotContain("绝密正文");
    }

    @Test
    void cannotChangeSlugAfterFirstPublish() {
        Article article = article(7L, "stable-url", ArticleStatus.DRAFT, Instant.now());
        when(repository.findById(7L)).thenReturn(Optional.of(article));

        assertThatThrownBy(() -> service.update(9L, 7L, request("changed-url", "正文")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("文章发布后不能修改链接标识");

        verify(repository, never()).saveAndFlush(any(Article.class));
    }

    @Test
    void publishedArticleMustBeWithdrawnBeforeDelete() {
        Article article = article(7L, "seo-guide", ArticleStatus.PUBLISHED, Instant.now());
        when(repository.findById(7L)).thenReturn(Optional.of(article));

        assertThatThrownBy(() -> service.deleteDraft(9L, 7L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("已发布文章请先撤回");

        verify(repository, never()).delete(any(Article.class));
    }

    @Test
    void withdrawChangesPublishedArticleToDraftAndInvalidatesSitemap() {
        Instant firstPublishedAt = Instant.now().minusSeconds(3600);
        Article article = article(7L, "seo-guide", ArticleStatus.PUBLISHED, firstPublishedAt);
        when(repository.findById(7L)).thenReturn(Optional.of(article));
        when(repository.save(any(Article.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.withdraw(9L, 7L);

        assertThat(result.status()).isEqualTo(ArticleStatus.DRAFT);
        assertThat(result.publishedAt()).isEqualTo(firstPublishedAt);
        verify(sitemapVersion).invalidate();
        verify(auditLogService).log(
                eq(9L), eq("ARTICLE_WITHDRAW"), eq("ARTICLE"), eq("7"),
                any(String.class), any(String.class), eq("撤回文章"));
    }

    @Test
    void updatingPublishedArticleInvalidatesSitemap() {
        Article article = article(7L, "seo-guide", ArticleStatus.PUBLISHED, Instant.now());
        when(repository.findById(7L)).thenReturn(Optional.of(article));
        when(repository.saveAndFlush(any(Article.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.update(9L, 7L, request("seo-guide", "# 更新后的正文"));

        assertThat(result.contentHtml()).contains("更新后的正文");
        verify(sitemapVersion).invalidate();
    }

    @Test
    void previewDelegatesToSafeMarkdownService() {
        String html = service.preview("正文<script>alert(1)</script>");

        assertThat(html).contains("正文").doesNotContain("script", "alert(1)");
        verifyNoRepositoryWrite();
    }

    @Test
    void uniqueSlugViolationBecomesConflict() {
        when(repository.saveAndFlush(any(Article.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate slug"));

        assertThatThrownBy(() -> service.create(9L, request("duplicate", "正文")))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CONFLICT);
                    assertThat(exception).hasMessage("链接标识已存在");
                });

        verify(auditLogService, never()).log(
                any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void emptySlugUsesTemporarySlugBeforeStableArticleSlug() {
        List<String> savedSlugs = new ArrayList<>();
        when(repository.saveAndFlush(any(Article.class))).thenAnswer(invocation -> {
            Article saved = invocation.getArgument(0);
            savedSlugs.add(saved.getSlug());
            if (saved.getId() == null) {
                saved.setId(7L);
            }
            return saved;
        });

        var result = service.create(9L, request(null, "正文"));

        assertThat(savedSlugs).hasSize(2);
        assertThat(savedSlugs.get(0)).matches("draft-[0-9a-f]{32}");
        assertThat(savedSlugs.get(1)).isEqualTo("article-7");
        assertThat(result.slug()).isEqualTo("article-7");
        verify(repository, times(2)).saveAndFlush(any(Article.class));
    }

    @Test
    void secondAutoSlugFlushViolationBecomesConflict() {
        when(repository.saveAndFlush(any(Article.class)))
                .thenAnswer(invocation -> {
                    Article saved = invocation.getArgument(0);
                    saved.setId(7L);
                    return saved;
                })
                .thenThrow(new DataIntegrityViolationException("duplicate stable slug"));

        assertThatThrownBy(() -> service.create(9L, request("", "正文")))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CONFLICT);
                    assertThat(exception).hasMessage("链接标识已存在");
                });

        verify(repository, times(2)).saveAndFlush(any(Article.class));
        verify(auditLogService, never()).log(
                any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void normalizesSlugAndRejectsUnsupportedCharacters() {
        when(repository.saveAndFlush(any(Article.class))).thenAnswer(invocation -> {
            Article saved = invocation.getArgument(0);
            saved.setId(7L);
            return saved;
        });

        var result = service.create(9L, request(" SEO-Guide ", "正文"));

        assertThat(result.slug()).isEqualTo("seo-guide");
        assertThatThrownBy(() -> service.create(9L, request("bad_slug", "正文")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("链接标识只能包含小写字母、数字和连字符");
    }

    @Test
    void republishKeepsFirstPublishedAt() {
        Instant firstPublishedAt = Instant.now().minusSeconds(3600);
        Article article = article(7L, "seo-guide", ArticleStatus.DRAFT, firstPublishedAt);
        when(repository.findById(7L)).thenReturn(Optional.of(article));
        when(repository.save(any(Article.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.publish(9L, 7L);

        assertThat(result.publishedAt()).isEqualTo(firstPublishedAt);
    }

    @Test
    void deletingDraftAuditsOnlyPublicMetadataAndInvalidatesSitemap() {
        Article article = article(7L, "seo-guide", ArticleStatus.DRAFT, Instant.now());
        article.setContentMarkdown("绝密正文");
        when(repository.findById(7L)).thenReturn(Optional.of(article));

        service.deleteDraft(9L, 7L);

        verify(repository).delete(article);
        verify(sitemapVersion).invalidate();
        verify(auditLogService).log(
                eq(9L), eq("ARTICLE_DELETE"), eq("ARTICLE"), eq("7"),
                eq("{\"title\":\"SEO 指南\",\"slug\":\"seo-guide\",\"status\":\"DRAFT\"}"),
                isNull(), eq("删除文章"));
    }

    @Test
    void saveRequestUsesContractValidationMessages() {
        try (var validatorFactory = Validation.buildDefaultValidatorFactory()) {
            var violations = validatorFactory.getValidator().validate(new ArticleSaveRequest(
                    "标题",
                    "slug",
                    "摘".repeat(501),
                    "图".repeat(513),
                    "正文",
                    "SEO 标题",
                    "SEO 描述"));

            assertThat(violations)
                    .extracting(violation -> violation.getMessage())
                    .contains("摘要不能超过 500 个字符", "封面地址不能超过 512 个字符");
        }
    }

    @Test
    void publicQueriesUsePublishedRepositoryMethods() {
        Article article = article(7L, "seo-guide", ArticleStatus.PUBLISHED, Instant.now());
        when(repository.findByStatusOrderByPublishedAtDesc(eq(ArticleStatus.PUBLISHED), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(article), PageRequest.of(0, 10), 1));
        when(repository.findBySlugAndStatus("seo-guide", ArticleStatus.PUBLISHED))
                .thenReturn(Optional.of(article));

        var page = publicQueryService.listPublished(1, 10);
        var detail = publicQueryService.findPublished("seo-guide");

        assertThat(page.items()).hasSize(1);
        assertThat(detail).isPresent().get().extracting("slug").isEqualTo("seo-guide");
        verify(repository).findByStatusOrderByPublishedAtDesc(eq(ArticleStatus.PUBLISHED), any(Pageable.class));
        verify(repository).findBySlugAndStatus("seo-guide", ArticleStatus.PUBLISHED);
        verify(repository, never()).findById(any());
    }

    @Test
    void rejectsInvalidPaginationBoundary() {
        assertThatThrownBy(() -> service.search(null, null, 0, 10))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.BAD_REQUEST));
        assertThatThrownBy(() -> publicQueryService.listPublished(1, 51))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.BAD_REQUEST));

        verify(repository, never()).searchAdmin(any(), any(), any());
    }

    private Article article(Long id, String slug, ArticleStatus status, Instant publishedAt) {
        Article article = new Article();
        article.setId(id);
        article.setTitle("SEO 指南");
        article.setSlug(slug);
        article.setSummary("摘要");
        article.setCoverImageUrl("https://img.example/cover.png");
        article.setContentMarkdown("正文");
        article.setContentHtml("<p>正文</p>");
        article.setSeoTitle("SEO 标题");
        article.setSeoDescription("SEO 描述");
        article.setStatus(status);
        article.setPublishedAt(publishedAt);
        return article;
    }

    private ArticleSaveRequest request(String slug, String markdown) {
        return new ArticleSaveRequest(
                "SEO 指南", slug, "摘要", "https://img.example/cover.png",
                markdown, "SEO 标题", "SEO 描述");
    }

    private void verifyNoRepositoryWrite() {
        verify(repository, never()).save(any(Article.class));
        verify(repository, never()).saveAndFlush(any(Article.class));
        verify(repository, never()).delete(any(Article.class));
    }
}
