package com.wildai.product.service;

import com.wildai.common.config.WildAiProperties;
import com.wildai.common.exception.BusinessException;
import com.wildai.common.exception.ErrorCode;
import com.wildai.product.dto.TutorialAssetUploadDto;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

@Service
public class TutorialAssetService {

    private static final Map<String, String> IMAGE_EXT_BY_TYPE = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/gif", ".gif",
            "image/webp", ".webp"
    );

    private static final Map<String, String> VIDEO_EXT_BY_TYPE = Map.of(
            "video/mp4", ".mp4",
            "video/webm", ".webm",
            "video/quicktime", ".mov"
    );

    private final Path uploadRoot;
    private final long maxImageBytes;
    private final long maxVideoBytes;

    public TutorialAssetService(WildAiProperties properties) {
        this.uploadRoot = Path.of(properties.getTutorial().getUploadDir()).toAbsolutePath().normalize();
        this.maxImageBytes = properties.getTutorial().getMaxImageBytes();
        this.maxVideoBytes = properties.getTutorial().getMaxVideoBytes();
    }

    @PostConstruct
    void ensureUploadDir() throws IOException {
        Files.createDirectories(uploadRoot);
    }

    public TutorialAssetUploadDto upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请选择图片或视频文件");
        }
        String contentType = file.getContentType();
        if (contentType == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "无法识别文件类型");
        }

        String kind;
        String ext;
        long maxBytes;
        if (IMAGE_EXT_BY_TYPE.containsKey(contentType)) {
            kind = "image";
            ext = IMAGE_EXT_BY_TYPE.get(contentType);
            maxBytes = maxImageBytes;
        } else if (VIDEO_EXT_BY_TYPE.containsKey(contentType)) {
            kind = "video";
            ext = VIDEO_EXT_BY_TYPE.get(contentType);
            maxBytes = maxVideoBytes;
        } else {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "仅支持 JPG、PNG、GIF、WEBP 图片或 MP4、WEBM、MOV 视频");
        }

        if (file.getSize() > maxBytes) {
            String limit = kind.equals("video") ? formatMb(maxVideoBytes) : formatMb(maxImageBytes);
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    (kind.equals("video") ? "视频" : "图片") + "大小不能超过 " + limit);
        }

        String storedName = UUID.randomUUID().toString().replace("-", "") + ext;
        Path target = uploadRoot.resolve(storedName).normalize();
        if (!target.startsWith(uploadRoot)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "非法文件名");
        }

        try {
            file.transferTo(target);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "文件保存失败");
        }

        return new TutorialAssetUploadDto("/api/tutorial-assets/" + storedName, storedName, kind);
    }

    public Resource loadAsResource(String storedName) {
        if (storedName == null || storedName.isBlank() || storedName.contains("..") || storedName.contains("/")) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "资源不存在");
        }
        try {
            Path file = uploadRoot.resolve(storedName).normalize();
            if (!file.startsWith(uploadRoot) || !Files.exists(file)) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "资源不存在");
            }
            Resource resource = new UrlResource(file.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "资源不存在");
            }
            return resource;
        } catch (MalformedURLException e) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "资源不存在");
        }
    }

    public MediaType resolveMediaType(String storedName) {
        if (storedName.endsWith(".png")) {
            return MediaType.IMAGE_PNG;
        }
        if (storedName.endsWith(".gif")) {
            return MediaType.IMAGE_GIF;
        }
        if (storedName.endsWith(".webp")) {
            return MediaType.parseMediaType("image/webp");
        }
        if (storedName.endsWith(".mp4")) {
            return MediaType.parseMediaType("video/mp4");
        }
        if (storedName.endsWith(".webm")) {
            return MediaType.parseMediaType("video/webm");
        }
        if (storedName.endsWith(".mov")) {
            return MediaType.parseMediaType("video/quicktime");
        }
        return MediaType.IMAGE_JPEG;
    }

    private static String formatMb(long bytes) {
        return (bytes / (1024 * 1024)) + "MB";
    }
}
