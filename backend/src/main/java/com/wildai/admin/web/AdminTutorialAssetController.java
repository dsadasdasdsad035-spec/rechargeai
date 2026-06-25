package com.wildai.admin.web;

import com.wildai.common.dto.ApiResponse;
import com.wildai.product.dto.TutorialAssetUploadDto;
import com.wildai.product.service.TutorialAssetService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/admin/api/tutorial-assets")
public class AdminTutorialAssetController {

    private final TutorialAssetService tutorialAssetService;

    public AdminTutorialAssetController(TutorialAssetService tutorialAssetService) {
        this.tutorialAssetService = tutorialAssetService;
    }

    @PostMapping
    public ApiResponse<TutorialAssetUploadDto> upload(@RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(tutorialAssetService.upload(file));
    }
}
