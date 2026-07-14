package com.wildai.content.service;

import com.wildai.common.config.WildAiProperties;
import com.wildai.common.exception.BusinessException;
import com.wildai.common.exception.ErrorCode;
import com.wildai.content.dto.ArticleAssetResource;
import com.wildai.content.dto.ArticleAssetUploadDto;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.PathResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class ArticleAssetService {

    private static final Pattern STORED_NAME_PATTERN =
            Pattern.compile("[0-9a-f]{32}\\.(jpg|png|gif|webp)");
    private static final Pattern SUPPORTED_FILENAME_PATTERN =
            Pattern.compile("(?i).*\\.(jpe?g|png|gif|webp)$");
    private static final MediaType IMAGE_WEBP = MediaType.parseMediaType("image/webp");
    private static final long SERVLET_MULTIPART_MAX_BYTES = 52L * 1024 * 1024;

    private final Path configuredUploadRoot;
    private final long maxImageBytes;
    private Path uploadRoot;

    public ArticleAssetService(WildAiProperties properties) {
        WildAiProperties.Article article = properties.getArticle();
        String uploadDirectory = article.getUploadDir();
        if (uploadDirectory == null || uploadDirectory.isBlank()) {
            throw new IllegalStateException("文章图片上传目录不能为空");
        }
        if (article.getMaxImageBytes() <= 0) {
            throw new IllegalStateException("文章图片大小上限必须大于 0");
        }
        if (article.getMaxImageBytes() > SERVLET_MULTIPART_MAX_BYTES) {
            throw new IllegalStateException("文章图片大小上限不能超过 52MB");
        }

        this.configuredUploadRoot = Path.of(uploadDirectory)
                .toAbsolutePath()
                .normalize();
        this.maxImageBytes = article.getMaxImageBytes();
    }

    @PostConstruct
    public void ensureUploadDir() {
        try {
            Files.createDirectories(configuredUploadRoot);
            uploadRoot = configuredUploadRoot.toRealPath();
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "图片存储目录初始化失败");
        }
    }

    public ArticleAssetUploadDto upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请选择图片文件");
        }
        if (file.getSize() > maxImageBytes) {
            throw imageTooLarge();
        }

        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "图片读取失败");
        }
        if (content.length > maxImageBytes) {
            throw imageTooLarge();
        }

        ImageType imageType = detectImageType(content);
        if (imageType == null) {
            if (claimsSupportedFormat(file)) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "图片内容与格式不匹配");
            }
            throw new BusinessException(ErrorCode.BAD_REQUEST, "仅支持 JPG、PNG、GIF、WEBP 图片");
        }

        String storedName = UUID.randomUUID().toString().replace("-", "") + imageType.extension;
        Path target = currentUploadRoot().resolve(storedName).normalize();
        if (!target.startsWith(currentUploadRoot())) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "图片保存失败");
        }

        try {
            Files.write(target, content, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        } catch (FileAlreadyExistsException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "图片保存失败");
        } catch (IOException exception) {
            try {
                Files.deleteIfExists(target);
            } catch (IOException ignored) {
                // 清理失败时仍只返回统一业务错误，避免泄漏存储路径。
            }
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "图片保存失败");
        }

        return new ArticleAssetUploadDto(
                "/api/article-assets/" + storedName,
                storedName,
                imageType.mediaType.toString());
    }

    public ArticleAssetResource load(String storedName) {
        if (storedName == null || !STORED_NAME_PATTERN.matcher(storedName).matches()) {
            throw resourceNotFound();
        }

        Path root = currentUploadRoot();
        Path file = root.resolve(storedName).normalize();
        if (!file.startsWith(root)
                || !Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)
                || !Files.isReadable(file)) {
            throw resourceNotFound();
        }

        return new ArticleAssetResource(new PathResource(file), mediaTypeFor(storedName));
    }

    private Path currentUploadRoot() {
        if (uploadRoot == null) {
            ensureUploadDir();
        }
        return uploadRoot;
    }

    private BusinessException imageTooLarge() {
        return new BusinessException(
                ErrorCode.BAD_REQUEST,
                "图片大小不能超过 " + (maxImageBytes / (1024 * 1024)) + "MB");
    }

    private static ImageType detectImageType(byte[] content) {
        if (startsWith(content, 0xFF, 0xD8, 0xFF)) {
            return ImageType.JPEG;
        }
        if (startsWith(content, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)) {
            return ImageType.PNG;
        }
        if (startsWith(content, 'G', 'I', 'F', '8', '7', 'a')
                || startsWith(content, 'G', 'I', 'F', '8', '9', 'a')) {
            return ImageType.GIF;
        }
        if (startsWith(content, 'R', 'I', 'F', 'F')
                && matchesAt(content, 8, 'W', 'E', 'B', 'P')) {
            return ImageType.WEBP;
        }
        return null;
    }

    private static boolean claimsSupportedFormat(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType != null) {
            String normalizedType = contentType.toLowerCase(Locale.ROOT);
            if (normalizedType.equals("image/jpeg")
                    || normalizedType.equals("image/png")
                    || normalizedType.equals("image/gif")
                    || normalizedType.equals("image/webp")) {
                return true;
            }
        }
        String originalFilename = file.getOriginalFilename();
        return originalFilename != null && SUPPORTED_FILENAME_PATTERN.matcher(originalFilename).matches();
    }

    private static boolean startsWith(byte[] content, int... expected) {
        return matchesAt(content, 0, expected);
    }

    private static boolean matchesAt(byte[] content, int offset, int... expected) {
        if (content.length < offset + expected.length) {
            return false;
        }
        for (int i = 0; i < expected.length; i++) {
            if ((content[offset + i] & 0xFF) != expected[i]) {
                return false;
            }
        }
        return true;
    }

    private static MediaType mediaTypeFor(String storedName) {
        if (storedName.endsWith(".png")) {
            return MediaType.IMAGE_PNG;
        }
        if (storedName.endsWith(".gif")) {
            return MediaType.IMAGE_GIF;
        }
        if (storedName.endsWith(".webp")) {
            return IMAGE_WEBP;
        }
        return MediaType.IMAGE_JPEG;
    }

    private static BusinessException resourceNotFound() {
        return new BusinessException(ErrorCode.NOT_FOUND, "资源不存在");
    }

    private enum ImageType {
        JPEG(".jpg", MediaType.IMAGE_JPEG),
        PNG(".png", MediaType.IMAGE_PNG),
        GIF(".gif", MediaType.IMAGE_GIF),
        WEBP(".webp", IMAGE_WEBP);

        private final String extension;
        private final MediaType mediaType;

        ImageType(String extension, MediaType mediaType) {
            this.extension = extension;
            this.mediaType = mediaType;
        }
    }
}
