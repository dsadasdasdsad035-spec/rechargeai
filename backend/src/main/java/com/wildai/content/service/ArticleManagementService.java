package com.wildai.content.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wildai.admin.service.AuditLogService;
import com.wildai.common.dto.PageResult;
import com.wildai.common.exception.BusinessException;
import com.wildai.common.exception.ErrorCode;
import com.wildai.content.domain.Article;
import com.wildai.content.domain.ArticleStatus;
import com.wildai.content.dto.ArticleDetailDto;
import com.wildai.content.dto.ArticleSaveRequest;
import com.wildai.content.dto.ArticleSummaryDto;
import com.wildai.content.dto.RenderedArticleContent;
import com.wildai.content.repository.ArticleListProjection;
import com.wildai.content.repository.ArticleRepository;
import com.wildai.seo.service.SitemapVersion;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class ArticleManagementService {

    private static final Pattern SLUG_PATTERN = Pattern.compile("[a-z0-9]+(?:-[a-z0-9]+)*");
    private static final ObjectMapper AUDIT_OBJECT_MAPPER = new ObjectMapper();

    private final ArticleRepository repository;
    private final ArticleMarkdownService markdownService;
    private final AuditLogService auditLogService;
    private final SitemapVersion sitemapVersion;

    public ArticleManagementService(ArticleRepository repository,
                                    ArticleMarkdownService markdownService,
                                    AuditLogService auditLogService,
                                    SitemapVersion sitemapVersion) {
        this.repository = repository;
        this.markdownService = markdownService;
        this.auditLogService = auditLogService;
        this.sitemapVersion = sitemapVersion;
    }

    @Transactional(readOnly = true)
    public PageResult<ArticleSummaryDto> search(String title, ArticleStatus status, int pageNo, int pageSize) {
        validatePagination(pageNo, pageSize);
        String normalizedTitle = title == null || title.isBlank() ? null : title.trim();
        var page = repository.searchAdmin(
                normalizedTitle, status, PageRequest.of(pageNo - 1, pageSize));
        return new PageResult<>(
                pageNo,
                pageSize,
                page.getTotalElements(),
                page.getContent().stream().map(this::toSummary).toList());
    }

    @Transactional(readOnly = true)
    public ArticleDetailDto get(Long id) {
        return toDetail(requireArticle(id));
    }

    @Transactional
    public ArticleDetailDto create(Long operatorId, ArticleSaveRequest request) {
        String slug = normalizeSlug(request.slug());
        String markdown = normalizeMarkdown(request.contentMarkdown());
        RenderedArticleContent rendered = markdownService.render(markdown, request.summary());

        Article article = new Article();
        article.setTitle(request.title());
        article.setSlug(slug.isEmpty() ? temporarySlug() : slug);
        article.setSummary(rendered.summary());
        article.setCoverImageUrl(request.coverImageUrl());
        article.setContentMarkdown(markdown);
        article.setContentHtml(rendered.html());
        article.setSeoTitle(request.seoTitle());
        article.setSeoDescription(request.seoDescription());

        try {
            article = repository.saveAndFlush(article);
            if (slug.isEmpty()) {
                article.setSlug("article-" + article.getId());
                article.setUpdatedAt(Instant.now());
                article = repository.saveAndFlush(article);
            }
        } catch (DataIntegrityViolationException exception) {
            throw translateDataIntegrityViolation(exception);
        }

        auditLogService.log(
                operatorId, "ARTICLE_CREATE", "ARTICLE", String.valueOf(article.getId()),
                null, snapshot(article), "创建文章");
        return toDetail(article);
    }

    @Transactional
    public ArticleDetailDto update(Long operatorId, Long id, ArticleSaveRequest request) {
        Article article = requireArticle(id);
        String normalizedSlug = normalizeSlug(request.slug());
        String desiredSlug = normalizedSlug.isEmpty() ? "article-" + article.getId() : normalizedSlug;
        if (article.getPublishedAt() != null && !Objects.equals(article.getSlug(), desiredSlug)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "文章发布后不能修改链接标识");
        }

        String before = snapshot(article);
        ArticleStatus beforeStatus = article.getStatus();
        String markdown = normalizeMarkdown(request.contentMarkdown());
        RenderedArticleContent rendered = markdownService.render(markdown, request.summary());
        article.setTitle(request.title());
        article.setSlug(desiredSlug);
        article.setSummary(rendered.summary());
        article.setCoverImageUrl(request.coverImageUrl());
        article.setContentMarkdown(markdown);
        article.setContentHtml(rendered.html());
        article.setSeoTitle(request.seoTitle());
        article.setSeoDescription(request.seoDescription());
        article.setUpdatedAt(Instant.now());

        try {
            article = repository.saveAndFlush(article);
        } catch (OptimisticLockingFailureException exception) {
            throw optimisticConflict();
        } catch (DataIntegrityViolationException exception) {
            throw translateDataIntegrityViolation(exception);
        }

        auditLogService.log(
                operatorId, "ARTICLE_UPDATE", "ARTICLE", String.valueOf(article.getId()),
                before, snapshot(article), "更新文章");
        if (beforeStatus == ArticleStatus.PUBLISHED || article.getStatus() == ArticleStatus.PUBLISHED) {
            sitemapVersion.invalidate();
        }
        return toDetail(article);
    }

    @Transactional
    public ArticleDetailDto publish(Long operatorId, Long id) {
        Article article = requireArticle(id);
        if (article.getStatus() != ArticleStatus.DRAFT) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "文章已发布");
        }
        if (article.getTitle() == null || article.getTitle().isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请填写文章标题");
        }
        if (article.getContentMarkdown() == null || article.getContentMarkdown().isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请填写文章正文");
        }

        String before = snapshot(article);
        Instant now = Instant.now();
        article.setStatus(ArticleStatus.PUBLISHED);
        if (article.getPublishedAt() == null) {
            article.setPublishedAt(now);
        }
        article.setUpdatedAt(now);
        try {
            article = repository.save(article);
            repository.flush();
        } catch (OptimisticLockingFailureException exception) {
            throw optimisticConflict();
        }

        auditLogService.log(
                operatorId, "ARTICLE_PUBLISH", "ARTICLE", String.valueOf(article.getId()),
                before, snapshot(article), "发布文章");
        sitemapVersion.invalidate();
        return toDetail(article);
    }

    @Transactional
    public ArticleDetailDto withdraw(Long operatorId, Long id) {
        Article article = requireArticle(id);
        if (article.getStatus() != ArticleStatus.PUBLISHED) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "文章尚未发布");
        }

        String before = snapshot(article);
        article.setStatus(ArticleStatus.DRAFT);
        article.setUpdatedAt(Instant.now());
        try {
            article = repository.save(article);
            repository.flush();
        } catch (OptimisticLockingFailureException exception) {
            throw optimisticConflict();
        }

        auditLogService.log(
                operatorId, "ARTICLE_WITHDRAW", "ARTICLE", String.valueOf(article.getId()),
                before, snapshot(article), "撤回文章");
        sitemapVersion.invalidate();
        return toDetail(article);
    }

    @Transactional
    public void deleteDraft(Long operatorId, Long id) {
        Article article = requireArticle(id);
        if (article.getStatus() == ArticleStatus.PUBLISHED) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "已发布文章请先撤回");
        }

        String before = snapshot(article);
        try {
            repository.delete(article);
            repository.flush();
        } catch (OptimisticLockingFailureException exception) {
            throw optimisticConflict();
        }
        auditLogService.log(
                operatorId, "ARTICLE_DELETE", "ARTICLE", String.valueOf(article.getId()),
                before, null, "删除文章");
        sitemapVersion.invalidate();
    }

    public String preview(String markdown) {
        return markdownService.preview(markdown);
    }

    private Article requireArticle(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "文章不存在"));
    }

    private String normalizeSlug(String slug) {
        String normalized = slug == null ? "" : slug.trim().toLowerCase(Locale.ROOT);
        if (!normalized.isEmpty() && !SLUG_PATTERN.matcher(normalized).matches()) {
            throw new BusinessException(
                    ErrorCode.BAD_REQUEST, "链接标识只能包含小写字母、数字和连字符");
        }
        return normalized;
    }

    private void validatePagination(int pageNo, int pageSize) {
        if (pageNo < 1 || pageSize < 1 || pageSize > 50) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "分页参数超出允许范围");
        }
    }

    private String normalizeMarkdown(String markdown) {
        return markdown == null ? "" : markdown;
    }

    private String temporarySlug() {
        return "draft-" + UUID.randomUUID().toString().replace("-", "");
    }

    private BusinessException slugConflict() {
        return new BusinessException(ErrorCode.CONFLICT, "链接标识已存在");
    }

    private RuntimeException translateDataIntegrityViolation(DataIntegrityViolationException exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (containsSlugConstraint(cause.getMessage())) {
                return slugConflict();
            }
            if (cause instanceof ConstraintViolationException constraintViolation
                    && containsSlugConstraint(constraintViolation.getConstraintName())) {
                return slugConflict();
            }
        }
        return exception;
    }

    private boolean containsSlugConstraint(String value) {
        return value != null
                && value.toLowerCase(Locale.ROOT).contains("uk_content_article_slug");
    }

    private BusinessException optimisticConflict() {
        return new BusinessException(
                ErrorCode.CONFLICT, "文章已被其他管理员修改，请刷新后重试");
    }

    private ArticleSummaryDto toSummary(ArticleListProjection article) {
        return new ArticleSummaryDto(
                article.getId(),
                article.getTitle(),
                article.getSlug(),
                article.getSummary(),
                article.getCoverImageUrl(),
                article.getStatus(),
                article.getPublishedAt(),
                article.getCreatedAt(),
                article.getUpdatedAt());
    }

    private ArticleDetailDto toDetail(Article article) {
        return new ArticleDetailDto(
                article.getId(),
                article.getTitle(),
                article.getSlug(),
                article.getSummary(),
                article.getCoverImageUrl(),
                article.getContentMarkdown(),
                article.getContentHtml(),
                article.getSeoTitle(),
                article.getSeoDescription(),
                article.getStatus(),
                article.getPublishedAt(),
                article.getCreatedAt(),
                article.getUpdatedAt());
    }

    private String snapshot(Article article) {
        try {
            return AUDIT_OBJECT_MAPPER.writeValueAsString(new ArticleAuditSnapshot(
                    article.getTitle(), article.getSlug(), article.getStatus()));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("文章审计快照序列化失败", exception);
        }
    }

    private record ArticleAuditSnapshot(String title, String slug, ArticleStatus status) {
    }
}
