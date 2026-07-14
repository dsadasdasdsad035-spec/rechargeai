package com.wildai.admin.web;

import com.wildai.common.dto.ApiResponse;
import com.wildai.common.dto.PageResult;
import com.wildai.common.security.UserPrincipal;
import com.wildai.content.domain.ArticleStatus;
import com.wildai.content.dto.ArticleDetailDto;
import com.wildai.content.dto.ArticlePreviewDto;
import com.wildai.content.dto.ArticlePreviewRequest;
import com.wildai.content.dto.ArticleSaveRequest;
import com.wildai.content.dto.ArticleSummaryDto;
import com.wildai.content.service.ArticleManagementService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/api/articles")
public class AdminArticleController {

    private final ArticleManagementService articleManagementService;

    public AdminArticleController(ArticleManagementService articleManagementService) {
        this.articleManagementService = articleManagementService;
    }

    @GetMapping
    public ApiResponse<PageResult<ArticleSummaryDto>> list(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) ArticleStatus status,
            @RequestParam(defaultValue = "1") int pageNo,
            @RequestParam(defaultValue = "10") int pageSize) {
        return ApiResponse.ok(articleManagementService.search(title, status, pageNo, pageSize));
    }

    @GetMapping("/{id}")
    public ApiResponse<ArticleDetailDto> detail(@PathVariable Long id) {
        return ApiResponse.ok(articleManagementService.get(id));
    }

    @PostMapping
    public ApiResponse<ArticleDetailDto> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ArticleSaveRequest request) {
        return ApiResponse.ok(articleManagementService.create(principal.id(), request));
    }

    @PutMapping("/{id}")
    public ApiResponse<ArticleDetailDto> update(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody ArticleSaveRequest request) {
        return ApiResponse.ok(articleManagementService.update(principal.id(), id, request));
    }

    @PostMapping("/preview")
    public ApiResponse<ArticlePreviewDto> preview(@Valid @RequestBody ArticlePreviewRequest request) {
        return ApiResponse.ok(new ArticlePreviewDto(
                articleManagementService.preview(request.contentMarkdown())));
    }

    @PostMapping("/{id}/publish")
    public ApiResponse<ArticleDetailDto> publish(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        return ApiResponse.ok(articleManagementService.publish(principal.id(), id));
    }

    @PostMapping("/{id}/withdraw")
    public ApiResponse<ArticleDetailDto> withdraw(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        return ApiResponse.ok(articleManagementService.withdraw(principal.id(), id));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        articleManagementService.deleteDraft(principal.id(), id);
        return ApiResponse.ok(null);
    }
}
