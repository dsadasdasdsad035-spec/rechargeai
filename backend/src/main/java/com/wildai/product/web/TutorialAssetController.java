package com.wildai.product.web;

import com.wildai.product.service.TutorialAssetService;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/tutorial-assets")
public class TutorialAssetController {

    private final TutorialAssetService tutorialAssetService;

    public TutorialAssetController(TutorialAssetService tutorialAssetService) {
        this.tutorialAssetService = tutorialAssetService;
    }

    @GetMapping("/{storedName}")
    public ResponseEntity<Resource> get(@PathVariable String storedName) {
        Resource resource = tutorialAssetService.loadAsResource(storedName);
        MediaType mediaType = tutorialAssetService.resolveMediaType(storedName);
        return ResponseEntity.ok()
                .contentType(mediaType)
                .cacheControl(CacheControl.maxAge(30, TimeUnit.DAYS).cachePublic())
                .body(resource);
    }
}
