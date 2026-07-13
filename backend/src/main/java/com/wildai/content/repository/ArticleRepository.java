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

    Page<Article> findByStatusOrderByPublishedAtDesc(ArticleStatus status, Pageable pageable);

    List<Article> findByStatusOrderByUpdatedAtDesc(ArticleStatus status);

    @Query("""
            SELECT article
            FROM Article article
            WHERE (:title IS NULL OR :title = '' OR LOWER(article.title) LIKE LOWER(CONCAT('%', :title, '%')))
              AND (:status IS NULL OR article.status = :status)
            ORDER BY article.updatedAt DESC
            """)
    Page<Article> searchAdmin(
            @Param("title") String title,
            @Param("status") ArticleStatus status,
            Pageable pageable);
}
