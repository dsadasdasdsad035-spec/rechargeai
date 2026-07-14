package com.wildai.content.repository;

import com.wildai.content.domain.Article;
import com.wildai.content.domain.ArticleStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ArticleRepository extends JpaRepository<Article, Long> {

    Optional<Article> findBySlug(String slug);

    Optional<Article> findBySlugAndStatus(String slug, ArticleStatus status);

    boolean existsBySlugAndIdNot(String slug, Long id);

    @Query(value = """
            SELECT article.id AS id,
                   article.title AS title,
                   article.slug AS slug,
                   article.summary AS summary,
                   article.coverImageUrl AS coverImageUrl,
                   article.status AS status,
                   article.publishedAt AS publishedAt,
                   article.createdAt AS createdAt,
                   article.updatedAt AS updatedAt
            FROM Article article
            WHERE article.status = :status
            ORDER BY article.publishedAt DESC, article.id DESC
            """,
            countQuery = """
                    SELECT COUNT(article)
                    FROM Article article
                    WHERE article.status = :status
                    """)
    Page<ArticleListProjection> findByStatusOrderByPublishedAtDesc(
            @Param("status") ArticleStatus status,
            Pageable pageable);

    @Query("""
            SELECT article
            FROM Article article
            WHERE article.status = :status
            ORDER BY article.updatedAt DESC, article.id DESC
            """)
    List<Article> findByStatusOrderByUpdatedAtDesc(@Param("status") ArticleStatus status);

    @Query(value = """
            SELECT article.id AS id,
                   article.title AS title,
                   article.slug AS slug,
                   article.summary AS summary,
                   article.coverImageUrl AS coverImageUrl,
                   article.status AS status,
                   article.publishedAt AS publishedAt,
                   article.createdAt AS createdAt,
                   article.updatedAt AS updatedAt
            FROM Article article
            WHERE (:title IS NULL OR :title = '' OR LOWER(article.title) LIKE LOWER(CONCAT('%', :title, '%')))
              AND (:status IS NULL OR article.status = :status)
            ORDER BY article.updatedAt DESC, article.id DESC
            """,
            countQuery = """
                    SELECT COUNT(article)
                    FROM Article article
                    WHERE (:title IS NULL OR :title = '' OR LOWER(article.title) LIKE LOWER(CONCAT('%', :title, '%')))
                      AND (:status IS NULL OR article.status = :status)
                    """)
    Page<ArticleListProjection> searchAdmin(
            @Param("title") String title,
            @Param("status") ArticleStatus status,
            Pageable pageable);
}
