package com.wildai.content.service;

import com.wildai.common.dto.PageResult;
import com.wildai.common.exception.BusinessException;
import com.wildai.common.exception.ErrorCode;
import com.wildai.content.domain.Article;
import com.wildai.content.domain.ArticleStatus;
import com.wildai.content.dto.ArticlePublicDto;
import com.wildai.content.repository.ArticleRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class ArticlePublicQueryService {

    private final ArticleRepository repository;

    public ArticlePublicQueryService(ArticleRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public PageResult<ArticlePublicDto> listPublished(int pageNo, int pageSize) {
        validatePagination(pageNo, pageSize);
        var page = repository.findByStatusOrderByPublishedAtDesc(
                ArticleStatus.PUBLISHED, PageRequest.of(pageNo - 1, pageSize));
        return new PageResult<>(
                pageNo,
                pageSize,
                page.getTotalElements(),
                page.getContent().stream().map(this::toPublicDto).toList());
    }

    @Transactional(readOnly = true)
    public Optional<ArticlePublicDto> findPublished(String slug) {
        return repository.findBySlugAndStatus(slug, ArticleStatus.PUBLISHED)
                .map(this::toPublicDto);
    }

    private void validatePagination(int pageNo, int pageSize) {
        if (pageNo < 1 || pageSize < 1 || pageSize > 50) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "分页参数超出允许范围");
        }
    }

    private ArticlePublicDto toPublicDto(Article article) {
        return new ArticlePublicDto(
                article.getId(),
                article.getTitle(),
                article.getSlug(),
                article.getSummary(),
                article.getCoverImageUrl(),
                article.getContentHtml(),
                article.getSeoTitle(),
                article.getSeoDescription(),
                article.getPublishedAt(),
                article.getUpdatedAt());
    }
}
