package com.wildai.content.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wildai.admin.service.AuditLogService;
import com.wildai.admin.service.RbacService;
import com.wildai.common.exception.BusinessException;
import com.wildai.common.exception.ErrorCode;
import com.wildai.content.domain.Article;
import com.wildai.content.domain.ArticleStatus;
import com.wildai.content.dto.ArticlePreviewRequest;
import com.wildai.content.dto.ArticlePublicSummaryDto;
import com.wildai.content.dto.ArticleSaveRequest;
import com.wildai.content.repository.ArticleListProjection;
import com.wildai.content.repository.ArticleRepository;
import com.wildai.seo.service.SitemapVersion;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ArticleManagementServiceTest {

    private final ArticleRepository repository = mock(ArticleRepository.class);
    private final AuditLogService auditLogService = mock(AuditLogService.class);
    private final SitemapVersion sitemapVersion = mock(SitemapVersion.class);
    private final RbacService rbacService = mock(RbacService.class);
    private final ArticleManagementService service = new ArticleManagementService(
            repository, new ArticleMarkdownService(), auditLogService, sitemapVersion, rbacService);
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
    void auditSnapshotEscapesControlCharactersAndContainsOnlyMetadata() throws Exception {
        Article article = article(7L, "seo-guide", ArticleStatus.DRAFT, null);
        article.setTitle("标题\b\f\u0001");
        article.setContentMarkdown("绝密正文");
        when(repository.findById(7L)).thenReturn(Optional.of(article));
        when(repository.save(any(Article.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.publish(9L, 7L);

        ArgumentCaptor<String> afterCaptor = ArgumentCaptor.forClass(String.class);
        verify(auditLogService).log(
                eq(9L), eq("ARTICLE_PUBLISH"), eq("ARTICLE"), eq("7"),
                any(String.class), afterCaptor.capture(), eq("发布文章"));
        var snapshot = new ObjectMapper().readTree(afterCaptor.getValue());
        assertThat(snapshot.size()).isEqualTo(3);
        assertThat(snapshot.has("title")).isTrue();
        assertThat(snapshot.has("slug")).isTrue();
        assertThat(snapshot.has("status")).isTrue();
        assertThat(snapshot.get("title").asText()).isEqualTo("标题\b\f\u0001");
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
    void updateConvertsOptimisticLockFailureToConflict() {
        Article article = article(7L, "seo-guide", ArticleStatus.DRAFT, null);
        when(repository.findById(7L)).thenReturn(Optional.of(article));
        when(repository.saveAndFlush(any(Article.class)))
                .thenThrow(new ObjectOptimisticLockingFailureException(Article.class, 7L));

        assertOptimisticConflict(() -> service.update(9L, 7L, request("seo-guide", "更新正文")));
    }

    @Test
    void publishFlushesAndConvertsOptimisticLockFailureToConflict() {
        Article article = article(7L, "seo-guide", ArticleStatus.DRAFT, null);
        when(repository.findById(7L)).thenReturn(Optional.of(article));
        when(repository.save(any(Article.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doThrow(new ObjectOptimisticLockingFailureException(Article.class, 7L))
                .when(repository).flush();

        assertOptimisticConflict(() -> service.publish(9L, 7L));
    }

    @Test
    void withdrawFlushesAndConvertsOptimisticLockFailureToConflict() {
        Article article = article(7L, "seo-guide", ArticleStatus.PUBLISHED, Instant.now());
        when(repository.findById(7L)).thenReturn(Optional.of(article));
        when(repository.save(any(Article.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doThrow(new ObjectOptimisticLockingFailureException(Article.class, 7L))
                .when(repository).flush();

        assertOptimisticConflict(() -> service.withdraw(9L, 7L));
    }

    @Test
    void deleteFlushesAndConvertsOptimisticLockFailureToConflict() {
        Article article = article(7L, "seo-guide", ArticleStatus.DRAFT, null);
        when(repository.findById(7L)).thenReturn(Optional.of(article));
        doThrow(new ObjectOptimisticLockingFailureException(Article.class, 7L))
                .when(repository).flush();

        assertOptimisticConflict(() -> service.deleteDraft(9L, 7L));
    }

    @Test
    void previewDelegatesToSafeMarkdownService() {
        String html = service.preview("正文<script>alert(1)</script>");

        assertThat(html).contains("正文").doesNotContain("script", "alert(1)");
        verifyNoRepositoryWrite();
    }

    @Test
    void writeOperationsRequireContentPermissionBeforeRepositoryAccess() {
        doThrow(new BusinessException(ErrorCode.FORBIDDEN, "无权管理文章内容"))
                .when(rbacService).requireManageContent(9L);

        assertManageContentForbidden(() -> service.create(9L, request("seo-guide", "正文")));
        assertManageContentForbidden(() -> service.update(9L, 7L, request("seo-guide", "正文")));
        assertManageContentForbidden(() -> service.publish(9L, 7L));
        assertManageContentForbidden(() -> service.withdraw(9L, 7L));
        assertManageContentForbidden(() -> service.deleteDraft(9L, 7L));

        verify(rbacService, times(5)).requireManageContent(9L);
        verifyNoRepositoryWrite();
        verify(repository, never()).findById(any());
    }

    @Test
    void uniqueSlugViolationBecomesConflict() {
        when(repository.saveAndFlush(any(Article.class)))
                .thenThrow(new DataIntegrityViolationException(
                        "Duplicate entry 'duplicate' for key 'uk_content_article_slug'"));

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
                .thenThrow(new DataIntegrityViolationException(
                        "Duplicate entry 'article-7' for key 'uk_content_article_slug'"));

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
    void updateSlugConstraintViolationBecomesConflict() {
        Article article = article(7L, "old-slug", ArticleStatus.DRAFT, null);
        when(repository.findById(7L)).thenReturn(Optional.of(article));
        when(repository.saveAndFlush(any(Article.class)))
                .thenThrow(new DataIntegrityViolationException(
                        "Duplicate entry 'new-slug' for key 'uk_content_article_slug'"));

        assertThatThrownBy(() -> service.update(9L, 7L, request("new-slug", "正文")))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CONFLICT);
                    assertThat(exception).hasMessage("链接标识已存在");
                });
    }

    @Test
    void nonSlugDataIntegrityViolationIsNotConverted() {
        DataIntegrityViolationException failure = new DataIntegrityViolationException(
                "Column 'content_html' cannot be null");
        when(repository.saveAndFlush(any(Article.class))).thenThrow(failure);

        assertThatThrownBy(() -> service.create(9L, request("valid-slug", "正文")))
                .isSameAs(failure);
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
            var validator = validatorFactory.getValidator();
            var violations = validator.validate(new ArticleSaveRequest(
                    "标题",
                    "slug",
                    "摘".repeat(501),
                    "图".repeat(513),
                    "正".repeat(ArticleSaveRequest.MAX_CONTENT_MARKDOWN_LENGTH + 1),
                    "SEO 标题",
                    "SEO 描述"));
            var previewViolations = validator.validate(new ArticlePreviewRequest(
                    "正".repeat(ArticleSaveRequest.MAX_CONTENT_MARKDOWN_LENGTH + 1)));
            var blankDraftViolations = validator.validate(request("blank-draft", ""));

            assertThat(violations)
                    .extracting(violation -> violation.getMessage())
                    .contains(
                            "摘要不能超过 500 个字符",
                            "封面地址不能超过 512 个字符",
                            "文章正文不能超过 200000 个字符");
            assertThat(previewViolations)
                    .extracting(violation -> violation.getMessage())
                    .containsExactly("文章正文不能超过 200000 个字符");
            assertThat(blankDraftViolations).isEmpty();
        }
    }

    @Test
    void publicQueriesUsePublishedRepositoryMethods() {
        Article article = article(7L, "seo-guide", ArticleStatus.PUBLISHED, Instant.now());
        ArticleListProjection projection = projection(article);
        when(repository.findByStatusOrderByPublishedAtDesc(eq(ArticleStatus.PUBLISHED), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(projection), PageRequest.of(0, 10), 1));
        when(repository.findBySlugAndStatus("seo-guide", ArticleStatus.PUBLISHED))
                .thenReturn(Optional.of(article));

        var page = publicQueryService.listPublished(1, 10);
        var detail = publicQueryService.findPublished("seo-guide");

        assertThat(page.items()).hasSize(1);
        ArticlePublicSummaryDto summary = page.items().getFirst();
        assertThat(summary.slug()).isEqualTo("seo-guide");
        assertThat(detail).isPresent().get().extracting("slug").isEqualTo("seo-guide");
        verify(repository).findByStatusOrderByPublishedAtDesc(eq(ArticleStatus.PUBLISHED), any(Pageable.class));
        verify(repository).findBySlugAndStatus("seo-guide", ArticleStatus.PUBLISHED);
        verify(repository, never()).findById(any());
    }

    @Test
    void adminSearchMapsLightweightProjection() {
        Article article = article(7L, "seo-guide", ArticleStatus.DRAFT, null);
        ArticleListProjection projection = projection(article);
        when(repository.searchAdmin(eq("SEO"), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(projection), PageRequest.of(0, 10), 1));

        var page = service.search(" SEO ", null, 1, 10);

        assertThat(page.items()).singleElement().satisfies(summary -> {
            assertThat(summary.id()).isEqualTo(7L);
            assertThat(summary.slug()).isEqualTo("seo-guide");
            assertThat(summary.status()).isEqualTo(ArticleStatus.DRAFT);
        });
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

    private ArticleListProjection projection(Article article) {
        ArticleListProjection projection = mock(ArticleListProjection.class);
        when(projection.getId()).thenReturn(article.getId());
        when(projection.getTitle()).thenReturn(article.getTitle());
        when(projection.getSlug()).thenReturn(article.getSlug());
        when(projection.getSummary()).thenReturn(article.getSummary());
        when(projection.getCoverImageUrl()).thenReturn(article.getCoverImageUrl());
        when(projection.getStatus()).thenReturn(article.getStatus());
        when(projection.getPublishedAt()).thenReturn(article.getPublishedAt());
        when(projection.getCreatedAt()).thenReturn(article.getCreatedAt());
        when(projection.getUpdatedAt()).thenReturn(article.getUpdatedAt());
        return projection;
    }

    private void verifyNoRepositoryWrite() {
        verify(repository, never()).save(any(Article.class));
        verify(repository, never()).saveAndFlush(any(Article.class));
        verify(repository, never()).delete(any(Article.class));
    }

    private void assertOptimisticConflict(org.assertj.core.api.ThrowableAssert.ThrowingCallable operation) {
        assertThatThrownBy(operation)
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CONFLICT);
                    assertThat(exception).hasMessage("文章已被其他管理员修改，请刷新后重试");
                });
    }

    private void assertManageContentForbidden(org.assertj.core.api.ThrowableAssert.ThrowingCallable operation) {
        assertThatThrownBy(operation)
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN);
                    assertThat(exception).hasMessage("无权管理文章内容");
                });
    }
}
