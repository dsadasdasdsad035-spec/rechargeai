package com.wildai.admin.web;

import com.wildai.admin.service.RbacService;
import com.wildai.common.dto.ApiResponse;
import com.wildai.common.security.UserPrincipal;
import com.wildai.content.dto.ArticleAssetUploadDto;
import com.wildai.content.service.ArticleAssetService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/admin/api/article-assets")
public class AdminArticleAssetController {

    private final ArticleAssetService articleAssetService;
    private final RbacService rbacService;

    public AdminArticleAssetController(ArticleAssetService articleAssetService, RbacService rbacService) {
        this.articleAssetService = articleAssetService;
        this.rbacService = rbacService;
    }

    @PostMapping
    public ApiResponse<ArticleAssetUploadDto> upload(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam("file") MultipartFile file) {
        rbacService.requireManageContent(principal.id());
        return ApiResponse.ok(articleAssetService.upload(file));
    }
}
