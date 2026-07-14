package com.wildai.content.service;

import com.wildai.common.config.WildAiProperties;
import com.wildai.common.exception.BusinessException;
import com.wildai.common.exception.ErrorCode;
import com.wildai.content.dto.ArticleAssetResource;
import com.wildai.content.dto.ArticleAssetUploadDto;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ArticleAssetServiceTest {

    private static final long MAX_IMAGE_BYTES = 5L * 1024 * 1024;

    @TempDir
    Path uploadDir;

    private ArticleAssetService service;

    @BeforeEach
    void setUp() throws IOException {
        WildAiProperties properties = new WildAiProperties();
        properties.getArticle().setUploadDir(uploadDir.toString());
        properties.getArticle().setMaxImageBytes(MAX_IMAGE_BYTES);
        service = new ArticleAssetService(properties);
        service.ensureUploadDir();
    }

    @Test
    void uploadAcceptsPngMagicAndStoresGeneratedResource() throws IOException {
        byte[] png = bytes(0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00);
        MockMultipartFile file = new MockMultipartFile("file", "cover.png", "image/png", png);

        ArticleAssetUploadDto result = service.upload(file);

        assertThat(result.storedName()).matches("[0-9a-f]{32}\\.png");
        assertThat(result.url()).isEqualTo("/api/article-assets/" + result.storedName());
        assertThat(result.contentType()).isEqualTo("image/png");
        assertThat(uploadDir.resolve(result.storedName()))
                .isRegularFile()
                .hasBinaryContent(png);
    }

    @Test
    void uploadRejectsHtmlDisguisedAsPng() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "cover.png", "image/png", "<html>bad</html>".getBytes());

        assertBadRequest(file, "图片内容与格式不匹配");
    }

    @ParameterizedTest(name = "魔数识别 {0}")
    @MethodSource("supportedImages")
    void uploadUsesMagicInsteadOfFilenameOrDeclaredType(
            String label, byte[] content, String extension, MediaType mediaType) throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file", "伪装文件.svg", "text/html", content);

        ArticleAssetUploadDto result = service.upload(file);
        ArticleAssetResource loaded = service.load(result.storedName());

        assertThat(result.storedName()).matches("[0-9a-f]{32}\\." + extension);
        assertThat(result.contentType()).isEqualTo(mediaType.toString());
        assertThat(loaded.contentType()).isEqualTo(mediaType);
        assertThat(loaded.resource().getInputStream().readAllBytes()).isEqualTo(content);
    }

    @ParameterizedTest(name = "拒绝不支持内容 {0}")
    @MethodSource("unsupportedImages")
    void uploadRejectsUnsupportedImageContent(
            String label, String filename, String declaredType, byte[] content, String message) {
        MockMultipartFile file = new MockMultipartFile("file", filename, declaredType, content);

        assertBadRequest(file, message);
    }

    @Test
    void uploadRejectsNullAndEmptyFiles() {
        assertBadRequest(null, "请选择图片文件");
        assertBadRequest(new MockMultipartFile("file", new byte[0]), "请选择图片文件");
    }

    @Test
    void uploadRejectsAdvertisedSizeOverLimit() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "large.png", "image/png", new byte[(int) MAX_IMAGE_BYTES + 1]);

        assertBadRequest(file, "图片大小不能超过 5MB");
    }

    @Test
    void uploadRejectsActualBytesOverLimitEvenWhenReportedSizeIsSmall() {
        byte[] oversized = new byte[(int) MAX_IMAGE_BYTES + 1];
        System.arraycopy(bytes(0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A), 0, oversized, 0, 8);
        MockMultipartFile file = new MockMultipartFile("file", "large.png", "image/png", oversized) {
            @Override
            public long getSize() {
                return 1;
            }
        };

        assertBadRequest(file, "图片大小不能超过 5MB");
    }

    @ParameterizedTest(name = "非法资源名 {0}")
    @MethodSource("invalidStoredNames")
    void loadRejectsInvalidOrMissingStoredName(String storedName) {
        assertThatThrownBy(() -> service.load(storedName))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND);
                    assertThat(exception).hasMessage("资源不存在");
                });
    }

    @Test
    void loadReturnsResourceAndExactMediaType() throws IOException {
        byte[] gif = "GIF89a-content".getBytes();
        ArticleAssetUploadDto uploaded = service.upload(
                new MockMultipartFile("file", "wrong.jpg", "image/jpeg", gif));

        ArticleAssetResource result = service.load(uploaded.storedName());

        assertThat(result.contentType()).isEqualTo(MediaType.IMAGE_GIF);
        assertThat(result.resource().getInputStream().readAllBytes()).isEqualTo(gif);
    }

    @Test
    void loadRejectsSymbolicLinkWithoutFollowingIt() throws IOException {
        Path outsideFile = Files.createTempFile("article-asset-outside-", ".png");
        Path link = uploadDir.resolve("0123456789abcdef0123456789abcdef.png");
        try {
            try {
                Files.createSymbolicLink(link, outsideFile);
            } catch (UnsupportedOperationException | SecurityException | IOException exception) {
                Assumptions.abort("当前平台不支持符号链接测试");
            }

            assertThatThrownBy(() -> service.load(link.getFileName().toString()))
                    .isInstanceOfSatisfying(BusinessException.class, exception -> {
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND);
                        assertThat(exception).hasMessage("资源不存在");
                    });
        } finally {
            Files.deleteIfExists(link);
            Files.deleteIfExists(outsideFile);
        }
    }

    private void assertBadRequest(MockMultipartFile file, String message) {
        assertThatThrownBy(() -> service.upload(file))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.BAD_REQUEST);
                    assertThat(exception).hasMessage(message);
                });
    }

    private static Stream<Arguments> supportedImages() {
        return Stream.of(
                Arguments.of("JPEG", bytes(0xFF, 0xD8, 0xFF, 0x00), "jpg", MediaType.IMAGE_JPEG),
                Arguments.of("PNG", bytes(0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A),
                        "png", MediaType.IMAGE_PNG),
                Arguments.of("GIF87a", "GIF87a-content".getBytes(), "gif", MediaType.IMAGE_GIF),
                Arguments.of("GIF89a", "GIF89a-content".getBytes(), "gif", MediaType.IMAGE_GIF),
                Arguments.of("WebP", bytes(
                        0x52, 0x49, 0x46, 0x46, 0x04, 0x00, 0x00, 0x00,
                        0x57, 0x45, 0x42, 0x50), "webp", MediaType.parseMediaType("image/webp")));
    }

    private static Stream<Arguments> unsupportedImages() {
        return Stream.of(
                Arguments.of("SVG", "vector.svg", "image/svg+xml", "<svg/>".getBytes(),
                        "仅支持 JPG、PNG、GIF、WEBP 图片"),
                Arguments.of("HTML", "page.html", "text/html", "<html/>".getBytes(),
                        "仅支持 JPG、PNG、GIF、WEBP 图片"),
                Arguments.of("未知格式", "blob.bin", "application/octet-stream", bytes(0x01, 0x02, 0x03),
                        "仅支持 JPG、PNG、GIF、WEBP 图片"));
    }

    private static Stream<String> invalidStoredNames() {
        return Stream.of(
                "../secret",
                "/tmp/0123456789abcdef0123456789abcdef.png",
                "..\\secret",
                "%2e%2e%2fsecret",
                "ABCDEF0123456789ABCDEF0123456789.png",
                "0123456789abcdef0123456789abcdef.svg",
                "0123456789abcdef0123456789abcdeg.png",
                "0123456789abcdef0123456789abcdef.png/extra",
                "0123456789abcdef0123456789abcdef.png");
    }

    private static byte[] bytes(int... values) {
        byte[] result = new byte[values.length];
        for (int i = 0; i < values.length; i++) {
            result[i] = (byte) values[i];
        }
        return result;
    }
}
